package io.github.petretiandrea.mqtt.core.transport

import io.github.petretiandrea.mqtt.core.model.packets.MqttPacket
import io.github.petretiandrea.socket.SocketInterface
import io.github.petretiandrea.socket.buffer.wrapMultiplatformBuffer
import io.github.petretiandrea.socket.createSocket
import io.github.petretiandrea.socket.stream.BufferedInputStream.Companion.buffered
import io.github.petretiandrea.socket.stream.OutputStream
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration

@ExperimentalUnsignedTypes
internal class TransportTCP : Transport {

    private var socket: SocketInterface? = null
    private var mqttReader: BufferedPacketReader? = null
    private var outputStream: OutputStream? = null

    override suspend fun isConnected(): Boolean {
        TODO()
        return true
    }

    override suspend fun connect(hostname: String, port: Int): Result<Unit> {
        return kotlin.runCatching {
            if (socket == null) {
                socket = createSocket(hostname, port)
            }
            mqttReader = DefaultBufferedPacketReader(socket!!.inputStream().buffered())
            outputStream = socket!!.outputStream()
        }
    }

    override suspend fun readPacket(): Result<MqttPacket> = readPacket(Duration.ZERO)

    override suspend fun readPacket(timeout: Duration): Result<MqttPacket> {
        return kotlin.runCatching {
            if (timeout > Duration.ZERO) {
                withTimeout(timeout) {
                    mqttReader?.next() ?: throw Exception("Parse error")
                }
            } else {
                mqttReader?.next() ?: throw Exception("Parse error")
            }
        }
    }

    override suspend fun writePacket(packet: MqttPacket): Result<Unit> = writePacket(packet, Duration.ZERO)

    override suspend fun writePacket(packet: MqttPacket, timeout: Duration): Result<Unit> {
        return kotlin.runCatching {
            val buffer = wrapMultiplatformBuffer(packet.toByteArray().toByteArray())
            if (timeout > Duration.ZERO) {
                withTimeout(timeout) {
                    outputStream?.send(buffer)
                }
            } else {
                outputStream?.send(buffer)
            }
            buffer.destroy()
        }
    }

    override suspend fun close() {
        kotlin.runCatching { socket?.close() }
        socket = null
        mqttReader = null
        outputStream = null
    }
}