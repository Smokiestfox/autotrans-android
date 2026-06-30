package com.autotrans.android.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Injectable coroutine dispatcher provider.
 * Swap all dispatchers with [TestAppDispatchers] in unit tests for deterministic behavior.
 * See PIPELINE.md §3 and TESTING_STRATEGY.md §7.
 */
@Singleton
open class AppDispatchers @Inject constructor() {
    open val default: CoroutineDispatcher = Dispatchers.Default
    open val io: CoroutineDispatcher = Dispatchers.IO
    open val main: CoroutineDispatcher = Dispatchers.Main
}
