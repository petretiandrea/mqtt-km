package io.github.petretiandrea.mqtt.core.model.packets

import io.github.petretiandrea.mqtt.core.model.MqttDeserializer
import io.github.petretiandrea.mqtt.core.model.Util

@ExperimentalUnsignedTypes
data class UnsubAck(
    val messageId: Int
) : MqttPacket {

    override val qos: QoS = QoS.Q0

    override fun toByteArray(): UByteArray {
        val bytes = ByteArray(2).apply {
            this[0] = (messageId shr 8).toByte()
            this[1] = (messageId and 0xFF).toByte()
        }

        return FixedHeader(
            type = Type.UNSUBACK,
            retain = false,
            qos = qos,
            duplicate = false
        ).toByteArray(2) + bytes.toUByteArray()
    }


    companion object : MqttDeserializer {
        override fun fromByteArray(data: UByteArray): Result<UnsubAck> {
            val messageId = Util.getIntFromMSBLSB(data[0], data[1])
            return Result.success(UnsubAck(messageId))
        }
    }
}