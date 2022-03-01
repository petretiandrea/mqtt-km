package io.github.petretiandrea.mqtt.core.model.packets

import io.github.petretiandrea.mqtt.core.model.Extension.writeString

@OptIn(ExperimentalUnsignedTypes::class)
data class Unsubscribe(
    val messageId: Int,
    val topic: String
) : MqttPacket {

    override val qos: QoS = QoS.Q1

    override fun toByteArray(): UByteArray {
        val bytes = mutableListOf<Byte>()
        bytes += (messageId shr 8).toByte()
        bytes += (messageId and 0xFF).toByte()
        bytes.writeString(topic)
        return FixedHeader(Type.UNSUBSCRIBE, false, qos, false).toByteArray(bytes.size) +
                bytes.toByteArray().toUByteArray()
    }

}