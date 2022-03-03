package io.github.petretiandrea.mqtt.core.model.packets

enum class Type(val value: Int) {
    CONNECT(1),
    CONNACK(2),
    PUBLISH(3),
    PUBACK(4),
    PUBREC(5),
    PUBREL(6),
    PUBCOMP(7),
    SUBSCRIBE(8),
    SUBACK(9),
    UNSUBSCRIBE(10),
    UNSUBACK(11),
    PINGREQ(12),
    PINGRESP(13),
    DISCONNECT(14)
}

enum class QoS {
    Q0,
    Q1,
    Q2
}

@Suppress("MagicNumber")
data class FixedHeader(
    val type: Type,
    val retain: Boolean,
    val qos: QoS,
    val duplicate: Boolean
) {

    @ExperimentalUnsignedTypes
    @Suppress("MaxLineLength")
    fun toByteArray(remainingLength: Int): UByteArray {
        val bytes = UByteArray(if (remainingLength > 127) 3 else 2)
        bytes[0] =
            (type.value and 0x0F shl 4 or ((if (duplicate) 1 else 0) and 0x01 shl 3) or (qos.ordinal and 0x3 shl 1) or ((if (retain) 1 else 0) and 0x1)).toUByte()
        if (remainingLength > 127) {
            bytes[1] = ((remainingLength % 128) or 0x80).toUByte()
            bytes[2] = (remainingLength / 128).toUByte()
        } else {
            bytes[1] = remainingLength.toUByte()
        }
        return bytes
    }

    companion object {
        /**
         * @param packet The whole packet as array including fixed header
         */
        fun detectRemainingLengthSize(packet: UByteArray): Int = if (packet.size >= 130) 2 else 1
        fun fromByte(fixedHeader: UByte): Result<FixedHeader> {
            val parsedType = Type.values().firstOrNull { it.value == (fixedHeader.toInt() and 240) shr 4 }
            val parsedQos = QoS.values().firstOrNull { it.ordinal == (fixedHeader.toInt() and 6) shr 1 }
            return parsedType?.let { type ->
                parsedQos?.let { qos ->
                    Result.success(
                        FixedHeader(
                            type = type,
                            retain = (fixedHeader.toInt() and 1) == 1,
                            qos = qos,
                            duplicate = (fixedHeader.toInt() and 8) == 1
                        )
                    )
                }
            } ?: Result.failure(Exception("MQTT parse error"))
        }
    }
}
