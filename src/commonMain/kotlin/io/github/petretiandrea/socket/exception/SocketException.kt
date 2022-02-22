package io.github.petretiandrea.socket.exception

enum class SocketErrorReason(val message: String) {
    PEER_CLOSED("Peer closed connection"),
    TIMEOUT("Timeout error"),
    IO("IO error"),
    UNKNOWN("unknown error")
}

data class SocketException(
    val reason: SocketErrorReason,
    val additionalMessage: String = ""
) : Exception("${reason.message}, $additionalMessage") {
    constructor(message: String) : this(SocketErrorReason.UNKNOWN, message)
}