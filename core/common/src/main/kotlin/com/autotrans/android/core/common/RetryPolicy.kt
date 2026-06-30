package com.autotrans.android.core.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * Executes [block] up to [times] times with exponential backoff between retries.
 * [CancellationException] is never retried and always propagates immediately.
 *
 * @param times Total number of attempts (including the first).
 * @param initialDelay Delay before the second attempt in milliseconds.
 * @param factor Backoff multiplier applied after each failed attempt.
 * @param isRetryable Predicate to decide whether a given error warrants a retry.
 *
 * See ERROR_HANDLING.md §4 for the retry policy table.
 */
suspend fun <T> withRetry(
    times: Int,
    initialDelay: Long = 200L,
    factor: Double = 2.0,
    isRetryable: (Throwable) -> Boolean = { it !is CancellationException },
    block: suspend () -> Result<T>
): Result<T> {
    var currentDelay = initialDelay
    repeat(times - 1) {
        val result = block()
        if (result.isSuccess) return result
        val error = result.exceptionOrNull()!!
        if (!isRetryable(error)) return result
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong()
    }
    return block()
}
