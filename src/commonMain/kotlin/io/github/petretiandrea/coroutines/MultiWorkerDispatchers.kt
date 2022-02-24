package io.github.petretiandrea.coroutines

import kotlinx.coroutines.CoroutineDispatcher

expect fun newSingleThreadContext(name: String): CoroutineDispatcher