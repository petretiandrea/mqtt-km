package io.github.petretiandrea.mqtt.core.session

import io.github.petretiandrea.mqtt.core.model.packets.MqttPacket

@ExperimentalUnsignedTypes
data class ClientSession(
    override val clientId: String,
    override val cleanSession: Boolean,
    private var sentNotAck: List<MqttPacket> = emptyList(),
    private var receivedNotAck: List<MqttPacket> = emptyList()
) : Session {

    override fun pushPendingSentNotAck(packet: MqttPacket) {
        sentNotAck += packet
    }

    override fun pushPendingReceivedNotAck(packet: MqttPacket) {
        receivedNotAck += packet
    }

    override fun <T : MqttPacket> popPendingReceivedNotAck(filter: (T) -> Boolean): T? {
        return receivedNotAck.mapNotNull { it as? T }.firstOrNull(filter)?.also {
            receivedNotAck -= it
        }
    }

    override fun <T : MqttPacket> popPendingSentNotAck(filter: (T) -> Boolean): T? {
        return sentNotAck.mapNotNull { it as? T }.firstOrNull(filter)?.also {
            sentNotAck -= it
        }
    }
}