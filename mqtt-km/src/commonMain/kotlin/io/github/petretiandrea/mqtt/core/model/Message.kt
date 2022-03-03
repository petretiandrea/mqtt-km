package io.github.petretiandrea.mqtt.core.model

import io.github.petretiandrea.mqtt.core.model.packets.QoS

data class Message(
    val topic: String,
    val message: String,
    val qos: QoS = QoS.Q0,
    val retain: Boolean = false,
    val duplicate: Boolean = false,
    internal val messageId: Int = if (qos > QoS.Q0) MessageId.generate() else 0
)