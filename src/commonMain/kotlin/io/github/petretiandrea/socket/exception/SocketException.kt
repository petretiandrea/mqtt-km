package io.github.petretiandrea.socket.exception

data class SocketException(override val message: String) : Exception(message)