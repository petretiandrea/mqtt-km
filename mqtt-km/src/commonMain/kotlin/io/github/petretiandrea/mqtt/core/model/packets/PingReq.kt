package io.github.petretiandrea.mqtt.core.model.packets

@ExperimentalUnsignedTypes
object PingReq : MqttPacket {

    override val qos: QoS = QoS.Q0

    override fun toByteArray(): UByteArray {
        return FixedHeader(Type.PINGREQ, false, qos, false).toByteArray(0)
    }
}