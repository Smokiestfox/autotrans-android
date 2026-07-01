package com.autotrans.android.feature.capture.ui

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Primary screen for requesting MediaProjection permission and controlling capture.
 *
 * The screen handles three main states:
 * - **Idle / Denied**: Shows a "Start Capture" button.
 * - **AwaitingPermission**: Launches the system permission dialog.
 * - **CaptureActive**: Shows a "Stop Capture" button.
 *
 * Error states show a Snackbar with the error message.
 */
@Composable
fun CapturePermissionScreen(
    viewModel: CapturePermissionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Launcher for the system MediaProjection permission dialog
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onPermissionResult(result.resultCode, result.data)
    }

    // Side-effects: react to state transitions
    LaunchedEffect(state) {
        when (state) {
            is CapturePermissionState.AwaitingPermission -> {
                val mgr = context.getSystemService(MediaProjectionManager::class.java)
                launcher.launch(mgr.createScreenCaptureIntent())
            }
            is CapturePermissionState.Granted -> {
                val granted = state as CapturePermissionState.Granted
                viewModel.startCapture(granted.resultCode, granted.data)
            }
            is CapturePermissionState.Error -> {
                val error = state as CapturePermissionState.Error
                snackbarHostState.showSnackbar(
                    message = error.message,
                    duration = SnackbarDuration.Long,
                )
            }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A237E), // Deep indigo
                            Color(0xFF283593),
                            Color(0xFF0D47A1),
                        )
                    )
                )
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(32.dp),
            ) {
                // App icon / title
                Icon(
                    imageVector = Icons.Filled.ScreenShare,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(72.dp),
                )
                Text(
                    text = "AutoTrans",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Real-time screen translator",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Status card
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = when (state) {
                                is CapturePermissionState.CaptureActive -> "● Capture active"
                                is CapturePermissionState.Denied -> "Permission denied"
                                is CapturePermissionState.Error -> "Error occurred"
                                else -> "Ready to start"
                            },
                            color = when (state) {
                                is CapturePermissionState.CaptureActive -> Color(0xFF69F0AE)
                                is CapturePermissionState.Denied,
                                is CapturePermissionState.Error -> Color(0xFFFF5252)
                                else -> Color.White
                            },
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = when (state) {
                                is CapturePermissionState.Idle -> "Tap \"Start Capture\" to begin translating your screen."
                                is CapturePermissionState.AwaitingPermission -> "Waiting for screen capture permission..."
                                is CapturePermissionState.CaptureActive -> "Screen is being captured and translated."
                                is CapturePermissionState.Denied -> "Please grant the screen capture permission to use AutoTrans."
                                is CapturePermissionState.Error -> (state as CapturePermissionState.Error).message
                                is CapturePermissionState.Granted -> "Permission granted! Starting capture..."
                            },
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                        )
                    }
                }

                // Primary action button
                val isCaptureActive = state is CapturePermissionState.CaptureActive
                val isLoading = state is CapturePermissionState.AwaitingPermission ||
                    state is CapturePermissionState.Granted

                Button(
                    onClick = {
                        if (isCaptureActive) viewModel.stopCapture()
                        else viewModel.onStartCaptureClicked()
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCaptureActive) Color(0xFFFF5252) else Color(0xFF69F0AE),
                        contentColor = Color.Black,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = if (isCaptureActive) Icons.Filled.Stop else Icons.Filled.ScreenShare,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCaptureActive) "Stop Capture" else "Start Capture",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        }
    }
}
