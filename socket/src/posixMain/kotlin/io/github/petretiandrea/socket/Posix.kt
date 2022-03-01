package io.github.petretiandrea.socket

import kotlinx.cinterop.*
import platform.posix.fd_set

expect fun connectSocket(
    hostname: String,
    port: Int,
): Int?

expect fun isSocketConnected(
    socket: Int
): Boolean


expect fun getMaxSendBuffer(
    socket: Int
): Int

expect fun send(
    socket: Int,
    buf: CValuesRef<ByteVar>?,
    len: Int,
    flags: Int
): Int

expect fun recv(
    socket: Int,
    buf: CValuesRef<ByteVar>?,
    len: Int,
    flags: Int
): Int

expect fun shutdown(socket: Int): Int

expect fun shutdownInput(socket: Int)

expect fun shutdownOutput(socket: Int)

expect fun close(socket: Int): Int

expect fun memset(
    __s: CValuesRef<*>?,
    __c: Int,
    __n: ULong
): CPointer<out CPointed>?


expect fun socket(__domain: Int, __type: Int, __protocol: Int): Int

expect fun MemScope.select(
    __nfds: Int,
    __readfds: CValuesRef<fd_set>?,
    __writefds: CValuesRef<fd_set>?,
    __exceptfds: CValuesRef<fd_set>?,
    timeout: Long
): Int

expect fun socketsInit()
expect fun socketsCleanup()