package com.privacyshield.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun SettingsScreen(
    viewModel: PrivacyViewModel,
    onBack: () -> Unit,
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor         = Background,
                    titleContentColor      = Color.White,
                    navigationIconContentColor = TextSecondary,
                ),
            )
        },
        containerColor = Background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {

            // ── Detection ──────────────────────────────────────────────
            SectionHeader("Detection")
            Spacer(Modifier.height(10.dp))
            SettingsCard {
                SensitivityRow(settings.sensitivity) { viewModel.setSensitivity(it) }
            }

            Spacer(Modifier.height(24.dp))

            // ── Overlay Style ──────────────────────────────────────────
            SectionHeader("Privacy Overlay Style")
            Spacer(Modifier.height(10.dp))
            OverlayStylePicker(settings.overlayStyle) { viewModel.setOverlayStyle(it) }

            Spacer(Modifier.height(24.dp))

            // ── Auto-Lock ──────────────────────────────────────────────
            SectionHeader("Auto-Lock")
            Spacer(Modifier.height(10.dp))
            SettingsCard {
                AutoLockRow(settings.autoLockSeconds) { viewModel.setAutoLockSeconds(it) }
            }

            Spacer(Modifier.height(24.dp))

            // ── Security ───────────────────────────────────────────────
            SectionHeader("Security")
            Spacer(Modifier.height(10.dp))
            SettingsCard {
                ToggleRow(
                    icon       = Icons.Default.Fingerprint,
                    iconColor  = SafeGreen,
                    title      = "Require Biometric to Dismiss",
                    subtitle   = "Fingerprint or PIN required to hide the overlay",
                    checked    = settings.requireBiometric,
                    onToggle   = { viewModel.setRequireBiometric(it) },
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── About ──────────────────────────────────────────────────
            AboutCard()

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─── Section Header ────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(text: String) {
    Text(
        text          = text.uppercase(),
        color         = TextSecondary,
        fontSize      = 12.sp,
        fontWeight    = FontWeight.SemiBold,
        letterSpacing = 1.sp,
    )
}

// ─── Settings Card ─────────────────────────────────────────────────────────

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = Surface),
        content  = content,
    )
}

// ─── Sensitivity Picker ────────────────────────────────────────────────────

@Composable
private fun SensitivityRow(
    current: DetectionSensitivity,
    onChange: (DetectionSensitivity) -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBox(Icons.Outlined.Tune, WarmOrange)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Detection Sensitivity", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color.White)
                Text("How quickly the alert triggers", fontSize = 12.sp, color = TextSecondary)
            }
            Text(
                current.name.lowercase().replaceFirstChar { it.uppercase() },
                color = WarmOrange, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DetectionSensitivity.entries.forEach { s ->
                val selected = s == current
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) WarmOrange else SurfaceVariant)
                        .clickable { onChange(s) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        s.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (selected) Color.Black else TextSecondary,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "High: instant (1 frame) · Medium: 3 frames · Low: 5 frames",
            fontSize = 11.sp,
            color    = TextSecondary.copy(alpha = 0.7f),
        )
    }
}

// ─── Overlay Style Picker ──────────────────────────────────────────────────

@Composable
private fun OverlayStylePicker(
    current: OverlayStyle,
    onChange: (OverlayStyle) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        data class StyleOption(val style: OverlayStyle, val icon: ImageVector, val label: String)
        val options = listOf(
            StyleOption(OverlayStyle.BLUR,         Icons.Default.BlurOn,    "Blur"),
            StyleOption(OverlayStyle.DARK_CURTAIN, Icons.Default.DarkMode,  "Dark"),
            StyleOption(OverlayStyle.MOSAIC,       Icons.Default.GridView,  "Mosaic"),
        )
        options.forEach { opt ->
            val selected = opt.style == current
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onChange(opt.style) },
                shape  = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) PrimaryBlue.copy(alpha = 0.12f) else Surface,
                ),
                border = if (selected) BorderStroke(1.5.dp, PrimaryBlue) else null,
            ) {
                Column(
                    modifier            = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        opt.icon,
                        contentDescription = opt.label,
                        tint     = if (selected) PrimaryBlue else TextSecondary,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        opt.label,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (selected) PrimaryBlue else TextSecondary,
                    )
                }
            }
        }
    }
}

// ─── Auto-Lock Row ─────────────────────────────────────────────────────────

@Composable
private fun AutoLockRow(current: Int, onChange: (Int) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBox(Icons.Outlined.Timer, SafeGreen)
            Spacer(Modifier.width(12.dp))
            Text("Lock when owner steps away", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color.White)
        }
        Spacer(Modifier.height(14.dp))
        val options = listOf(0, 10, 30, 60)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { sec ->
                val selected = sec == current
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) SafeGreen else SurfaceVariant)
                        .clickable { onChange(sec) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (sec == 0) "Off" else "${sec}s",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (selected) Color.Black else TextSecondary,
                    )
                }
            }
        }
    }
}

// ─── Toggle Row ────────────────────────────────────────────────────────────

@Composable
private fun ToggleRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier          = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBox(icon, iconColor)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color.White)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = TextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

// ─── Icon Box ──────────────────────────────────────────────────────────────

@Composable
private fun IconBox(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
    }
}

// ─── About Card ────────────────────────────────────────────────────────────

@Composable
private fun AboutCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Surface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryBlue),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Privacy Shield", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    Text("Version 1.0.0 · Native Kotlin + Compose", fontSize = 12.sp, color = TextSecondary)
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Separator, thickness = 0.5.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                "All face detection runs entirely on-device using Google ML Kit.\n" +
                "No camera data is ever uploaded, stored, or shared with anyone.",
                fontSize  = 12.sp,
                color     = TextSecondary,
                lineHeight = 18.sp,
                textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
                modifier  = Modifier.fillMaxWidth(),
            )
        }
    }
}
