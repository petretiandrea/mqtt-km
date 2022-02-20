package io.github.petretiandrea.mqtt.core.model.packets

import io.github.petretiandrea.mqtt.core.model.MqttDeserializer
import io.github.petretiandrea.mqtt.core.model.Util

@ExperimentalUnsignedTypes
data class PubRel(
    val messageId: Int
) : MqttPacket {

    companion object : MqttDeserializer {
        override fun fromByteArray(data: UByteArray): Result<PubRel> {
            val messageId = Util.getIntFromMSBLSB(data[0].toByte(), data[1].toByte())
            return Result.success(PubRel(messageId))
        }
    }

    override fun toByteArray(): UByteArray {
        val bytes = ByteArray(2).apply {
            this[0] = (messageId shr 8).toByte()
            this[1] = (messageId and 0xFF).toByte()
        }
        return FixedHeader(
            type = Type.PUBREL,
            retain = false,
            qos = QoS.Q1,
            duplicate = false
        ).toByteArray(2) + bytes.toUByteArray()
    }

}