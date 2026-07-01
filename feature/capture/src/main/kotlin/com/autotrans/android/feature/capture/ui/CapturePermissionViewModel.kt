package com.autotrans.android.feature.capture.ui

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autotrans.android.feature.capture.repository.CaptureRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel that manages the MediaProjection permission flow and capture lifecycle.
 *
 * Responsibilities:
 * - Holds and exposes [CapturePermissionState] to the UI.
 * - Delegates actual capture work to [CaptureRepository].
 * - Survives configuration changes (screen rotation during permission dialog).
 */
@HiltViewModel
class CapturePermissionViewModel @Inject constructor(
    private val captureRepository: CaptureRepositoryImpl,
) : ViewModel() {

    private val _state = MutableStateFlow<CapturePermissionState>(CapturePermissionState.Idle)

    /** Observable UI state for the permission screen. */
    val state: StateFlow<CapturePermissionState> = _state.asStateFlow()

    /**
     * Called when the user taps the "Start Capture" button.
     * The UI should then launch [MediaProjectionManager.createScreenCaptureIntent].
     */
    fun onStartCaptureClicked() {
        _state.value = CapturePermissionState.AwaitingPermission
        Timber.d("Waiting for MediaProjection permission...")
    }

    /**
     * Called after the system permission dialog returns a result.
     *
     * @param resultCode [Activity.RESULT_OK] if granted, [Activity.RESULT_CANCELED] if denied.
     * @param data The intent returned by the permission activity.
     */
    fun onPermissionResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            Timber.i("MediaProjection permission granted.")
            _state.value = CapturePermissionState.Granted(resultCode, data)
        } else {
            Timber.w("MediaProjection permission denied.")
            _state.value = CapturePermissionState.Denied
        }
    }

    /**
     * Starts the continuous capture session using the granted MediaProjection token.
     * Should only be called after state is [CapturePermissionState.Granted].
     */
    fun startCapture(resultCode: Int, data: Intent) {
        viewModelScope.launch {
            try {
                _state.value = CapturePermissionState.CaptureActive
                captureRepository.startContinuousCapture(resultCode, data)
                    .collect { result ->
                        result.onFailure { error ->
                            Timber.e(error, "Capture stream error")
                            _state.value = CapturePermissionState.Error(
                                error.message ?: "Unknown capture error"
                            )
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to start capture")
                _state.value = CapturePermissionState.Error(e.message ?: "Capture failed")
            }
        }
    }

    /** Stops the current capture session and resets state to [CapturePermissionState.Idle]. */
    fun stopCapture() {
        viewModelScope.launch {
            try {
                captureRepository.stopCapture()
                Timber.i("Capture stopped.")
            } catch (e: Exception) {
                Timber.w(e, "Error stopping capture")
            } finally {
                _state.value = CapturePermissionState.Idle
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Ensure capture is released if the ViewModel is destroyed
        viewModelScope.launch {
            runCatching { captureRepository.stopCapture() }
        }
    }
}
