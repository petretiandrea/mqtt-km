package io.github.petretiandrea.mqtt.core.session

import io.github.petretiandrea.mqtt.core.model.packets.MqttPacket

@ExperimentalUnsignedTypes
interface Session {
    val clientId: String
    val cleanSession: Boolean

    infix fun addPendingSentNotAck(packet: MqttPacket): Session
    infix fun addPendingReceivedNotAck(packet: MqttPacket): Session

    fun removePendingSendNotAck(filter: (MqttPacket) -> Boolean): Session
    fun removePendingReceivedNotAck(filter: (MqttPacket) -> Boolean): Session
}