package io.github.petretiandrea.mqtt.core.session

import io.github.petretiandrea.mqtt.core.model.packets.MqttPacket

@ExperimentalUnsignedTypes
data class ClientSession(
    override val clientId: String,
    override val cleanSession: Boolean,
    private val sentNotAck: List<MqttPacket> = emptyList(),
    private val receivedNotAck: List<MqttPacket> = emptyList()
) : Session {

    override fun addPendingSentNotAck(packet: MqttPacket): Session =
        this.copy(sentNotAck = sentNotAck + packet)

    override fun addPendingReceivedNotAck(packet: MqttPacket): Session =
        this.copy(receivedNotAck = sentNotAck + packet)

    override fun removePendingSendNotAck(filter: (MqttPacket) -> Boolean): Session =
        this.copy(sentNotAck = sentNotAck.filter { !filter(it) })

    override fun removePendingReceivedNotAck(filter: (MqttPacket) -> Boolean): Session =
        this.copy(sentNotAck = receivedNotAck.filter { !filter(it) })
}