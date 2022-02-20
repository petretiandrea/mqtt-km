package io.github.petretiandrea.socket.stream

import io.github.petretiandrea.socket.buffer.MultiplatformBuffer
import io.github.petretiandrea.socket.buffer.allocMultiplatformBuffer

interface BufferedInputStream : InputStream {
    suspend fun readByte(): Byte

    companion object {
        operator fun invoke(inputStream: InputStream, bufferSize: Int = 2048): BufferedInputStream =
            DefaultBufferedInputStream(inputStream, bufferSize)
        fun InputStream.buffered(bufferSize: Int = 2048): BufferedInputStream = invoke(this, bufferSize)
    }
}

private class DefaultBufferedInputStream(
    private val inputStream: InputStream,
    maxBufferSize: Int = 2048
) : BufferedInputStream, InputStream {

    private val bufferRead = allocMultiplatformBuffer(maxBufferSize)
    private var next = 0

    override suspend fun readByte(): Byte {
        if (!isAvailable()) {
            val read = fillBuffer()
            if (read > 0 && !isAvailable()) {
                return -1
            }
        }
        return bufferRead.getByte(next++)
    }


    override suspend fun read(buffer: MultiplatformBuffer): Int {
        val sizeNeedToRead = buffer.remaining()
        while (availableLength() < sizeNeedToRead) {
            val read = fillBuffer()
            if (read > 0 && !isAvailable()) {
                return -1
            }
        }
        (0 until sizeNeedToRead).forEach { _ ->
            buffer.putByte(bufferRead.getByte(next++))
        }
        return sizeNeedToRead
    }

    private fun availableLength(): Int = bufferRead.cursor - next

    private fun isAvailable(): Boolean {
        return availableLength() > 0
    }

    private suspend fun fillBuffer(): Int {
        if (!isAvailable()) {
            bufferRead.reset()
            next = 0
            return inputStream.read(bufferRead)
        }
        return -1
    }

    override suspend fun close() = inputStream.close()
}