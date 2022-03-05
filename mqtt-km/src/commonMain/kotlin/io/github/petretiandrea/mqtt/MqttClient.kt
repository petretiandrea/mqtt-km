@file:Suppress("WildcardImport", "NoWildcardImports")

package io.github.petretiandrea.mqtt

import io.github.petretiandrea.flatMap
import io.github.petretiandrea.mqtt.core.ConnectionSettings
import io.github.petretiandrea.mqtt.core.DefaultTopicValidator
import io.github.petretiandrea.mqtt.core.Protocol
import io.github.petretiandrea.mqtt.core.asConnectPacket
import io.github.petretiandrea.mqtt.core.model.ConnectionStatus
import io.github.petretiandrea.mqtt.core.model.Message
import io.github.petretiandrea.mqtt.core.model.MessageId
import io.github.petretiandrea.mqtt.core.model.packets.*
import io.github.petretiandrea.mqtt.core.session.ClientSession
import io.github.petretiandrea.mqtt.core.session.Session
import io.github.petretiandrea.mqtt.core.transport.Transport
import io.github.petretiandrea.socket.exception.SocketErrorReason
import io.github.petretiandrea.socket.exception.SocketException
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.milliseconds

interface MqttClient : ClientCallback {
    val isConnected: Boolean

    suspend fun connect(): Result<Unit>
    suspend fun disconnect(gracefully: Boolean = true): Result<Unit>

    suspend fun publish(message: Message): Boolean
    suspend fun publish(
        topic: String,
        data: String,
        qoS: QoS = QoS.Q0,
        retain: Boolean = false,
        duplicate: Boolean = false
    ): Boolean = publish(Message(topic, data, qoS, retain, duplicate))

    suspend fun subscribe(topic: String, qoS: QoS): Boolean
    suspend fun unsubscribe(topic: String): Boolean

