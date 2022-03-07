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
    s: CValuesRef<*>?,
    c: Int,
    n: ULong
): CPointer<out CPointed>?

expect fun memcpy(
    destination: CPointer<*>?,
    source: CPointer<*>?,
    size: ULong
): CPointer<out CPointed>?

expect fun socket(domain: Int, type: Int, protocol: Int): Int

expect fun MemScope.select(
    nfds: Int,
    readfds: CValuesRef<fd_set>?,
    writefds: CValuesRef<fd_set>?,
    exceptfds: CValuesRef<fd_set>?,
    timeout: Long
): Int

expect fun socketsInit()
expect fun socketsCleanup()