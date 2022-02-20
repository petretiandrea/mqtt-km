package io.github.petretiandrea.mqtt.core.transport

import io.github.petretiandrea.mqtt.core.model.packets.FixedHeader
import io.github.petretiandrea.mqtt.core.model.packets.MqttPacket
import io.github.petretiandrea.socket.buffer.allocMultiplatformBuffer
import io.github.petretiandrea.socket.stream.BufferedInputStream


interface BufferedPacketReader {
    suspend fun next(): MqttPacket?
}

@ExperimentalUnsignedTypes
class DefaultBufferedPacketReader(
    private val reader: BufferedInputStream
) : BufferedPacketReader {

    override suspend fun next(): MqttPacket? {
        var multiplier = 1
        var length = 0
        var currentByte: Int

        val fixedHeader = reader.readByte()
        println("Fixed: $fixedHeader")
        if (fixedHeader >= 0 && FixedHeader.fromByte(fixedHeader).isSuccess) {
            do {
                currentByte = reader.readByte().toInt()
                length += (currentByte and 127) * multiplier
                multiplier *= 128
                if (multiplier > 128 * 128 * 128)
                    TODO() // implement exception
            } while ((currentByte and 128) != 0)

            println("Length: $length")

            val buffer = allocMultiplatformBuffer(length)
            if (reader.read(buffer) == length) {
                return MqttPacket.parse(fixedHeader, buffer.getBytes(length, 0).toUByteArray())
            }
        }
        return null
    }

}