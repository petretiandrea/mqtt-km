package io.github.petretiandrea.mqtt.core.session

import io.github.petretiandrea.mqtt.core.model.packets.MqttPacket

@ExperimentalUnsignedTypes
interface Session {
    val clientId: String
    val cleanSession: Boolean

    infix fun pushSentPacket(packet: MqttPacket): Boolean
    fun popSentPacket(filter: (MqttPacket) -> Boolean): MqttPacket?

    infix fun pushPendingSentNotAck(packet: MqttPacket)
    infix fun pushPendingReceivedNotAck(packet: MqttPacket)

    fun <T : MqttPacket> popPendingReceivedNotAck(filter: (T) -> Boolean = { true }): T?
    fun <T : MqttPacket> popPendingSentNotAck(filter: (T) -> Boolean = { true }): T?
}