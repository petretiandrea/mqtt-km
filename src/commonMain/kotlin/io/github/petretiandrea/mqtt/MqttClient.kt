package io.github.petretiandrea.mqtt

import io.github.petretiandrea.flatMap
import io.github.petretiandrea.mqtt.core.ConnectionSettings
import io.github.petretiandrea.mqtt.core.MqttVersion
import io.github.petretiandrea.mqtt.core.model.ConnectionStatus
import io.github.petretiandrea.mqtt.core.model.Message
import io.github.petretiandrea.mqtt.core.model.packets.*
import io.github.petretiandrea.mqtt.core.session.Session
import io.github.petretiandrea.mqtt.core.transport.Transport
import io.github.petretiandrea.socket.exception.SocketErrorReason
import io.github.petretiandrea.socket.exception.SocketException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.milliseconds

typealias MessageDelivered = (Message) -> Unit
typealias MessageReceived = (Message) -> Unit

interface MqttClient {
    val isConnected: Boolean

    suspend fun connect(): Result<Unit>
    suspend fun disconnect(): Result<Unit>

    suspend fun publish(message: Message): Boolean
    suspend fun subscribe(topic: String, qoS: QoS): Boolean
    suspend fun unsubscribe(topic: String): Boolean

    suspend fun registerSendCallback(callback: MessageDelivered): Boolean
    suspend fun registerReceiveCallback(callback: MessageReceived): Boolean
}

@ExperimentalUnsignedTypes
class Pinger(
    private val keepAliveMillis: Long,
    private val transport: Transport
) {
    private var lastReceivedMessage: Long = 0
    private var pingSentAt: Long = 0
    private val keepAliveTimeout = keepAliveMillis - (keepAliveMillis / 2)

    fun updateLastReceivedMessageTime() {
        lastReceivedMessage = Clock.System.now().toEpochMilliseconds()
    }

    fun pongReceived() {
        pingSentAt = 0
    }

    suspend fun sendPing(): Result<Unit> {
        return when {
            needToPing() -> {
                pingSentAt =
                    if (transport.writePacket(PingReq).isSuccess) Clock.System.now().toEpochMilliseconds() else 0
                if (pingSentAt > 0) Result.success(Unit) else Result.failure(Exception("Failed to send ping"))
            }
            isPingExpired() -> Result.failure(Exception("No Ping response received!"))
            else -> Result.failure(Exception("Unknown error pinger"))
        }
    }

    private fun needToPing(): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        return pingSentAt <= 0 && ((now - lastReceivedMessage) > keepAliveMillis)
    }

    private fun isPingExpired(): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        return pingSentAt > 0 && (now - pingSentAt) > keepAliveTimeout
    }
}

@ExperimentalUnsignedTypes
class MqttClientImpl(
    private val connectionSettings: ConnectionSettings,
    private val transport: Transport,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    session: Session
) : MqttClient {

    companion object {
        val SOCKET_IO_TIMEOUT = (0.5 * 1000).milliseconds
    }

    override var isConnected: Boolean = false
        private set

    private lateinit var pingHelper: Pinger

    private var clientSession = session

    private val pendingQueue = MutableStateFlow(emptyList<MqttPacket>())
    private var eventLoop: Job? = null

    private fun ConnectionSettings.asConnectPacket(): Connect = Connect(
        version = MqttVersion.MQTT_311,
        clientId = clientId,
        username = username.orEmpty(),
        password = password.orEmpty(),
        cleanSession = cleanSession,
        keepAliveSeconds = keepAliveSeconds,
        willMessage = willMessage
    )

    override suspend fun connect(): Result<Unit> {
        if (transport.isConnected()) return Result.success(Unit)
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
            pingHelper = Pinger(connectionSettings.keepAliveSeconds * 1000L, transport)
            isConnected = true
            eventLoop = scope.launch { eventLoop() }
            println("Start read loop")
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

    override suspend fun publish(message: Message): Boolean {
        pendingQueue.update { it + Publish(message) }
        return true
    }

    override suspend fun subscribe(topic: String, qoS: QoS): Boolean {
        pendingQueue.update { it + Subscribe(0, topic, qoS) }
        return true
    }

    override suspend fun unsubscribe(topic: String): Boolean {
        pendingQueue.update { it + Unsubscribe(0, topic) }
        return true
    }

    override suspend fun registerSendCallback(callback: MessageDelivered): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun registerReceiveCallback(callback: MessageReceived): Boolean {
        TODO("Not yet implemented")
    }

    private suspend fun eventLoop(): Unit = withContext(dispatcher) {
        while (isActive) {
            sendPendingQueue()

            val packet = transport.readPacket(SOCKET_IO_TIMEOUT)
            val isFatal = packet.isFailure && packet.exceptionOrNull()?.let { it as? SocketException }?.reason != SocketErrorReason.TIMEOUT

            if (!isFatal) {
                if (packet.isSuccess) {
                    pingHelper.updateLastReceivedMessageTime()
                    if (packet.getOrNull() is PingResp) pingHelper.pongReceived()
                }
                pingHelper.sendPing()
            } else {
                // callbacks
                cancel("Fatal error packet read")
            }
        }
        transport.close()
    }

    private suspend fun sendPendingQueue() {
        val toRemove = pendingQueue.value.filter { transport.writePacket(it).isSuccess }
        val newSession = toRemove.filter { it.qos > QoS.Q0 }.fold(clientSession) { session, packet ->
            session addPendingSentNotAck packet
        }

        // set the session and clear packets from the send pending queue
        clientSession = newSession
        pendingQueue.update { it - toRemove }
    }
}