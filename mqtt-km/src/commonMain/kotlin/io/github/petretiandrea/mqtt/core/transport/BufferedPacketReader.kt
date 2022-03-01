package io.github.petretiandrea.mqtt.core.transport

import io.github.petretiandrea.mqtt.core.model.packets.FixedHeader
import io.github.petretiandrea.mqtt.core.model.packets.MqttPacket
import io.github.petretiandrea.socket.buffer.allocMultiplatformBuffer
import io.github.petretiandrea.socket.stream.BufferedInputStream
import kotlin.time.Duration

interface BufferedPacketReader {
    suspend fun next(timeout: Duration): MqttPacket?
}

@ExperimentalUnsignedTypes
class DefaultBufferedPacketReader(
    private val reader: BufferedInputStream
) : BufferedPacketReader {

    @Suppress("MagicNumber")
    override suspend fun next(timeout: Duration): MqttPacket? {
        var multiplier = 1
        var length = 0
        var currentByte: Int

        val fixedHeader = reader.readByte(timeout).toUByte()
        if (fixedHeader >= 0u && FixedHeader.fromByte(fixedHeader).isSuccess) {
            do {
                currentByte = reader.readByte(timeout).toInt()
                length += (currentByte and 127) * multiplier
                multiplier *= 128
                check(multiplier <= 128 * 128 * 128) { "Packet too big" }
            // implement exception
            } while ((currentByte and 128) != 0)

            val buffer = allocMultiplatformBuffer(length)
            if (reader.read(buffer, timeout) == length) {
                return MqttPacket.parse(fixedHeader, buffer.getBytes(length, 0).toUByteArray())
            }
        }
        return null
    }
}