    companion object {
        private const val CLIENT_ID_PREFIX = "mqtt-km"
        fun generateClientId() = CLIENT_ID_PREFIX + Clock.System.now().toEpochMilliseconds()

        operator fun invoke(
            scope: CoroutineScope,
            connectionSettings: ConnectionSettings,
            session: Session = ClientSession(connectionSettings.clientId, connectionSettings.cleanSession),
        ): MqttClient {
            return MqttClientImpl(
                connectionSettings = connectionSettings,
                transport = when (connectionSettings.protocol) {
                    Protocol.TCP -> Transport.tcp()
                    Protocol.SSL -> Transport.ssl()
                    Protocol.WS -> Transport.websocket()
                },
                scope = scope,
                session = session
            )
        }
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
internal class MqttClientImpl constructor(
    private val connectionSettings: ConnectionSettings,
    private val transport: Transport,
    private val scope: CoroutineScope,
    private val callbackRegistry: CallbackRegistry = ClientCallback.registry(),
    private var session: Session
) : MqttClient, ClientCallback by callbackRegistry {

    private val eventLoopContext = io.github.petretiandrea.coroutines.newSingleThreadContext("mqtt-eventloop")
    private val pingHelper: PingHelper = PingHelper(connectionSettings.keepAliveSeconds * 1000L, transport)
    private val topicValidator = DefaultTopicValidator()
    private var outgoingQueue = emptyList<MqttPacket>()
    private var eventLoop: Job? = null

    override var isConnected: Boolean = false
        private set

    override suspend fun connect(): Result<Unit> {
        if (isConnected && transport.isConnected()) return Result.success(Unit)
        val connected = transport.connect(connectionSettings.hostname, connectionSettings.port)
            .map { connectionSettings.asConnectPacket() }
            .flatMap { transport.writePacket(it) }
            .flatMap { transport.readPacket(timeout = (connectionSettings.keepAliveSeconds * 1000).milliseconds) }
            .flatMap {
                when (it) {
                    is ConnAck -> if (it.connectionStatus == ConnectionStatus.ACCEPT) Result.success(Unit)
                    else Result.failure(Exception("Status: ${it.connectionStatus}"))
                    else -> Result.failure(Exception("Received a $it instead of ConnAck"))
                }
            }

        if (connected.isSuccess) {
            isConnected = true
            eventLoop = scope.launch { eventLoop() }
        }

        return connected
    }

    override suspend fun disconnect(gracefully: Boolean): Result<Unit> {
        if (isConnected) {
            isConnected = false
            if (gracefully) {
                transport.writePacket(Disconnect)
                eventLoop?.cancel("Disconnected by user")
            } else {
                eventLoop?.cancel("Disconnected by user ungracefully")
            }
            eventLoop?.join()
            transport.close()
        }
        return Result.success(Unit)
    }

    override suspend fun publish(message: Message): Boolean = withContext(eventLoopContext) {
        if (topicValidator.isValidSubscribeTopic(message.topic)) {
            outgoingQueue += Publish(message)
            true
        } else false
    }

    override suspend fun subscribe(topic: String, qoS: QoS): Boolean = withContext(eventLoopContext) {
        if (topicValidator.isValidSubscribeTopic(topic)) {
            outgoingQueue += Subscribe(MessageId.generate(), topic, qoS)
            true
        } else false
    }

    override suspend fun unsubscribe(topic: String): Boolean = withContext(eventLoopContext) {
        if (topicValidator.isValidSubscribeTopic(topic)) {
            outgoingQueue += Unsubscribe(MessageId.generate(), topic)
            true
        } else true
    }

    private suspend fun eventLoop(): Unit = withContext(eventLoopContext) {
        while (isActive && transport.isConnected()) {
            sendPendingQueue()

            val packet = transport.readPacket(SOCKET_IO_TIMEOUT)
            val isFatal = packet.isFailure && packet.exceptionOrNull()
                ?.let { it as? SocketException }?.reason != SocketErrorReason.TIMEOUT

            if (!isFatal) {
                if (packet.isSuccess) {
                    pingHelper.updateLastReceivedMessageTime()
                    packet.getOrNull()?.let { routeIncomingPacket(it) }
                }
                pingHelper.sendPing()
            } else {
                // callbacks
                cancel("Fatal error packet read")
            }
            yield() // release to others coroutines
        }
        cancel()
        transport.close()
    }

    private fun routeIncomingPacket(packet: MqttPacket) = when (packet) {
        is PingResp -> pingHelper.pongReceived()
        is Publish -> onReceivePublish(packet)
        is PubAck -> onReceivePubAck(packet)
        is PubRec -> onReceivePubRec(packet)
        is PubRel -> onReceivePubRelAck(packet)
        is PubComp -> onReceivePubComp(packet)
        is SubAck -> onReceiveSubAck(packet)
        is UnsubAck -> onReceiveUnsubAck(packet)
        else -> println("Client can handle this packet: $packet")
    }

    private fun onReceiveUnsubAck(packet: UnsubAck) {
        session.popPendingSentNotAck<Unsubscribe> { it.messageId == packet.messageId }
            ?.let { callbackRegistry.unsubscribeCallback?.invoke(it) }
    }

    private fun onReceivePubComp(packet: PubComp) {
        session.popPendingReceivedNotAck<PubRec> { it.messageId == packet.messageId }
        val publish = session.popPendingSentNotAck<Publish> { it.message.messageId == packet.messageId }
        if (publish != null) {
            callbackRegistry.deliveryCompletedCallback?.invoke(publish.message)
        }
    }

    private fun onReceivePublish(publish: Publish) = when (publish.qos) {
        QoS.Q0 -> callbackRegistry.messageReceivedCallback?.invoke(publish.message)
        QoS.Q1 -> {
            callbackRegistry.messageReceivedCallback?.invoke(publish.message)
            outgoingQueue += PubAck(publish.message.messageId)
        }
        QoS.Q2 -> {
            session pushPendingReceivedNotAck publish
            outgoingQueue += PubRec(publish.message.messageId)
        }
    }

    private fun onReceivePubAck(packet: PubAck) {
        session.popPendingSentNotAck<Publish> { it.message.messageId == packet.messageId }
            ?.let { callbackRegistry.deliveryCompletedCallback?.invoke(it.message) }
    }

    private fun onReceivePubRec(packet: PubRec) {
        val publish = session.popPendingSentNotAck<Publish> { it.message.messageId == packet.messageId }
        if (publish != null) {
            session.pushPendingSentNotAck(publish)
            session.pushPendingReceivedNotAck(packet)
            outgoingQueue += PubRel(packet.messageId)
        }
    }

    private fun onReceivePubRelAck(packet: PubRel) {
        session.popPendingReceivedNotAck<Publish> { it.message.messageId == packet.messageId }
            ?.let { publish ->
                outgoingQueue += PubComp(packet.messageId)
                callbackRegistry.messageReceivedCallback?.invoke(publish.message)
            }
    }

    private fun onReceiveSubAck(packet: SubAck) {
        session.popPendingSentNotAck<Subscribe> { it.messageId == packet.messageId }
            ?.let { subscribe ->
                callbackRegistry.subscribeCompletedCallback?.invoke(subscribe, packet.grantedQos)
            }
    }

    private suspend fun sendPendingQueue() {
        val toRemove = outgoingQueue.filter { transport.writePacket(it).isSuccess }
        val newSession = toRemove.filter { it.qos > QoS.Q0 }.fold(session) { session, packet ->
            session pushPendingSentNotAck packet
            session
        }

        // set the session and clear packets from the send pending queue
        session = newSession
        outgoingQueue -= toRemove
    }

    companion object {
        private val SOCKET_IO_TIMEOUT = (0.5 * 1000).milliseconds
    }
}
