package com.autotrans.android.feature.capture.ui

import android.media.projection.MediaProjectionManager

/**
 * Represents the UI state of the screen capture permission request screen.
 */
sealed interface CapturePermissionState {

    /** Initial state — no action taken yet. */
    data object Idle : CapturePermissionState

    /** Waiting for the user to interact with the system permission dialog. */
    data object AwaitingPermission : CapturePermissionState

    /**
     * The user granted permission.
     * @param resultCode The result code from [MediaProjectionManager.createScreenCaptureIntent].
     */
    data class Granted(val resultCode: Int, val data: android.content.Intent) :
        CapturePermissionState

    /** The user denied or dismissed the permission dialog. */
    data object Denied : CapturePermissionState

    /** The capture session is currently active. */
    data object CaptureActive : CapturePermissionState

    /** An error occurred. */
    data class Error(val message: String) : CapturePermissionState
}
