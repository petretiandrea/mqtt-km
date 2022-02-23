package io.github.petretiandrea.mqtt

import io.github.petretiandrea.flatMap
import io.github.petretiandrea.mqtt.core.ConnectionSettings
import io.github.petretiandrea.mqtt.core.asConnectPacket
import io.github.petretiandrea.mqtt.core.model.ConnectionStatus
import io.github.petretiandrea.mqtt.core.model.Message
import io.github.petretiandrea.mqtt.core.model.MessageId
import io.github.petretiandrea.mqtt.core.model.packets.*
import io.github.petretiandrea.mqtt.core.session.Session
import io.github.petretiandrea.mqtt.core.transport.Transport
import io.github.petretiandrea.socket.exception.SocketErrorReason
import io.github.petretiandrea.socket.exception.SocketException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharedFlow
import kotlin.time.Duration.Companion.milliseconds

interface DeliveryCallback {
    fun onDeliveryCompleted(message: Message)
}

interface SubscribeCallback {
    fun onSubscribeCompleted(subscribe: Subscribe)
    fun onMessageReceived(message: Message)
    fun onUnsubscribeCompleted(unsubscribe: Unsubscribe)
}

interface ConnectionStateCallback {
    fun onLostConnection(error: Exception)
    fun onDisconnect()
}

interface MqttClient {
    val isConnected: Boolean

    suspend fun connect(): Result<Unit>
    suspend fun disconnect(): Result<Unit>

    suspend fun publish(message: Message): Boolean
    suspend fun subscribe(topic: String, qoS: QoS): Boolean
    suspend fun unsubscribe(topic: String): Boolean

    suspend fun registerDeliveryCallback(deliveryCallback: DeliveryCallback): Boolean
    suspend fun registerSubscribeCallback(subscribeCallback: SubscribeCallback): Boolean
}


@ExperimentalUnsignedTypes
class MqttClientImpl(
    private val connectionSettings: ConnectionSettings,
    private val transport: Transport,
    private val scope: CoroutineScope,
    session: Session
) : MqttClient {

    companion object {
        val SOCKET_IO_TIMEOUT = (0.5 * 1000).milliseconds
    }

    override var isConnected: Boolean = false
        private set

    // TODO: implements using actors and channels
    private val eventLoopContext = newSingleThreadContext("mqtt-eventloop")
    private val pingHelper: PingHelper = PingHelper(connectionSettings.keepAliveSeconds * 1000L, transport)

    private var clientSession = session
    private var outgoingQueue = emptyList<MqttPacket>()
    private var deliveryCallbacks = emptyList<DeliveryCallback>()
    private var subscribeCallbacks = emptyList<SubscribeCallback>()
    private var eventLoop: Job? = null

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

    override suspend fun disconnect(): Result<Unit> {
        if (isConnected) {
            isConnected = false
            transport.writePacket(Disconnect)
            eventLoop?.cancel("Disconnected by user")
            eventLoop?.join()
        }
        return Result.success(Unit)
    }

    override suspend fun publish(message: Message): Boolean = withContext(eventLoopContext) {
        outgoingQueue += Publish(message)
        true
    }

    override suspend fun subscribe(topic: String, qoS: QoS): Boolean = withContext(eventLoopContext) {
        outgoingQueue += Subscribe(MessageId.generate(), topic, qoS)
        true
    }

    override suspend fun unsubscribe(topic: String): Boolean = withContext(eventLoopContext) {
        outgoingQueue += Unsubscribe(MessageId.generate(), topic)
        true
    }

    override suspend fun registerDeliveryCallback(deliveryCallback: DeliveryCallback): Boolean =
        withContext(eventLoopContext) {
            deliveryCallbacks += deliveryCallback
            true
        }

    override suspend fun registerSubscribeCallback(subscribeCallback: SubscribeCallback): Boolean =
        withContext(eventLoopContext) {
            subscribeCallbacks += subscribeCallback
            true
        }

    private suspend fun eventLoop(): Unit = withContext(eventLoopContext) {
        while (isActive) {
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
        else -> println("Client can handle this packet: $packet")
    }

    private fun onReceivePubComp(packet: PubComp) {
        // remove pubrec
        clientSession.popPendingReceivedNotAck<PubRec> { it.messageId == packet.messageId }
        val publish = clientSession.popPendingSentNotAck<Publish> { it.message.messageId == packet.messageId }
        if (publish != null) {
            deliveryCallbacks.forEach { it.onDeliveryCompleted(publish.message) }
        }
    }

    private fun onReceivePublish(publish: Publish) = when (publish.qos) {
        QoS.Q0 -> subscribeCallbacks.forEach { it.onMessageReceived(publish.message) }
        QoS.Q1 -> {
            subscribeCallbacks.forEach { it.onMessageReceived(publish.message) }
            outgoingQueue += PubAck(publish.message.messageId)
        }
        QoS.Q2 -> {
            clientSession pushPendingReceivedNotAck publish
            outgoingQueue += PubRec(publish.message.messageId)
        }
    }

    private fun onReceivePubAck(packet: PubAck) {
        val publish = clientSession.popPendingSentNotAck<Publish> { it.message.messageId == packet.messageId }
        if (publish != null)
            deliveryCallbacks.forEach { it.onDeliveryCompleted(publish.message) }
    }

    private fun onReceivePubRec(packet: PubRec) {
        val publish = clientSession.popPendingSentNotAck<Publish> { it.message.messageId == packet.messageId }
        if (publish != null) {
            clientSession.pushPendingSentNotAck(publish)
            clientSession.pushPendingReceivedNotAck(packet)
            outgoingQueue += PubRel(packet.messageId)
        }
    }

    private fun onReceivePubRelAck(packet: PubRel) {
        clientSession.popPendingReceivedNotAck<Publish> { it.message.messageId == packet.messageId }
            ?.let { publish ->
                outgoingQueue += PubComp(packet.messageId)
                subscribeCallbacks.forEach { it.onMessageReceived(publish.message) }
            }
    }

    private fun onReceiveSubAck(packet: SubAck) {
        clientSession.popPendingSentNotAck<Subscribe> { it.messageId == packet.messageId }
            ?.let { subscribe ->
                subscribeCallbacks.forEach { it.onSubscribeCompleted(subscribe) }
            }
    }

    private suspend fun sendPendingQueue() {
        val toRemove = outgoingQueue.filter { transport.writePacket(it).isSuccess }
        val newSession = toRemove.filter { it.qos > QoS.Q0 }.fold(clientSession) { session, packet ->
            session pushPendingSentNotAck packet
            session
        }

        // set the session and clear packets from the send pending queue
        clientSession = newSession
        outgoingQueue -= toRemove
    }
}