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
import kotlin.time.Duration

@OptIn(ExperimentalUnsignedTypes::class)
actual fun createSocket(hostname: String, port: Int): SocketInterface {
    Socket.initializeSockets()

    return connectSocket(hostname, port)?.let { socketFd ->
        Socket(
            socketFd = socketFd,
            socketInputStream = SocketInputStream(socketFd),
            socketOutputStream = SocketOutputStream(socketFd, getMaxSendBuffer(socketFd))
        )
    } ?: throw SocketException("Cannot connect to $hostname on $port")
}

@ExperimentalUnsignedTypes
private class Socket(
    private val socketFd: Int,
    private val socketInputStream: SocketInputStream,
    private val socketOutputStream: SocketOutputStream,
) : SocketInterface {

    override fun inputStream(): InputStream = socketInputStream
    override fun outputStream(): OutputStream = socketOutputStream

    override fun isConnected(): Boolean {
        return isSocketConnected(socketFd)
    }

    override suspend fun close() {
        inputStream().close()
        outputStream().close()
        shutdown(socketFd)
        close(socketFd)
    }

    @ThreadLocal
    companion object {
        private var initialized: Boolean = false
        internal fun initializeSockets() {
            if (!initialized) {
                initialized = true
                socketsInit()
            }
        }
    }
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
                val readSize = recv(
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
        shutdownInput(socketFd)
    }
}

private class SocketOutputStream(
    private val socketFd: Int,
    private val maxSendBufferSize: Int
) : OutputStream {
    override suspend fun send(buffer: MultiplatformBuffer): Boolean {
        val nativeBuffer = buffer as NativeMultiplatformBuffer
        if (nativeBuffer.limit > maxSendBufferSize) {
            val writeSize = send(
                socketFd.convert(),
                buffer.nativePointer() + buffer.cursor,
                buffer.remaining(),
                0
            )
            when {
                writeSize >= 0 -> {
                    buffer.cursor += writeSize; buffer.hasRemaining()
                }
                else -> throw SocketException("Peer closed connection")
            }
        } else {
            while (nativeBuffer.remaining() > 0) {
                val writeSize = send(
                    socketFd.convert(),
                    buffer.nativePointer() + buffer.cursor,
                    buffer.remaining(),
                    0
                )
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
        shutdownOutput(socketFd)
    }
}

