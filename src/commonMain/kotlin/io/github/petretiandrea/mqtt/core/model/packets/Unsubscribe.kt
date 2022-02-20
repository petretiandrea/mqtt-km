package io.github.petretiandrea.mqtt.core.model.packets

import io.github.petretiandrea.mqtt.core.model.Extension.writeString

@ExperimentalUnsignedTypes
data class Unsubscribe(
    val messageId: Int,
    val topic: String
) : MqttPacket {


    override fun toByteArray(): UByteArray {
        val bytes = mutableListOf<Byte>()
        bytes += (messageId shr 8).toByte()
        bytes += (messageId and 0xFF).toByte()
        bytes.writeString(topic)
        return FixedHeader(Type.UNSUBSCRIBE, false, QoS.Q1, false).toByteArray(bytes.size) +
                bytes.toByteArray().toUByteArray()
    }

}