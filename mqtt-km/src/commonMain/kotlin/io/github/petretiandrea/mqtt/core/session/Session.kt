package io.github.petretiandrea.mqtt.core.session

import io.github.petretiandrea.mqtt.core.model.packets.MqttPacket

@OptIn(ExperimentalUnsignedTypes::class)
sealed interface Session {
    val clientId: String
    val cleanSession: Boolean

    infix fun pushPendingSentNotAck(packet: MqttPacket)
    infix fun pushPendingReceivedNotAck(packet: MqttPacket)

    fun <T : MqttPacket> popPendingReceivedNotAck(filter: (T) -> Boolean): T?
    fun <T : MqttPacket> popPendingSentNotAck(filter: (T) -> Boolean): T?
}