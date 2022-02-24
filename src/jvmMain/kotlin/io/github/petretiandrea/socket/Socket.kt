package io.github.petretiandrea.socket

import io.github.petretiandrea.socket.buffer.BufferOverflowException
import io.github.petretiandrea.socket.buffer.JVMMultiplatformBuffer
import io.github.petretiandrea.socket.buffer.MultiplatformBuffer
import io.github.petretiandrea.socket.exception.SocketErrorReason
import io.github.petretiandrea.socket.exception.SocketException
import io.github.petretiandrea.socket.stream.InputStream
import io.github.petretiandrea.socket.stream.OutputStream
import java.io.IOException
import java.net.Socket
import java.net.SocketTimeoutException
import java.io.InputStream as JvmInputStream
import java.io.OutputStream as JvmOutputStream
import kotlin.time.Duration

actual fun createSocket(hostname: String, port: Int): SocketInterface {
    return try {
        val socket = Socket(hostname, port)
        JvmSocket(
            socket,
            JvmSocketInputStream(socket, socket.getInputStream()),
            JvmSocketOutputStream(socket.getOutputStream())
        )
    } catch (ex: IOException) {
        throw SocketException("Cannot connect to $hostname on $port")
    }
}

private class JvmSocket(
    private val socket: Socket,
    private val inputStream: JvmSocketInputStream,
    private val outputStream: JvmSocketOutputStream
) : SocketInterface {

    override fun inputStream(): InputStream = inputStream
    override fun outputStream(): OutputStream = outputStream

    override fun isConnected(): Boolean = socket.isConnected

    override suspend fun close() {
        inputStream.close()
        outputStream.close()
        socket.close()
    }
}

private class JvmSocketOutputStream(
    private val outputStream: JvmOutputStream
) : OutputStream {
    override suspend fun send(buffer: MultiplatformBuffer): Boolean {
        buffer as JVMMultiplatformBuffer
        return kotlin.runCatching { outputStream.write(buffer.nativeBuffer().array()) }.isSuccess
    }

    override suspend fun close() {
        outputStream.close()
    }
}

private class JvmSocketInputStream(
    private val socket: Socket,
    private val inputStream: JvmInputStream
) : InputStream {
    override suspend fun read(buffer: MultiplatformBuffer): Int =
        read(buffer, Duration.ZERO)

    override suspend fun read(buffer: MultiplatformBuffer, timeout: Duration): Int {
        (buffer as JVMMultiplatformBuffer)
        if (buffer.remaining() <= 0) throw BufferOverflowException()

        return withSocketTimeout(timeout) {
            try {
                val byteArray = ByteArray(buffer.remaining())
                val readSize = inputStream.read(
                    byteArray,
                    0,
                    buffer.remaining()
                )
                if (readSize > 0) {
                    buffer.putBytes(byteArray.copyOf(readSize))
                    readSize
                } else {
                    throw SocketException("Peer closed connection")
                }
            } catch (timeoutEx: SocketTimeoutException) {
                throw SocketException(SocketErrorReason.TIMEOUT)
            } catch (ioError: IOException) {
                throw SocketException("Peer closed connection")
            } catch (_: Exception) {
                throw SocketException("This should not happen")
            }
        }
    }

    override suspend fun close() {
        kotlin.runCatching { inputStream.close() }
    }

    private fun <T> withSocketTimeout(duration: Duration, bloc: () -> T): T {
        val oldTimeout = socket.soTimeout
        socket.soTimeout = duration.inWholeMilliseconds.toInt()
        val t = bloc()
        socket.soTimeout = oldTimeout
        return t
    }
}