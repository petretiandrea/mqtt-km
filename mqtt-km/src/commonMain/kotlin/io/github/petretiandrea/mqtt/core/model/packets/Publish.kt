package io.github.petretiandrea.mqtt.core.model.packets

import io.github.petretiandrea.mqtt.core.model.Extension.writeString
import io.github.petretiandrea.mqtt.core.model.Message
import io.github.petretiandrea.mqtt.core.model.MqttDeserializer
import io.github.petretiandrea.mqtt.core.model.Util

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
        bytes += message.message.encodeToByteArray().toList()
        return FixedHeader(
            Type.PUBLISH,
            message.retain,
            message.qos,
            message.duplicate
        ).toByteArray(bytes.size) + bytes.toByteArray().toUByteArray()
    }

    companion object : MqttDeserializer {
        override fun fromByteArray(data: UByteArray): Result<Publish> {
            var offset = 0
            // need to parse header to obtain right qos and other fixed header info
            return FixedHeader.fromByte(data[offset++]).map { header ->
                val topicLength = Util.getIntFromMSBLSB(data[offset++].toByte(), data[offset++].toByte())
                val topic =
                    data.drop(offset).take(topicLength).map { it.toInt().toChar() }.toCharArray().concatToString()

                offset += topicLength

                val messageId = if (header.qos > QoS.Q0) Util.getIntFromMSBLSB(
                    data[offset++].toByte(),
                    data[offset++].toByte()
                ) else 0
                val message =
                    data.drop(offset).map { it.toInt().toChar() }.toCharArray().concatToString()

                Publish(
                    Message(
                        messageId = messageId,
                        topic = topic,
                        message = message,
                        qos = header.qos,
                        retain = header.retain,
                        duplicate = header.duplicate
                    )
                )
            }
        }
    }
}