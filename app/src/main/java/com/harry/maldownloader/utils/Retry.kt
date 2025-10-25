package com.harry.maldownloader.utils

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
    var lastResponse: Response<T>? = null
    
    while (attempt < maxAttempts) {
        try {
            val response = block()
            if (response.code() != 429) {
                return response
            }
            lastResponse = response
        } catch (e: Exception) {
            // Network error, continue to retry
        }
        
        attempt++
        if (attempt < maxAttempts) {
            delay(delayMs)
            delayMs = (delayMs * factor).toLong().coerceAtMost(6000)
        }
    }
    
    // Return last response or try once more
    return lastResponse ?: block()
}