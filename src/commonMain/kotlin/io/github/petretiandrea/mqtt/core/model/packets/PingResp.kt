package io.github.petretiandrea.mqtt.core.model.packets

@ExperimentalUnsignedTypes
object PingResp : MqttPacket {

    override val qos: QoS = QoS.Q0

    override fun toByteArray(): UByteArray {
        return FixedHeader(Type.PINGRESP, false, qos, false).toByteArray(0)
    }
}