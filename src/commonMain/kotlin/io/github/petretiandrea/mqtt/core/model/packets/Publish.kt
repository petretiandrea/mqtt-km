package io.github.petretiandrea.mqtt.core.model.packets

import io.github.petretiandrea.mqtt.core.model.Message
import io.github.petretiandrea.mqtt.core.model.Extension.writeString

@ExperimentalUnsignedTypes
data class Publish(
    val message: Message
) : MqttPacket {

    override val qos: QoS = message.qos

    override fun toByteArray(): UByteArray {
        val bytes = mutableListOf<Byte>()
        bytes.writeString(message.topic)
        // qos 1 and 2 need a message id
        if (message.qos.ordinal > QoS.Q0.ordinal) {
            bytes += (message.messageId shr 8).toByte()
            bytes += (message.messageId and 0xFF).toByte()
        }
        bytes.writeString(message.message)
        return FixedHeader(Type.PUBLISH, message.retain, message.qos, message.duplicate).toByteArray(bytes.size) +
                bytes.toByteArray().toUByteArray()
    }
}