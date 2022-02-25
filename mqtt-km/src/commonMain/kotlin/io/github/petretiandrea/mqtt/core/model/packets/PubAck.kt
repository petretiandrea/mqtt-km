package io.github.petretiandrea.mqtt.core.model.packets

import io.github.petretiandrea.mqtt.core.model.MqttDeserializer
import io.github.petretiandrea.mqtt.core.model.Util

@ExperimentalUnsignedTypes
data class PubAck(
    val messageId: Int
) : MqttPacket {

    companion object : MqttDeserializer {
        override fun fromByteArray(data: UByteArray): Result<PubAck> {
            val messageId = Util.getIntFromMSBLSB(data[0].toByte(), data[1].toByte())
            return Result.success(PubAck(messageId))
        }
    }

    override val qos: QoS = QoS.Q0

    override fun toByteArray(): UByteArray {
        val bytes = ByteArray(2).apply {
            this[0] = (messageId shr 8).toByte()
            this[1] = (messageId and 0xFF).toByte()
        }
        return FixedHeader(
            type = Type.PUBACK,
            retain = false,
            qos = qos,
            duplicate = false
        ).toByteArray(2) + bytes.toUByteArray()
    }
}