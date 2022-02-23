package io.github.petretiandrea

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration

typealias Wait<T> = suspend () -> T

object AsyncTest {
    suspend fun <T> waitCallback(timeout: Duration, callbackBloc: suspend Channel<T>.() -> Unit): Wait<T> {
        val channel = Channel<T>(1)
        callbackBloc.invoke(channel)
        return { withTimeout(timeout) { channel.receive().also { channel.close() }; } }
    }
}