package io.github.petretiandrea.coroutines

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.native.concurrent.Worker

actual fun newSingleThreadContext(name: String): CoroutineDispatcher {
    return SingleWorkerDispatcher(name)
}

/**
 * Porting of original single worker dispatcher of koltinx.coroutines 1.6.0.
 * This allow to use fixed coroutine context with 1.6.0-dandroid variant which support arm but miss this class.
 */
@OptIn(InternalCoroutinesApi::class)
internal class SingleWorkerDispatcher(name: String) : CloseableCoroutineDispatcher(), Delay {
    private val worker = Worker.start(name = name)

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        worker.executeAfter(0L) { block.run() }
    }

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        worker.executeAfter(timeMillis.toMicrosSafe()) {
            with(continuation) { resumeUndispatched(Unit) }
        }
    }

    @OptIn(InternalCoroutinesApi::class)
    override fun invokeOnTimeout(timeMillis: Long, block: Runnable, context: CoroutineContext): DisposableHandle {
        // No API to cancel on timeout
        worker.executeAfter(timeMillis.toMicrosSafe()) { block.run() }
        return NonDisposableHandle
    }

    override fun close() {
        worker.requestTermination().result // Note: calling "result" blocks
    }

    private fun Long.toMicrosSafe(): Long {
        val result = this * 1000
        return if (result > this) result else Long.MAX_VALUE
    }
}
