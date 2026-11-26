package com.talaxy.wowsinsight.util

import kotlinx.coroutines.CancellationException

/**
 * [runCatching] for coroutine code: a cancelled coroutine must keep unwinding
 * instead of being reported as a failure of [block].
 */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }
