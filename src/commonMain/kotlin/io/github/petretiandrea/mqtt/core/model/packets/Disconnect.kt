package io.github.petretiandrea.mqtt.core.model.packets

@ExperimentalUnsignedTypes
object Disconnect : MqttPacket {

    override fun toByteArray(): UByteArray {
        return FixedHeader(Type.DISCONNECT, false, QoS.Q0, false).toByteArray(0)
    }
}