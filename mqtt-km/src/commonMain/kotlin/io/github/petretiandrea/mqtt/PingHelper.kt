package io.github.petretiandrea.mqtt

import io.github.petretiandrea.mqtt.core.model.packets.PingReq
import io.github.petretiandrea.mqtt.core.transport.Transport
import kotlinx.datetime.Clock

@ExperimentalUnsignedTypes
class PingHelper(
    private val keepAliveMillis: Long,
    private val transport: Transport
) {
    private var lastReceivedMessage: Long = 0
    private var pingSentAt: Long = 0
    private val keepAliveTimeout = keepAliveMillis - (keepAliveMillis / 2)

    fun updateLastReceivedMessageTime() {
        lastReceivedMessage = Clock.System.now().toEpochMilliseconds()
    }

    fun pongReceived() {
        pingSentAt = 0
    }

    suspend fun sendPing(): Result<Unit> {
        return when {
            needToPing() -> {
                pingSentAt =
                    if (transport.writePacket(PingReq).isSuccess) Clock.System.now().toEpochMilliseconds() else 0
                if (pingSentAt > 0) Result.success(Unit) else Result.failure(Exception("Failed to send ping"))
            }
            isPingExpired() -> Result.failure(Exception("No Ping response received!"))
            else -> Result.failure(Exception("Unknown error pinger"))
        }
    }

    private fun needToPing(): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        return pingSentAt <= 0 && ((now - lastReceivedMessage) > keepAliveTimeout)
    }

    private fun isPingExpired(): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        return pingSentAt > 0 && (now - pingSentAt) > keepAliveTimeout
    }
}