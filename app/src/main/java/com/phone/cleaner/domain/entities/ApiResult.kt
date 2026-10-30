package com.phone.cleaner.domain.entities

/**
 * What every repository returns. There is no backend yet, so this lives here; it moves to
 * `data/remote/` unchanged when the network layer from CLAUDE.md ("Reference implementation") arrives.
 */
sealed class ApiResult<T>(
    val data: T? = null,
    val message: String? = null,
) {
    class Success<T>(data: T) : ApiResult<T>(data = data)
    class Failure<T>(message: String) : ApiResult<T>(message = message)
}
