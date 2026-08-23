package com.privacyshield.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.*
import com.privacyshield.app.model.PrivacyState
import com.privacyshield.app.ui.*
import com.privacyshield.app.ui.theme.PrivacyShieldTheme
import com.privacyshield.app.viewmodel.PrivacyViewModel

@ExperimentalGetImage
class MainActivity : FragmentActivity() {

    private val viewModel: PrivacyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Observe FLAG_SECURE changes from ViewModel
        observeFlagSecure()

        setContent {
            PrivacyShieldTheme {
                MainContent()
            }
        }
    }

    private fun observeFlagSecure() {
        // Collect flagSecure state and apply to window
        // We use lifecycle-aware collection via LaunchedEffect in Compose below
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    private fun MainContent() {
        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        val state    by viewModel.privacyState.collectAsStateWithLifecycle()
        val settings by viewModel.settings.collectAsStateWithLifecycle()
        val flagSecure by viewModel.flagSecure.collectAsStateWithLifecycle()

        var showSettings by remember { mutableStateOf(false) }

        // Apply FLAG_SECURE whenever it changes
        LaunchedEffect(flagSecure) {
            if (flagSecure) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }

        // Camera permission state
        val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)

        Box(modifier = Modifier.fillMaxSize()) {

            // ── Main Screen ───────────────────────────────────────────
            AnimatedContent(
                targetState = showSettings,
                transitionSpec = {
                    if (targetState) {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "screen_transition",
            ) { isSettings ->
                if (isSettings) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack    = { showSettings = false },
                    )
                } else {
                    HomeScreen(
                        viewModel            = viewModel,
                        onNavigateToSettings = { showSettings = true },
                        lifecycleOwner       = lifecycleOwner,
                    )
                }
            }

            // ── Privacy Overlay — always on top ───────────────────────
            if (state.isOverlayActive) {
                PrivacyOverlayScreen(
                    state    = state,
                    settings = settings,
                    viewModel = viewModel,
                )
            }

            // ── Permission Rationale ──────────────────────────────────
            if (!cameraPermission.status.isGranted && state.isProtectionEnabled) {
                CameraPermissionDialog(
                    cameraPermission  = cameraPermission,
                    onDisableShield   = { viewModel.disable() },
                )
            }
        }
    }
}

// ─── Permission Dialog ─────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun CameraPermissionDialog(
    cameraPermission: PermissionState,
    onDisableShield: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDisableShield,
        icon = {
            androidx.compose.material3.Icon(
                androidx.compose.material.icons.Icons.Default.Info,
                contentDescription = null,
                tint = com.privacyshield.app.ui.theme.PrimaryBlue,
            )
        },
        title = { androidx.compose.material3.Text("Camera Permission Required") },
        text  = {
            androidx.compose.material3.Text(
                "Privacy Shield needs the front camera to detect faces. " +
                "Grant the camera permission to enable protection."
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { cameraPermission.launchPermissionRequest() }) {
                androidx.compose.material3.Text("Grant Permission")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDisableShield) {
                androidx.compose.material3.Text("Disable Shield")
            }
        },
        containerColor = com.privacyshield.app.ui.theme.Surface,
        titleContentColor = androidx.compose.ui.graphics.Color.White,
        textContentColor  = com.privacyshield.app.ui.theme.TextSecondary,
    )
}
