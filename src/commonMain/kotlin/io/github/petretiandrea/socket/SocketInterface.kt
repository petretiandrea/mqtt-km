package io.github.petretiandrea.socket

import io.github.petretiandrea.socket.stream.InputStream
import io.github.petretiandrea.socket.stream.OutputStream

@ExperimentalUnsignedTypes
interface SocketInterface {
    fun inputStream(): InputStream
    fun outputStream(): OutputStream
    suspend fun close()
}

expect fun createSocket(hostname: String, port: Int): SocketInterface