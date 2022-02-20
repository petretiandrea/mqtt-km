package io.github.petretiandrea.mqtt.core.model.packets

@ExperimentalUnsignedTypes
object PingReq : MqttPacket {

    override fun toByteArray(): UByteArray {
        return FixedHeader(Type.PINGREQ, false, QoS.Q0, false).toByteArray(0)
    }
}