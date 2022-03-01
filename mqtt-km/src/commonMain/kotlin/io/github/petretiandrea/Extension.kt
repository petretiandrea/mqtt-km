package io.github.petretiandrea

internal suspend fun <T, O> Result<T>.flatMap(map: suspend (T) -> Result<O>): Result<O> {
    return this.fold(
        onSuccess = { map(it) },
        onFailure = { Result.failure(it) }
    )
}