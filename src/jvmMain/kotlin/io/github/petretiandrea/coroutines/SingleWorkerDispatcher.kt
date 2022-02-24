package io.github.petretiandrea.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

actual fun newSingleThreadContext(name: String): CoroutineDispatcher {
    return Executors.newFixedThreadPool(1) { runnable ->
        Thread(runnable, name).apply { isDaemon = true }
    }.asCoroutineDispatcher()
}