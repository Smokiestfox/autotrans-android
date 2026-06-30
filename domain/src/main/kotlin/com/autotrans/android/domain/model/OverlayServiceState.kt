package com.autotrans.android.domain.model

/** Lifecycle state of the [OverlayForegroundService]. */
sealed interface OverlayServiceState {
    data object Created : OverlayServiceState
    data object Starting : OverlayServiceState
    data object Running : OverlayServiceState
    data object Paused : OverlayServiceState
    data object Stopping : OverlayServiceState
    data object Destroyed : OverlayServiceState
}
