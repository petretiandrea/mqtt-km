package io.github.petretiandrea.mqtt

import io.github.petretiandrea.mqtt.core.model.Message
import io.github.petretiandrea.mqtt.core.model.MessageId
import io.github.petretiandrea.mqtt.core.model.packets.QoS

object Extension {
    suspend fun MqttClient.publish(
        topic: String,
        message: String,
        qos: QoS,
        retain: Boolean = false,
        duplicate: Boolean = false
    ): Boolean {
        return publish(Message(MessageId.generate(), topic, message, qos, retain, duplicate))
    }
}