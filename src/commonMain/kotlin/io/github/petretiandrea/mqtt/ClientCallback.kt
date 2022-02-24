package io.github.petretiandrea.mqtt

import io.github.petretiandrea.mqtt.core.model.Message
import io.github.petretiandrea.mqtt.core.model.packets.QoS
import io.github.petretiandrea.mqtt.core.model.packets.Subscribe
import io.github.petretiandrea.mqtt.core.model.packets.Unsubscribe

interface ClientCallback {
    fun onMessageReceived(callback: (Message) -> Unit)
    fun onDeliveryCompleted(callback: (Message) -> Unit)
    fun onSubscribeCompleted(callback: (Subscribe, QoS) -> Unit)
    fun onLostConnection(callback: (Exception) -> Unit)
    fun onDisconnect(callback: (Exception?) -> Unit)
    fun onUnsubscribeComplete(callback: (Unsubscribe) -> Unit)

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
    override fun onMessageReceived(callback: (Message) -> Unit) {
        messageReceivedCallback = callback
    }

    override fun onDeliveryCompleted(callback: (Message) -> Unit) {
        deliveryCompletedCallback = callback
    }

    override fun onSubscribeCompleted(callback: (Subscribe, QoS) -> Unit) {
        subscribeCompletedCallback = callback
    }

    override fun onLostConnection(callback: (Exception) -> Unit) {
        lostConnectionCallback = callback
    }

    override fun onDisconnect(callback: (Exception?) -> Unit) {
        disconnectCallback = callback
    }

    override fun onUnsubscribeComplete(callback: (Unsubscribe) -> Unit) {
        unsubscribeCallback = callback
    }
}