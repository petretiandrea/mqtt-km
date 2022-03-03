package io.github.petretiandrea

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeout
import kotlin.random.Random
import kotlin.test.fail
import kotlin.time.Duration

typealias Wait<T> = suspend () -> T

fun generateRandomMessage(size: Int): String {
    val alphabet = "abcdefghilmnopqrstuvzywx1234567890"
    return (0..size).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
}

object TestCollection {
    fun <T> assertContentEqualsIgnoreOrder(expected: Iterable<T>, actual: Iterable<T>) {
        val actualSet = actual.toSet()
        val expectedIterator = expected.iterator()

        var expectedSize = 0
        while (expectedIterator.hasNext()) {
            val expectedItem = expectedIterator.next()
            if (!actualSet.contains(expectedItem)) {
                fail("Actual: $actualSet not contains the expected item: $expectedItem")
            }
            expectedSize++
        }

        if (expectedSize != actualSet.size) {
            fail("Different length from expected: [$expectedSize] and actual: [${actualSet.size}]")
        }
    }
}


object AsyncTest {

    suspend fun <T> collectCallback(
        toCollect: Int,
        timeout: Duration,
        callbackBloc: suspend Channel<T>.() -> Unit,
    ): Wait<List<T>> {
        val channel = Channel<T>(toCollect)
        callbackBloc.invoke(channel)
        return {
            withTimeout(timeout) {
                channel.receiveAsFlow().take(toCollect).toList().also { channel.close(); }
            }
        }
    }

    suspend fun <T> waitCallback(timeout: Duration, callbackBloc: suspend Channel<T>.() -> Unit): Wait<T> {
        val channel = Channel<T>(1)
        callbackBloc.invoke(channel)
        return { withTimeout(timeout) { channel.receive().also { channel.close() }; } }
    }
}