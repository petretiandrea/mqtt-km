package io.github.petretiandrea.mqtt.core.model.packets

@ExperimentalUnsignedTypes
object PingResp : MqttPacket {

    override fun toByteArray(): UByteArray {
        return FixedHeader(Type.PINGRESP, false, QoS.Q0, false).toByteArray(0)
    }
}