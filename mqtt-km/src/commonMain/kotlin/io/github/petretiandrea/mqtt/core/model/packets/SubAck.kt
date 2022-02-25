package io.github.petretiandrea.mqtt.core.model.packets

import io.github.petretiandrea.mqtt.core.model.MqttDeserializer
import io.github.petretiandrea.mqtt.core.model.Util

@ExperimentalUnsignedTypes
data class SubAck(
    val messageId: Int,
    val grantedQos: QoS,
    val isFailure: Boolean
) : MqttPacket {

    companion object : MqttDeserializer {
        override fun fromByteArray(data: UByteArray): Result<SubAck> {
            var offset = 0
            val messageId = Util.getIntFromMSBLSB(data[offset++].toByte(), data[offset++].toByte())
            val grantedQos = (data[offset].toUInt() and 255u).let { byte ->
                if (byte != 128u) QoS.values().firstOrNull { it.ordinal == byte.toInt() } else null
            }
            return Result.success(
                SubAck(messageId, grantedQos ?: QoS.Q0, grantedQos == null)
            )
        }
    }

    override val qos: QoS = QoS.Q0

    override fun toByteArray(): UByteArray {
        val bytes = ByteArray(3).apply {
            this[0] = (messageId shr 8).toByte()
            this[1] = (messageId and 0xFF).toByte()
            this[2] = getGrantedFlag()
        }
        return FixedHeader(
            type = Type.SUBACK,
            retain = false,
            qos = qos,
            duplicate = false
        ).toByteArray(3) + bytes.toUByteArray()
    }

    private fun getGrantedFlag(): Byte = when {
        isFailure -> 128u
        grantedQos == QoS.Q0 -> 0u
        grantedQos == QoS.Q1 -> 1u
        else -> 2u // qos2
    }.toByte()
}