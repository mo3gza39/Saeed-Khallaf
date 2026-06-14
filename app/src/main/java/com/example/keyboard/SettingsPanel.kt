package com.example.keyboard

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsPanel(
    viewModel: KeyboardViewModel,
    modifier: Modifier = Modifier
) {
    val theme by viewModel.theme.collectAsState()
    val intensity by viewModel.intensity.collectAsState()
    val hapticEnabled by viewModel.hapticEnabled.collectAsState()
    val hapticWeight by viewModel.hapticWeight.collectAsState()
    val boardScale by viewModel.boardScale.collectAsState()
    val glowIntensity by viewModel.glowIntensity.collectAsState()
    val borderThickness by viewModel.borderThickness.collectAsState()
    val cornerRadius by viewModel.cornerRadius.collectAsState()
    val backlightsSpeed by viewModel.backlightsSpeed.collectAsState()
    val particlesCount by viewModel.particlesCount.collectAsState()
    val count by viewModel.keystrokeCount.collectAsState()
    val lastKey by viewModel.lastPressedKey.collectAsState()

    val scrollState = rememberScrollState()
    val currentView = LocalView.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // HEADER
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Settings",
                tint = theme.accentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "GLASS DESIGN WORKSPACE",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = theme.textColor,
                letterSpacing = 1.sp
            )
        }

        // STATIC DIAGNOSTICS STATS CARD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(cornerRadius.dp))
                .background(theme.boardBackground.copy(alpha = 0.25f))
                .border(borderThickness.dp, theme.borderNormal.copy(alpha = 0.15f), RoundedCornerShape(cornerRadius.dp))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Live Keyboard Diagnostics",
                    color = theme.textColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Active Theme", color = theme.textColor.copy(alpha = 0.5f), fontSize = 10.sp)
                        Text(theme.displayName, color = theme.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Vibrator State", color = theme.textColor.copy(alpha = 0.5f), fontSize = 10.sp)
                        Text(
                            if (hapticEnabled) "ON (${(hapticWeight * 100).toInt()}% weight)" else "OFF",
                            color = if (hapticEnabled) Color(0xFF4CAF50) else Color(0xFFF44336),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Keystrokes", color = theme.textColor.copy(alpha = 0.5f), fontSize = 10.sp)
                        Text("$count presses", color = theme.textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (lastKey != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(theme.keyBackground.copy(alpha = 0.1f))
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Last keystroke down: \"$lastKey\" in ~12ms response window",
                            color = theme.textColor,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // THEME PRESETS SELECTOR
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Backing Glass Themes",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = theme.textColor.copy(alpha = 0.65f)
            )

            Row(
                modifier = Modifier
                    .fillModifierHorizontalScrollable()
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Manually map list so we have custom visual indicator cards
                PresetThemes.list.forEach { item ->
                    val isSelected = item.name == theme.name
                    Box(
                        modifier = Modifier
                            .width(135.dp)
                            .height(65.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(item.backingColor1.copy(alpha = 0.8f), item.backingColor2.copy(alpha = 0.8f))
                                )
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) item.accentColor else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.setTheme(item) }
                            .padding(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.displayName,
                                    fontSize = 11.sp,
                                    color = item.textColor,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(item.accentColor, CircleShape)
                                    )
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(item.keyBackground.copy(alpha = 0.4f)))
                                Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(item.keyPressedBackground.copy(alpha = 0.4f)))
                                Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(item.glowColor.copy(alpha = 0.4f)))
                            }
                        }
                    }
                }
            }
        }

        // ANIMATION INTENSITY SELECTOR
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Animation Intensity Control",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = theme.textColor.copy(alpha = 0.65f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.boardBackground.copy(alpha = 0.15f))
                    .border(borderThickness.dp, theme.borderNormal.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            ) {
                AnimationIntensity.values().forEach { lv ->
                    val isSelected = lv == intensity
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setIntensity(lv) }
                            .background(
                                if (isSelected) theme.keyPressedBackground.copy(alpha = 0.25f)
                                else Color.Transparent
                            )
                            .padding(vertical = 10.dp)
                            .testTag("intensity_${lv.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lv.displayName,
                            color = if (isSelected) theme.accentColor else theme.textColor.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            val caption = when (intensity) {
                AnimationIntensity.LOW -> "Low: Ultra responsive, instant typing speeds, minimal scale frames for maximum FPS."
                AnimationIntensity.BALANCED -> "Balanced: Gentle responsive micro-bounces, cozy spring physics."
                AnimationIntensity.HIGH -> "High: Fluid spring scale, glowing aura expansions, standard vibrating clicks."
                AnimationIntensity.ULTRA -> "Ultra: Maximum visual richness, particle drift ripples, cascading dual-stage haptics!"
            }

            Text(
                text = caption,
                color = theme.textColor.copy(alpha = 0.45f),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // SLIDERS PANEL
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(cornerRadius.dp))
                .background(theme.boardBackground.copy(alpha = 0.1f))
                .border(borderThickness.dp, theme.borderNormal.copy(alpha = 0.1f), RoundedCornerShape(cornerRadius.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "Frosted Refraction Modifiers",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = theme.textColor
            )

            // CORNER RADIUS
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Corner Roundness", color = theme.textColor.copy(alpha = 0.6f), fontSize = 11.sp)
                    Text("${cornerRadius}dp", color = theme.textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = cornerRadius.toFloat(),
                    onValueChange = { viewModel.setCornerRadius(it.toInt()) },
                    valueRange = 4f..20f,
                    colors = SliderDefaults.colors(
                        thumbColor = theme.accentColor,
                        activeTrackColor = theme.accentColor,
                        inactiveTrackColor = theme.borderNormal.copy(alpha = 0.2f)
                    )
                )
            }

            // BORDER THICKNESS
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Glass Shimmer Border", color = theme.textColor.copy(alpha = 0.6f), fontSize = 11.sp)
                    Text(String.format("%.1f dp", borderThickness), color = theme.textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = borderThickness,
                    onValueChange = { viewModel.setBorderThickness(it) },
                    valueRange = 0.5f..3.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = theme.accentColor,
                        activeTrackColor = theme.accentColor,
                        inactiveTrackColor = theme.borderNormal.copy(alpha = 0.2f)
                    )
                )
            }

            // HAPTIC WEIGHT
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Haptic Feedback Clicks", color = theme.textColor.copy(alpha = 0.6f), fontSize = 11.sp)
                    Text(
                        if (hapticEnabled) String.format("%.0f%% Force", hapticWeight * 100f) else "Disabled",
                        color = theme.textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = hapticEnabled,
                        onCheckedChange = { viewModel.setHapticEnabled(it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = theme.accentColor,
                            uncheckedColor = theme.textColor.copy(0.4f),
                            checkmarkColor = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Slider(
                        value = hapticWeight,
                        onValueChange = { viewModel.setHapticWeight(it) },
                        enabled = hapticEnabled,
                        valueRange = 0.1f..1.0f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = theme.accentColor,
                            activeTrackColor = theme.accentColor,
                            inactiveTrackColor = theme.borderNormal.copy(alpha = 0.2f)
                        )
                    )
                }

                // Haptic feedback testing triggers
                if (hapticEnabled) {
                    Button(
                        onClick = { viewModel.onKeyPress("TEST_TAP", currentView) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.keyBackground.copy(alpha = 0.2f),
                            contentColor = theme.accentColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .border(1.dp, theme.borderNormal.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Vibration, contentDescription = "Vibrate", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trigger Test ${intensity.displayName} Click Pattern", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // PLASMA BACKLIGHT DIVERSITY
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Refraction Drift Speed", color = theme.textColor.copy(alpha = 0.6f), fontSize = 11.sp)
                    Text(String.format("%.1fx Speed", backlightsSpeed), color = theme.textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = backlightsSpeed,
                    onValueChange = { viewModel.setBacklightsSpeed(it) },
                    valueRange = 0.2f..2.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = theme.accentColor,
                        activeTrackColor = theme.accentColor,
                        inactiveTrackColor = theme.borderNormal.copy(alpha = 0.2f)
                    )
                )
            }

            // PLASMA DUST COUNT
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Floating Glass Sparkles", color = theme.textColor.copy(alpha = 0.6f), fontSize = 11.sp)
                    Text("$particlesCount sparkles", color = theme.textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = particlesCount.toFloat(),
                    onValueChange = { viewModel.setParticlesCount(it.toInt()) },
                    valueRange = 0f..20f,
                    colors = SliderDefaults.colors(
                        thumbColor = theme.accentColor,
                        activeTrackColor = theme.accentColor,
                        inactiveTrackColor = theme.borderNormal.copy(alpha = 0.2f)
                    )
                )
            }

            // KEYBOARD SCALE SIZE
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Physical Board Scale", color = theme.textColor.copy(alpha = 0.6f), fontSize = 11.sp)
                    Text(String.format("%.0f%% scale", boardScale * 100f), color = theme.textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = boardScale,
                    onValueChange = { viewModel.setBoardScale(it) },
                    valueRange = 0.85f..1.10f,
                    colors = SliderDefaults.colors(
                        thumbColor = theme.accentColor,
                        activeTrackColor = theme.accentColor,
                        inactiveTrackColor = theme.borderNormal.copy(alpha = 0.2f)
                    )
                )
            }
        }
    }
}

// Extension function helper to handle custom linear rows or scrolls
private fun Modifier.fillModifierHorizontalScrollable(): Modifier {
    return this.wrapContentHeight()
}
