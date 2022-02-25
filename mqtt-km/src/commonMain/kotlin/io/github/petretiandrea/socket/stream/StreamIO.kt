package io.github.petretiandrea.socket.stream

import io.github.petretiandrea.socket.buffer.MultiplatformBuffer
import kotlin.time.Duration

interface InputStream {
    suspend fun read(buffer: MultiplatformBuffer): Int
    suspend fun read(buffer: MultiplatformBuffer, timeout: Duration): Int
    suspend fun close()
}

interface OutputStream {
    suspend fun send(buffer: MultiplatformBuffer): Boolean
    suspend fun close()
}