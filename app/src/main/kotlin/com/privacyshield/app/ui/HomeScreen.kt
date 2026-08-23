package com.privacyshield.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privacyshield.app.model.*
import com.privacyshield.app.ui.theme.*
import com.privacyshield.app.viewmodel.PrivacyViewModel

@androidx.camera.core.ExperimentalGetImage
@Composable
fun HomeScreen(
    viewModel: PrivacyViewModel,
    onNavigateToSettings: () -> Unit,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
) {
    val state    by viewModel.privacyState.collectAsState()
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            HomeTopBar(onSettingsClick = onNavigateToSettings)
        },
        containerColor = Background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))

            // ── Animated Shield Hero ──────────────────────────────────
            ShieldHero(state = state)

            Spacer(Modifier.height(28.dp))

            // ── Status Card ───────────────────────────────────────────
            StatusCard(state = state)

            Spacer(Modifier.height(16.dp))

            // ── Main Toggle ───────────────────────────────────────────
            PowerToggle(
                state = state,
                onEnable  = { viewModel.enable(lifecycleOwner) },
                onDisable = { viewModel.disable() },
            )

            Spacer(Modifier.height(16.dp))

            // ── Info Tiles ────────────────────────────────────────────
            InfoTilesRow(settings = settings)

            Spacer(Modifier.height(16.dp))

            // ── How It Works ──────────────────────────────────────────
            HowItWorksCard()

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Top Bar ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(onSettingsClick: () -> Unit) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null,
                    tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Privacy Shield", fontWeight = FontWeight.SemiBold)
            }
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor         = Background,
            titleContentColor      = Color.White,
            actionIconContentColor = TextSecondary,
        ),
    )
}

// ─── Shield Hero ───────────────────────────────────────────────────────────

@Composable
private fun ShieldHero(state: PrivacyState) {
    val color = state.brandColor()
    val icon  = state.shieldIcon()

    val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue  = if (state == PrivacyState.PEEKER_ALERT) 1.10f else 1.03f,
        animationSpec = infiniteRepeatable(
            tween(if (state == PrivacyState.PEEKER_ALERT) 400 else 2000, easing = EaseInOut),
            RepeatMode.Reverse,
        ),
        label = "scale",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(130.dp)
                .animateContentSize(),
        ) {
            // Glow ring
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f))
            )
            // Icon
            Icon(
                imageVector         = icon,
                contentDescription  = state.displayLabel,
                tint                = color,
                modifier            = Modifier
                    .size(58.dp)
                    .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale),
            )
        }

        Spacer(Modifier.height(16.dp))

        AnimatedContent(targetState = state.displayLabel, label = "label") { label ->
            Text(
                text       = label,
                color      = color,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            )
        }

        Spacer(Modifier.height(6.dp))

        AnimatedContent(targetState = state.statusMessage, label = "msg") { msg ->
            Text(
                text      = msg,
                color     = TextSecondary,
                fontSize  = 13.sp,
                lineHeight = 19.sp,
                modifier  = Modifier.padding(horizontal = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

// ─── Status Card ───────────────────────────────────────────────────────────

@Composable
private fun StatusCard(state: PrivacyState) {
    val color = state.brandColor()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Surface),
        border   = BorderStroke(1.5.dp, color.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier            = Modifier.padding(16.dp),
            verticalAlignment   = Alignment.CenterVertically,
        ) {
            Box(
                modifier         = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(state.statusIcon(), contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(state.displayLabel, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                Spacer(Modifier.height(2.dp))
                Text(state.cardSubtitle(), fontSize = 12.sp, color = TextSecondary)
            }
            if (state.isProtectionEnabled) {
                PulsingDot(color = color)
            }
        }
    }
}

// ─── Power Toggle ──────────────────────────────────────────────────────────

@Composable
private fun PowerToggle(
    state: PrivacyState,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
) {
    val isEnabled = state.isProtectionEnabled
    var isLoading by remember { mutableStateOf(false) }

    val bgBrush = if (isEnabled) Brush.linearGradient(listOf(PrimaryBlue, Color(0xFF0055CC)))
    else Brush.linearGradient(listOf(Surface, Surface))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bgBrush)
            .clickable {
                if (!isLoading) {
                    isLoading = true
                    if (isEnabled) { onDisable(); isLoading = false }
                    else { onEnable(); isLoading = false }
                }
            }
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = if (isEnabled) 0.18f else 0.07f)),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier  = Modifier.size(22.dp),
                        color     = Color.White,
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    Icon(
                        if (isEnabled) Icons.Default.Shield else Icons.Outlined.Shield,
                        contentDescription = null,
                        tint   = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (isEnabled) "Privacy Shield ON" else "Privacy Shield OFF",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp,
                    color      = Color.White,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    if (isEnabled) "Tap to disable" else "Tap to enable",
                    fontSize = 12.sp,
                    color    = Color.White.copy(alpha = 0.65f),
                )
            }
            Switch(
                checked    = isEnabled,
                onCheckedChange = { if (isEnabled) onDisable() else onEnable() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor   = PrimaryBlue,
                    checkedTrackColor   = Color.White.copy(alpha = 0.3f),
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = SurfaceVariant,
                ),
            )
        }
    }
}

