package io.github.petretiandrea.mqtt.core.model.packets

import io.github.petretiandrea.mqtt.core.model.ConnectionStatus
import io.github.petretiandrea.mqtt.core.model.MqttDeserializer
import kotlinx.cinterop.toByte

@ExperimentalUnsignedTypes
data class ConnAck internal constructor(
    val sessionPresent: Boolean,
    val connectionStatus: ConnectionStatus
) : MqttPacket {

    companion object : MqttDeserializer {
        override fun fromByteArray(data: UByteArray): Result<ConnAck> {
            val sessionPresent = (data[0] and 1u).toInt() == 1
            val connectionStatus = ConnectionStatus.values()
                .firstOrNull { it.ordinal == (data[1] and 255u).toInt() }
            val packet = connectionStatus?.let { connection ->
                ConnAck(
                    sessionPresent = sessionPresent,
                    connectionStatus = connection
                )
            }
            return packet?.let { Result.success(it) } ?: Result.failure(Exception("MQTT PARSE ERROR"))
        }
    }


    override fun toByteArray(): UByteArray {
        val fixedHeader = FixedHeader(Type.CONNACK, false, QoS.Q0, false)
        val bytes = UByteArray(2).apply {
            this[0] =
                if (connectionStatus != ConnectionStatus.ACCEPT) 0.toUByte() else sessionPresent.toByte().toUByte()
            this[1] = connectionStatus.ordinal.toUByte()
        }
        return fixedHeader.toByteArray(2) + bytes
    }
}