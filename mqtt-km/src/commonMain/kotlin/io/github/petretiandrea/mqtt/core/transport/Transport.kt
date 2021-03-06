package io.github.petretiandrea.mqtt.core.transport

import io.github.petretiandrea.mqtt.core.model.packets.MqttPacket
import kotlin.time.Duration

interface Transport {
    suspend fun isConnected(): Boolean

    suspend fun connect(hostname: String, port: Int): Result<Unit>

    suspend fun readPacket(): Result<MqttPacket>
    suspend fun readPacket(timeout: Duration): Result<MqttPacket>

    suspend fun writePacket(packet: MqttPacket): Result<Unit>
    suspend fun writePacket(packet: MqttPacket, timeout: Duration): Result<Unit>

    suspend fun close()

    @ExperimentalUnsignedTypes
    @Suppress("NotImplementedDeclaration")
    companion object {
        fun tcp(): Transport = TransportTCP()
        fun ssl(): Transport = throw NotImplementedError("SSL ACTUALLY NOT IMPLEMENTED")
        fun websocket(): Transport = throw NotImplementedError("WEBSOCKET ACTUALLY NOT IMPLEMENTED")
    }
}