// ─── Info Tiles ────────────────────────────────────────────────────────────

@Composable
private fun InfoTilesRow(settings: AppSettings) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        InfoTile(
            modifier  = Modifier.weight(1f),
            icon      = Icons.Outlined.RemoveRedEye,
            color     = PrimaryBlue,
            label     = "Sensitivity",
            value     = settings.sensitivity.name.lowercase().replaceFirstChar { it.uppercase() },
        )
        InfoTile(
            modifier  = Modifier.weight(1f),
            icon      = Icons.Outlined.Timer,
            color     = WarmOrange,
            label     = "Auto-Lock",
            value     = if (settings.autoLockSeconds == 0) "Off" else "${settings.autoLockSeconds}s",
        )
        InfoTile(
            modifier  = Modifier.weight(1f),
            icon      = Icons.Outlined.Layers,
            color     = SafeGreen,
            label     = "Overlay",
            value     = when (settings.overlayStyle) {
                OverlayStyle.BLUR         -> "Blur"
                OverlayStyle.DARK_CURTAIN -> "Dark"
                OverlayStyle.MOSAIC       -> "Mosaic"
            },
        )
    }
}

@Composable
private fun InfoTile(
    modifier: Modifier,
    icon: ImageVector,
    color: Color,
    label: String,
    value: String,
) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = Surface),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(10.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

// ─── How It Works ──────────────────────────────────────────────────────────

@Composable
private fun HowItWorksCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Surface),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("How It Works", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            Spacer(Modifier.height(14.dp))
            HowItWorksStep("1", "Front camera silently monitors for faces using on-device AI", PrimaryBlue)
            Spacer(Modifier.height(10.dp))
            HowItWorksStep("2", "When a 2nd face is detected, a privacy overlay appears instantly", AlertRed)
            Spacer(Modifier.height(10.dp))
            HowItWorksStep("3", "No video is stored or sent — all processing stays on your device", SafeGreen)
        }
    }
}

@Composable
private fun HowItWorksStep(number: String, text: String, color: Color) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier         = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, color = color, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 13.sp, color = TextSecondary, lineHeight = 19.sp, modifier = Modifier.weight(1f))
    }
}

// ─── Pulsing Dot ───────────────────────────────────────────────────────────

@Composable
private fun PulsingDot(color: Color) {
    val scale by rememberInfiniteTransition(label = "dot").animateFloat(
        initialValue = 0.6f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse),
        label = "dotScale",
    )
    Box(
        modifier = Modifier
            .size(10.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(CircleShape)
            .background(color),
    )
}

// ─── State Extension Helpers ───────────────────────────────────────────────

private fun PrivacyState.brandColor() = when (this) {
    PrivacyState.DISABLED     -> TextSecondary
    PrivacyState.NORMAL       -> SafeGreen
    PrivacyState.OWNER_AWAY   -> WarningYellow
    PrivacyState.PEEKER_ALERT -> AlertRed
    PrivacyState.LOCKED       -> AlertRed
}

private fun PrivacyState.shieldIcon() = when (this) {
    PrivacyState.DISABLED     -> Icons.Outlined.Shield
    PrivacyState.NORMAL       -> Icons.Default.Shield
    PrivacyState.OWNER_AWAY   -> Icons.Default.HourglassTop
    PrivacyState.PEEKER_ALERT -> Icons.Default.VisibilityOff
    PrivacyState.LOCKED       -> Icons.Default.Lock
}

private fun PrivacyState.statusIcon() = when (this) {
    PrivacyState.DISABLED     -> Icons.Outlined.Shield
    PrivacyState.NORMAL       -> Icons.Default.VerifiedUser
    PrivacyState.OWNER_AWAY   -> Icons.Default.HourglassTop
    PrivacyState.PEEKER_ALERT -> Icons.Default.Warning
    PrivacyState.LOCKED       -> Icons.Default.Lock
}

private fun PrivacyState.cardSubtitle() = when (this) {
    PrivacyState.DISABLED     -> "Tap the toggle to activate protection"
    PrivacyState.NORMAL       -> "AI monitoring active · On-device processing"
    PrivacyState.OWNER_AWAY   -> "Counting down to screen lock..."
    PrivacyState.PEEKER_ALERT -> "Unauthorized viewer detected — screen hidden"
    PrivacyState.LOCKED       -> "Authenticate to resume viewing"
}
