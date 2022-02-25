package io.github.petretiandrea.mqtt.core.model.packets

@ExperimentalUnsignedTypes
object Disconnect : MqttPacket {

    override val qos: QoS = QoS.Q0

    override fun toByteArray(): UByteArray {
        return FixedHeader(Type.DISCONNECT, false, qos, false).toByteArray(0)
    }
}