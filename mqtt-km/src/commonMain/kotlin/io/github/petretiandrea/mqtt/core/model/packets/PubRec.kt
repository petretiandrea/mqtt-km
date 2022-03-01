package io.github.petretiandrea.mqtt.core.model.packets

import io.github.petretiandrea.mqtt.core.model.MqttDeserializer
import io.github.petretiandrea.mqtt.core.model.Util

@ExperimentalUnsignedTypes
data class PubRec(
    val messageId: Int
) : MqttPacket {

    override val qos: QoS = QoS.Q0

    override fun toByteArray(): UByteArray {
        val bytes = ByteArray(2).apply {
            this[0] = (messageId shr 8).toByte()
            this[1] = (messageId and 0xFF).toByte()
        }
        return FixedHeader(
            type = Type.PUBREC,
            retain = false,
            qos = qos,
            duplicate = false
        ).toByteArray(2) + bytes.toUByteArray()
    }

    companion object : MqttDeserializer {
        override fun fromByteArray(data: UByteArray): Result<PubRec> {
            val messageId = Util.getIntFromMSBLSB(data[0].toByte(), data[1].toByte())
            return Result.success(PubRec(messageId))
        }
    }
}