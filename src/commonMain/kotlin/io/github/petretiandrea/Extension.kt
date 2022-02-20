package io.github.petretiandrea

fun <T, O> Result<T>.flatMap(map: (T) -> Result<O>): Result<O> {
    return this.fold(
        onSuccess = { map(it) },
        onFailure = { Result.failure(it) }
    )
}