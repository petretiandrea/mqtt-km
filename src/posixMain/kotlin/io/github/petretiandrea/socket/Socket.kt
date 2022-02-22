package io.github.petretiandrea.socket

import io.github.petretiandrea.socket.buffer.BufferOverflowException
import io.github.petretiandrea.socket.buffer.MultiplatformBuffer
import io.github.petretiandrea.socket.buffer.NativeMultiplatformBuffer
import io.github.petretiandrea.socket.exception.SocketErrorReason
import io.github.petretiandrea.socket.exception.SocketException
import io.github.petretiandrea.socket.stream.InputStream
import io.github.petretiandrea.socket.stream.OutputStream
import kotlinx.cinterop.*
import platform.posix.*
import platform.windows.addrinfo
import platform.windows.getaddrinfo
import platform.windows.select
import kotlin.time.Duration
import kotlin.time.DurationUnit

@OptIn(ExperimentalUnsignedTypes::class)
actual fun createSocket(hostname: String, port: Int): SocketInterface {
    memScoped {
        Socket.initializeSockets()
        val hints = alloc<addrinfo>()
        val result = allocPointerTo<addrinfo>()

        with(hints) {
            memset(this.ptr, 0, sizeOf<addrinfo>().toULong())
            this.ai_family = AF_INET
            this.ai_socktype = SOCK_STREAM
            this.ai_flags = 0
            this.ai_protocol = 0
        }

        if (getaddrinfo(hostname, port.toString(), hints.ptr, result.ptr) != 0) {
            throw SocketException("Invalid address: $hostname")
        }

        var socketInterface: SocketInterface? = null
        with(result) {
            var next : addrinfo? = this.pointed
            while(next != null && socketInterface == null){
                val socket = socket(next.ai_family, next.ai_socktype, next.ai_protocol)
                if (socket != -1) {
                    val connected = connect(socket.convert(), next.ai_addr, next.ai_addrlen.toInt())
                    if (connected >= 0 || posix_errno() == EINPROGRESS) {
                        socketInterface = Socket(
                            socketFd = socket,
                            maxSendBufferSize = Socket.getMaxSendBuffOption(socket)
                        )
                    } else {
                        close(socket)
                        next = next.ai_next?.pointed
                    }
                }
            }
        }
        return socketInterface ?: throw SocketException("Cannot connect to $hostname on $port")
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

    private val socketInputStream = SocketInputStream(socketFd)
    private val socketOutputStream = SocketOutputStream(socketFd, maxSendBufferSize)

    override fun inputStream(): InputStream = socketInputStream
    override fun outputStream(): OutputStream = socketOutputStream

    override fun isConnected(): Boolean {
        return memScoped {
            val error = alloc<IntVar>()
            val isConnected = getsockopt(socketFd.convert(), SOL_SOCKET, SO_ERROR, error.ptr.reinterpret(), sizeOf<IntVar>().toCPointer())
            error.value <= 0 && isConnected != 0
        }
    }

    override suspend fun close() {
        inputStream().close()
        outputStream().close()
        shutdown(socketFd)
        platform.posix.close(socketFd)
    }

    private class SocketInputStream(
        private val socketFd: Int
    ) : InputStream {

        private val readFds = nativeHeap.alloc<fd_set>()

        override suspend fun read(buffer: MultiplatformBuffer): Int {
            return read(buffer, Duration.ZERO)
        }

        override suspend fun read(buffer: MultiplatformBuffer, timeout: Duration): Int {
            val nativeBuffer = buffer as NativeMultiplatformBuffer
            if (nativeBuffer.remaining() <= 0) throw BufferOverflowException()

            val ready = memScoped {
                posix_FD_ZERO(readFds.ptr)
                posix_FD_SET(socketFd.convert(), readFds.ptr)
                select(2, readFds.ptr, null, null, timeout.inWholeMilliseconds)
            }

            return when (ready) {
                0 -> throw SocketException(SocketErrorReason.TIMEOUT)
                -1 -> throw SocketException("Peer closed connection")
                else -> if (posix_FD_ISSET(socketFd.convert(), readFds.ptr) == 1) {
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
                    readSize
                } else throw SocketException("This should not happen")
            }
        }

        override suspend fun close() {
            nativeHeap.free(readFds)
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

