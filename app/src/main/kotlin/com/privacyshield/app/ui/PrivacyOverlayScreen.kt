package com.privacyshield.app.ui

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.privacyshield.app.model.AppSettings
import com.privacyshield.app.model.OverlayStyle
import com.privacyshield.app.model.PrivacyState
import com.privacyshield.app.ui.theme.*
import com.privacyshield.app.viewmodel.PrivacyViewModel

@androidx.camera.core.ExperimentalGetImage
@Composable
fun PrivacyOverlayScreen(
    state: PrivacyState,
    settings: AppSettings,
    viewModel: PrivacyViewModel,
) {
    val context = LocalContext.current
    var isAuthenticating by remember { mutableStateOf(false) }

    // Full-screen animated entry
    AnimatedVisibility(
        visible = state.isOverlayActive,
        enter = fadeIn(tween(300)) + scaleIn(tween(400), initialScale = 0.92f),
        exit  = fadeOut(tween(250)) + scaleOut(tween(300), targetScale = 0.95f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayBackground(settings.overlayStyle))
                .clickable { handleDismiss(context, settings, isAuthenticating, viewModel) { isAuthenticating = it } },
            contentAlignment = Alignment.Center,
        ) {
            OverlayContent(
                state           = state,
                isAuthenticating = isAuthenticating,
                onDismiss       = {
                    handleDismiss(context, settings, isAuthenticating, viewModel) { isAuthenticating = it }
                },
            )
        }
    }
}

private fun overlayBackground(style: OverlayStyle) = when (style) {
    OverlayStyle.BLUR         -> Color.Black.copy(alpha = 0.80f)  // blur applied via modifier
    OverlayStyle.DARK_CURTAIN -> Color.Black
    OverlayStyle.MOSAIC       -> Color(0xFF0D0D30)
}

private fun handleDismiss(
    context: Context,
    settings: AppSettings,
    isAuthenticating: Boolean,
    viewModel: PrivacyViewModel,
    setAuthenticating: (Boolean) -> Unit,
) {
    if (isAuthenticating) return

    if (settings.requireBiometric) {
        setAuthenticating(true)
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            context as FragmentActivity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    setAuthenticating(false)
                    viewModel.dismissOverlay()
                }
                override fun onAuthenticationError(code: Int, msg: CharSequence) {
                    setAuthenticating(false)
                }
                override fun onAuthenticationFailed() {
                    setAuthenticating(false)
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authenticate")
            .setSubtitle("Verify identity to dismiss privacy overlay")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        prompt.authenticate(info)
    } else {
        viewModel.dismissOverlay()
    }
}

// ─── Overlay Content ───────────────────────────────────────────────────────

@Composable
private fun OverlayContent(
    state: PrivacyState,
    isAuthenticating: Boolean,
    onDismiss: () -> Unit,
) {
    val isPeekerAlert = state == PrivacyState.PEEKER_ALERT
    val alertColor    = if (isPeekerAlert) AlertRed else WarmOrange

    val pulseScale by rememberInfiniteTransition(label = "overlay_pulse").animateFloat(
        initialValue  = 0.92f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = EaseInOut), RepeatMode.Reverse),
        label         = "overlay_scale",
    )

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.weight(1.5f))

        // ── Pulsing Alert Icon ────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                .clip(CircleShape)
                .background(alertColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector         = if (isPeekerAlert) Icons.Default.VisibilityOff else Icons.Default.Lock,
                contentDescription  = null,
                tint                = alertColor,
                modifier            = Modifier.size(56.dp),
            )
        }

        Spacer(Modifier.height(32.dp))

        // ── Title ─────────────────────────────────────────────────────
        Text(
            text       = if (isPeekerAlert) "Intruder Detected!" else "Screen Locked",
            color      = alertColor,
            fontSize   = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign  = TextAlign.Center,
            letterSpacing = (-0.5).sp,
        )

        Spacer(Modifier.height(12.dp))

        // ── Subtitle ──────────────────────────────────────────────────
        Text(
            text       = if (isPeekerAlert)
                "Someone else is looking at your screen.\nYour content is hidden for privacy."
            else
                "You stepped away. Screen locked for your privacy.",
            color      = Color.White.copy(alpha = 0.60f),
            fontSize   = 15.sp,
            lineHeight = 22.sp,
            textAlign  = TextAlign.Center,
        )

        Spacer(Modifier.weight(1.5f))

        // ── Dismiss Button ────────────────────────────────────────────
        Button(
            onClick  = onDismiss,
            enabled  = !isAuthenticating,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = alertColor),
        ) {
            if (isAuthenticating) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(22.dp),
                    color       = Color.White,
                    strokeWidth = 2.5.dp,
                )
            } else {
                Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Authenticate & Resume", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            "Tap anywhere or the button above to dismiss",
            color    = Color.White.copy(alpha = 0.30f),
            fontSize = 12.sp,
        )

        Spacer(Modifier.height(32.dp))
    }
}
