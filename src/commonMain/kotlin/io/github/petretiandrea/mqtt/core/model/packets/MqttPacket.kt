package io.github.petretiandrea.mqtt.core.model.packets

@ExperimentalUnsignedTypes
sealed interface MqttPacket {

    fun toByteArray(): UByteArray


    companion object {
        @ExperimentalUnsignedTypes
        fun parse(header: Byte, body: UByteArray): MqttPacket? {
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
                Type.PINGREQ -> PingReq
                Type.PINGRESP -> PingResp
                Type.DISCONNECT -> Disconnect
                else -> null
            } as Result<MqttPacket>?

            return packet?.getOrNull()
        }
    }
}