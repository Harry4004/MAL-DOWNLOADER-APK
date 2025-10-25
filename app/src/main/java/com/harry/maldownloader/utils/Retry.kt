package com.harry.maldownloader

import kotlinx.coroutines.delay
import retrofit2.Response

suspend fun <T> retryWithBackoff(
    maxAttempts: Int = 3,
    initialDelayMs: Long = 800,
    factor: Double = 2.0,
    block: suspend () -> Response<T>
): Response<T> {
    var attempt = 0
    var delayMs = initialDelayMs
    var last: Response<T>? = null
    while (attempt < maxAttempts) {
        val resp = runCatching { block() }.getOrNull()
        if (resp == null) {
            attempt++
        } else {
            if (resp.code() != 429) return resp
            last = resp
            attempt++
        }
        if (attempt < maxAttempts) delay(delayMs)
        delayMs = (delayMs * factor).toLong().coerceAtMost(6000)
    }
    return last ?: block() // last try
}
