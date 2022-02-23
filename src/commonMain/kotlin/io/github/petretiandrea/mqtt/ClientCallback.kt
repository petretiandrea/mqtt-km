package io.github.petretiandrea.mqtt

import io.github.petretiandrea.mqtt.core.model.Message
import io.github.petretiandrea.mqtt.core.model.packets.QoS
import io.github.petretiandrea.mqtt.core.model.packets.Subscribe
import io.github.petretiandrea.mqtt.core.model.packets.Unsubscribe

interface ClientCallback {
    suspend fun onMessageReceived(callback: (Message) -> Unit)
    suspend fun onDeliveryCompleted(callback: (Message) -> Unit)
    suspend fun onSubscribeCompleted(callback: (Subscribe, QoS) -> Unit)
    suspend fun onLostConnection(callback: (Exception) -> Unit)
    suspend fun onDisconnect(callback: (Exception?) -> Unit)
    suspend fun onUnsubscribeComplete(callback: (Unsubscribe) -> Unit)

    companion object {
        internal fun registry(): CallbackRegistry = CallbackRegistry()
    }
}

/**
 * An easy way to handle multiple callbacks using delegation on MqttClient
 */
internal class CallbackRegistry(
    internal var messageReceivedCallback : ((Message) -> Unit)? = null,
    internal var deliveryCompletedCallback: ((Message) -> Unit)? = null,
    internal var subscribeCompletedCallback: ((Subscribe, QoS) -> Unit)? = null,
    internal var lostConnectionCallback: ((Exception) -> Unit)? = null,
    internal var disconnectCallback: ((Exception?) -> Unit)? = null,
    internal var unsubscribeCallback: ((Unsubscribe) -> Unit)? = null
) : ClientCallback {
    override suspend fun onMessageReceived(callback: (Message) -> Unit) {
        messageReceivedCallback = callback
    }

    override suspend fun onDeliveryCompleted(callback: (Message) -> Unit) {
        deliveryCompletedCallback = callback
    }

    override suspend fun onSubscribeCompleted(callback: (Subscribe, QoS) -> Unit) {
        subscribeCompletedCallback = callback
    }

    override suspend fun onLostConnection(callback: (Exception) -> Unit) {
        lostConnectionCallback = callback
    }

    override suspend fun onDisconnect(callback: (Exception?) -> Unit) {
        disconnectCallback = callback
    }

    override suspend fun onUnsubscribeComplete(callback: (Unsubscribe) -> Unit) {
        unsubscribeCallback = callback
    }
}