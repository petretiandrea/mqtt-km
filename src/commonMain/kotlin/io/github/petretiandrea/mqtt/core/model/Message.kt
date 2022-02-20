package io.github.petretiandrea.mqtt.core.model

import io.github.petretiandrea.mqtt.core.model.packets.QoS

data class Message(
    val messageId: Int,
    val topic: String,
    val message: String,
    val qos: QoS,
    val retain: Boolean,
    val duplicate: Boolean
)