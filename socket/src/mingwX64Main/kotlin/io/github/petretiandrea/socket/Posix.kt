package io.github.petretiandrea.socket

import io.github.petretiandrea.socket.exception.SocketException
import kotlinx.cinterop.*
import platform.posix.*
import platform.windows.addrinfo
import platform.windows.getaddrinfo

actual fun connectSocket(
    hostname: String,
    port: Int,
): Int? {
    memScoped {
        val hints = alloc<addrinfo>()
        val result = allocPointerTo<addrinfo>()

        with(hints) {
            memset(this.ptr, 0, sizeOf<addrinfo>().toULong())
            this.ai_family = platform.posix.AF_INET
            this.ai_socktype = platform.posix.SOCK_STREAM
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
        val isConnected =
            getsockopt(socket.convert(), SOL_SOCKET, SO_ERROR, error.ptr.reinterpret(), sizeOf<IntVar>().toCPointer())
        error.value <= 0 && isConnected != 0
    }
}

actual fun getMaxSendBuffer(
    socket: Int
): Int {
    return memScoped {
        val value = alloc<IntVar>()
        getsockopt(socket.convert(), SOL_SOCKET, SO_SNDBUF, value.ptr.reinterpret(), sizeOf<IntVar>().toCPointer())
        value.value
    }
}

actual fun shutdownInput(socket: Int) {
    shutdown(socket.convert(), SD_RECEIVE)
}

actual fun shutdownOutput(socket: Int) {
    shutdown(socket.convert(), SD_SEND)
}

actual fun send(socket: Int, buf: CValuesRef<ByteVar>?, len: Int, flags: Int): Int {
    return platform.posix.send(socket.convert(), buf, len, flags)
}

actual fun recv(socket: Int, buf: CValuesRef<ByteVar>?, len: Int, flags: Int): Int {
    return platform.posix.recv(socket.convert(), buf, len, flags)
}

actual fun shutdown(socket: Int): Int {
    return platform.posix.shutdown(socket.convert(), platform.posix.SD_BOTH)
}

actual fun close(socket: Int): Int {
    return platform.posix.closesocket(socket.convert())
}

actual fun memset(s: CValuesRef<*>?, c: Int, n: ULong): CPointer<out CPointed>? {
    return platform.posix.memset(s, c, n)
}

actual fun memcpy(
    destination: CPointer<*>?,
    source: CPointer<*>?,
    size: ULong
): CPointer<out CPointed>? = memScoped {
    return platform.posix.memcpy(destination, source, size)
}

actual fun socket(domain: Int, type: Int, protocol: Int): Int {
    return platform.posix.socket(domain, type, protocol).toInt()
}

actual fun MemScope.select(
    nfds: Int,
    readfds: CValuesRef<fd_set>?,
    writefds: CValuesRef<fd_set>?,
    exceptfds: CValuesRef<fd_set>?,
    timeout: Long
): Int {
    val timeoutStruct = alloc<timeval>()
    timeoutStruct.tv_sec = 0
    timeoutStruct.tv_usec = (timeout * 1000).toInt()
    return platform.windows.select(nfds, readfds, writefds, exceptfds, timeoutStruct.ptr)
}

actual fun socketsInit() {
    memScoped { init_sockets() }
}

actual fun socketsCleanup() {
    memScoped { deinit_sockets() }
}