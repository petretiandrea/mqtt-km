package io.github.petretiandrea.mqtt.dsl

import io.github.petretiandrea.mqtt.CallbackRegistry
import io.github.petretiandrea.mqtt.ClientCallback
import io.github.petretiandrea.mqtt.MqttClient
import io.github.petretiandrea.mqtt.core.ConnectionSettings
import io.github.petretiandrea.mqtt.core.Protocol
import io.github.petretiandrea.mqtt.core.model.Message
import io.github.petretiandrea.mqtt.core.model.packets.QoS
import io.github.petretiandrea.mqtt.core.model.packets.Subscribe
import io.github.petretiandrea.mqtt.core.model.packets.Unsubscribe
import io.github.petretiandrea.mqtt.core.session.ClientSession
import io.github.petretiandrea.mqtt.core.session.Session
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@MqttClientDsl
data class MqttClientSettingsBuilder(
    var hostname: String?,
    var port: Int = 1883,
    var clientId: String?,
    var username: String = "",
    var password: String = "",
    var cleanSession: Boolean = false,
    var willMessage: Message? = null,
    var keepAlive: Duration = 60.seconds,
    internal val protocol: Protocol = Protocol.TCP
) {
    internal fun buildSettings(): ConnectionSettings {
        requireNotNull(hostname) { "Hostname cannot be null" }
        requireNotNull(clientId) { "Client id cannot be null" }
        return ConnectionSettings(
            hostname = hostname.orEmpty(),
            port = port,
            clientId = clientId.orEmpty(),
            username = username,
            password = password,
            cleanSession = cleanSession,
            willMessage = willMessage,
            keepAliveSeconds = keepAlive.inWholeSeconds.toInt(),
            protocol = protocol
        )
    }
}

@MqttClientDsl
data class MqttClientBuilder(
    val scope: CoroutineScope,
    var settings: MqttClientSettingsBuilder? = null,
    var session: Session? = null,
) : ClientCallback {
    private val callbackRegistry: CallbackRegistry = ClientCallback.registry()

    internal fun buildClient(): MqttClient {
        requireNotNull(settings) { "Mqtt settings cannot be null" }
        val connectionSettings = settings!!.buildSettings()
        return MqttClient(
            scope = scope,
            connectionSettings = connectionSettings,
            session = session ?: ClientSession(connectionSettings.clientId, connectionSettings.cleanSession)
        ).apply {
            callbackRegistry.messageReceivedCallback?.let { onMessageReceived(it) }
            callbackRegistry.deliveryCompletedCallback?.let { onDeliveryCompleted(it) }
            callbackRegistry.subscribeCompletedCallback?.let { onSubscribeCompleted(it) }
            callbackRegistry.lostConnectionCallback?.let { onLostConnection(it) }
            callbackRegistry.disconnectCallback?.let { onDisconnect(it) }
            callbackRegistry.unsubscribeCallback?.let { onUnsubscribeComplete(it) }
        }
    }

    override fun onMessageReceived(callback: (Message) -> Unit) {
        callbackRegistry.messageReceivedCallback = callback
    }

    override fun onDeliveryCompleted(callback: (Message) -> Unit) {
        callbackRegistry.deliveryCompletedCallback = callback
    }

    override fun onSubscribeCompleted(callback: (Subscribe, QoS) -> Unit) {
        callbackRegistry.subscribeCompletedCallback = callback
    }

    override fun onLostConnection(callback: (Exception) -> Unit) {
        callbackRegistry.lostConnectionCallback = callback
    }

    override fun onDisconnect(callback: (Exception?) -> Unit) {
        callbackRegistry.disconnectCallback = callback
    }

    override fun onUnsubscribeComplete(callback: (Unsubscribe) -> Unit) {
        callbackRegistry.unsubscribeCallback = callback
    }
}