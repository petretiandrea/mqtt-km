package io.github.petretiandrea.mqtt.core.model.packets

import io.github.petretiandrea.mqtt.core.model.Extension.writeString

@ExperimentalUnsignedTypes
data class Subscribe(
    val messageId: Int,
    val topic: String,
    val qos: QoS
) : MqttPacket {

    override fun toByteArray(): UByteArray {
        val bytes = mutableListOf<Byte>()
        bytes += (messageId shr 8).toByte()
        bytes += (messageId and 0xFF).toByte()
        bytes.writeString(topic)
        bytes += qos.ordinal.toByte()
        return FixedHeader(Type.SUBSCRIBE, false, QoS.Q1, false).toByteArray(bytes.size) +
                bytes.toByteArray().toUByteArray()
    }
}