package io.github.petretiandrea.mqtt.core.model.packets

@ExperimentalUnsignedTypes
sealed interface MqttPacket {

    val qos: QoS

    fun toByteArray(): UByteArray

    companion object {
        @ExperimentalUnsignedTypes
        fun parse(header: UByte, body: UByteArray): MqttPacket? {
            val fixedHeader = FixedHeader.fromByte(header).getOrNull()
            val packet = when (fixedHeader?.type) {
                Type.CONNECT -> Connect.fromByteArray(body)
                Type.CONNACK -> ConnAck.fromByteArray(body)
                Type.PUBACK -> PubAck.fromByteArray(body)
                Type.PUBREC -> PubRec.fromByteArray(body)
                Type.PUBREL -> PubRel.fromByteArray(body)
                Type.PUBCOMP -> PubComp.fromByteArray(body)
                Type.SUBACK -> SubAck.fromByteArray(body)
                Type.UNSUBACK -> UnsubAck.fromByteArray(body)
                Type.PINGREQ -> Result.success(PingReq)
                Type.PINGRESP -> Result.success(PingResp)
                Type.DISCONNECT -> Result.success(Disconnect)
                else -> null
            }

            return packet?.getOrNull()
        }
    }
}