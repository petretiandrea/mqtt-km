package io.github.petretiandrea.socket

import io.github.petretiandrea.socket.exception.SocketException
import kotlinx.cinterop.*
import platform.posix.*

actual fun connectSocket(
    hostname: String,
    port: Int,
): Int? {
    memScoped {
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

        return with(result) {
            var socketFd: Int? = null
            var next: addrinfo? = this.pointed
            while (next != null && socketFd == null) {
                val socket = socket(next.ai_family, next.ai_socktype, next.ai_protocol)
                if (socket != -1) {
                    val connected = connect(socket.convert(), next.ai_addr, next.ai_addrlen.convert())
                    if (connected >= 0 || posix_errno() == EINPROGRESS) {
                        socketFd = socket
                    } else {
                        close(socket)
                        next = next.ai_next?.pointed
                    }
                }
            }
            socketFd
        }
    }
}

actual fun isSocketConnected(
    socket: Int
): Boolean {
    return memScoped {
        val error = alloc<IntVar>()
        val isConnected = getsockopt(socket.convert(), SOL_SOCKET, SO_ERROR, error.ptr, sizeOf<IntVar>().toCPointer())
        error.value <= 0 && isConnected != 0
    }
}

actual fun getMaxSendBuffer(
    socket: Int
): Int {
    return memScoped {
        val value = alloc<IntVar>()
        getsockopt(socket.convert(), SOL_SOCKET, SO_SNDBUF, value.ptr, sizeOf<IntVar>().toCPointer())
        value.value
    }
}

actual fun shutdownInput(socket: Int) {
    shutdown(socket.convert(), SHUT_RD)
}

actual fun shutdownOutput(socket: Int) {
    shutdown(socket.convert(), SHUT_WR)
}

actual fun send(socket: Int, buf: CValuesRef<ByteVar>?, len: Int, flags: Int): Int {
    return platform.posix.send(socket.convert(), buf, len.convert(), flags or MSG_NOSIGNAL).convert()
}

actual fun recv(socket: Int, buf: CValuesRef<ByteVar>?, len: Int, flags: Int): Int {
    return platform.posix.recv(socket, buf, len.convert(), flags).convert()
}

actual fun shutdown(socket: Int): Int {
    return shutdown(socket, SHUT_RDWR)
}

actual fun close(socket: Int): Int {
    return platform.posix.close(socket.convert())
}

actual fun memset(__s: CValuesRef<*>?, __c: Int, __n: ULong): CPointer<out CPointed>? {
    return platform.posix.memset(__s, __c, __n)
}

actual fun socket(__domain: Int, __type: Int, __protocol: Int): Int {
    return platform.posix.socket(__domain, __type, __protocol)
}

actual fun MemScope.select(
    __nfds: Int,
    __readfds: CValuesRef<fd_set>?,
    __writefds: CValuesRef<fd_set>?,
    __exceptfds: CValuesRef<fd_set>?,
    timeout: Long
): Int {
    val timeoutStruct = alloc<timeval>()
    timeoutStruct.tv_sec = 0
    timeoutStruct.tv_usec = timeout * 1000
    return select(__nfds, __readfds, __writefds, __exceptfds, timeoutStruct.ptr)
}

actual fun socketsInit() {
    memScoped { init_sockets() }
}
actual fun socketsCleanup() {
    memScoped { deinit_sockets() }
}