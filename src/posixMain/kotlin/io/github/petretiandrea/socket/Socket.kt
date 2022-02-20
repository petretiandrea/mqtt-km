package io.github.petretiandrea.socket

import io.github.petretiandrea.socket.buffer.BufferOverflowException
import io.github.petretiandrea.socket.buffer.MultiplatformBuffer
import io.github.petretiandrea.socket.buffer.NativeMultiplatformBuffer
import io.github.petretiandrea.socket.exception.SocketException
import io.github.petretiandrea.socket.stream.InputStream
import io.github.petretiandrea.socket.stream.OutputStream
import kotlinx.cinterop.*
import platform.posix.*

@OptIn(ExperimentalUnsignedTypes::class)
actual fun createSocket(hostname: String, port: Int): SocketInterface {
    memScoped {
        Socket.initializeSockets()
        val socketFd = platform.posix.socket(AF_INET, SOCK_STREAM, 0).toInt()
        if (socketFd == -1) {
            throw SocketException("Invalid socket: error $errno")
        }
        val targetAddress = sockaddrIn(AF_INET.convert(), port.convert())
        if (inet_pton(AF_INET, hostname, targetAddress.sin_addr.ptr) <= 0) {
            throw SocketException("Invalid address: $hostname")
        }
        if (connect(socketFd.convert(), targetAddress.ptr.reinterpret(), sizeOf<sockaddr>().toInt()) < 0) {
            throw SocketException("Connection failed!")
        }
        return Socket(
            socketFd = socketFd,
            maxSendBufferSize = Socket.getMaxSendBuffOption(socketFd)
        )
    }
}

@ExperimentalUnsignedTypes
class Socket(
    private val socketFd: Int,
    private val maxSendBufferSize: Int
) : SocketInterface {

    companion object {
        fun initializeSockets() {
            socketsInit()
        }

        fun getMaxSendBuffOption(socketFd: Int): Int {
            return memScoped {
                val value = alloc<IntVar>()
                getsockopt(socketFd.convert(), SOL_SOCKET, SO_SNDBUF, value.ptr.reinterpret(), cValuesOf(sizeOf<IntVar>().toInt()))
                value.value
            }
        }
    }
    override fun inputStream(): InputStream = SocketInputStream(socketFd)

    override fun outputStream(): OutputStream = SocketOutputStream(socketFd, maxSendBufferSize)

    override suspend fun close() {
        shutdown(socketFd)
        platform.posix.close(socketFd)
    }

    private class SocketInputStream(
        private val socketFd: Int
    ) : InputStream {
        override suspend fun read(buffer: MultiplatformBuffer): Int {
            val nativeBuffer = buffer as NativeMultiplatformBuffer
            if (nativeBuffer.remaining() <= 0) throw BufferOverflowException()

            val readSize = platform.posix.recv(
                socketFd.convert(),
                nativeBuffer.nativePointer() + nativeBuffer.cursor,
                nativeBuffer.remaining(),
                0
            )
            when {
                readSize > 0 -> {
                    nativeBuffer.cursor = readSize
                }
                else -> throw SocketException("Peer closed connection")
            }

            return readSize
        }

        override suspend fun close() {
            shutdown(socketFd.convert(), SD_RECEIVE)
        }
    }

    private class SocketOutputStream(
        private val socketFd: Int,
        private val maxSendBufferSize: Int
    ) : OutputStream {
        override suspend fun send(buffer: MultiplatformBuffer): Boolean {
            val nativeBuffer = buffer as NativeMultiplatformBuffer
            if (nativeBuffer.limit > maxSendBufferSize) {
                val writeSize =
                    platform.posix.send(socketFd.convert(), buffer.nativePointer() + buffer.cursor, buffer.remaining(), 0)
                when {
                    writeSize >= 0 -> { buffer.cursor += writeSize; buffer.hasRemaining() }
                    else -> throw SocketException("Peer closed connection")
                }
            } else {
                while (nativeBuffer.remaining() > 0) {
                    val writeSize =
                        platform.posix.send(socketFd.convert(), buffer.nativePointer() + buffer.cursor, buffer.remaining(), 0)
                    if (writeSize >= 0) {
                        buffer.cursor = buffer.cursor + writeSize
                    } else {
                        throw SocketException("Peer closed connection")
                    }
                }
            }
            return buffer.remaining() == 0
        }

        override suspend fun close() {
            shutdown(socketFd.convert(), SD_SEND)
        }

    }
}

