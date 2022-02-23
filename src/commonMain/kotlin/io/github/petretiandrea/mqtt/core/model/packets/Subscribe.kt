package io.github.petretiandrea.mqtt.core.model.packets

import io.github.petretiandrea.mqtt.core.model.Extension.writeString


@OptIn(ExperimentalUnsignedTypes::class)
data class Subscribe(
    val messageId: Int,
    val topic: String,
    val subscriptionQos: QoS
) : MqttPacket {

    override val qos: QoS = QoS.Q1

    override fun toByteArray(): UByteArray {
        val bytes = mutableListOf<Byte>()
        bytes += (messageId shr 8).toByte()
        bytes += (messageId and 0xFF).toByte()
        bytes.writeString(topic)
        bytes += subscriptionQos.ordinal.toByte()
        return FixedHeader(Type.SUBSCRIBE, false, qos, false).toByteArray(bytes.size) +
                bytes.toByteArray().toUByteArray()
    }
}