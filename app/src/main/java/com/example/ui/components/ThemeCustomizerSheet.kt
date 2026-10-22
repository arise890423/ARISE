package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun ThemeCustomizerDialog(
    onDismiss: () -> Unit
) {
    val config by ThemeManager.themeConfig.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0: Palettes (30), 1: Appearance & Styles, 2: Hybrid Customizer

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SYSTEM THEME ENGINE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "Global visual styling & coordinated palettes",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("30 Palettes", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Styles", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Hybrid", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Content
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> PalettesTabContent(
                            currentPaletteId = config.selectedPaletteId,
                            onSelectPalette = { ThemeManager.setPalette(it) }
                        )
                        1 -> StylesTabContent(
                            currentConfig = config,
                            onSelectMode = { ThemeManager.setAppearanceMode(it) },
                            onSelectStyle = { ThemeManager.setVisualStyle(it) },
                            onSelectDensity = { ThemeManager.setLayoutDensity(it) },
                            onToggleReducedMotion = { ThemeManager.setReducedMotion(it) }
                        )
                        2 -> HybridCustomizerContent(
                            currentConfig = config,
                            onApplyHybrid = { p, s, a, bg, surf ->
                                ThemeManager.setHybridColors(p, s, a, bg, surf)
                            },
                            onReset = { ThemeManager.resetToDefaults() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { ThemeManager.resetToDefaults() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Default", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Apply & Close", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PalettesTabContent(
    currentPaletteId: String,
    onSelectPalette: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(ArisePalettes.allPalettes) { palette ->
            val isSelected = palette.id.equals(currentPaletteId, ignoreCase = true)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectPalette(palette.id) },
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) palette.primary.copy(alpha = 0.15f) else palette.surface
                ),
                border = BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) palette.primary else palette.cardBorder
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = palette.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (palette.isLight) Color(0xFF0F172A) else Color(0xFFF1F5F9)
                                )
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ACTIVE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = palette.primary,
                                    modifier = Modifier
                                        .background(palette.primary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (palette.isLight) "Light Canvas Theme" else "Dark Cyber Theme",
                            fontSize = 11.sp,
                            color = if (palette.isLight) Color(0xFF475569) else Color(0xFF94A3B8)
                        )
                    }

                    // Palette color pills preview
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(palette.primary))
                        Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(palette.secondary))
                        Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(palette.accent))
                        Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(palette.background).border(1.dp, Color.Gray, CircleShape))
                    }
                }
            }
        }
    }
}

@Composable
private fun StylesTabContent(
    currentConfig: ThemeConfiguration,
    onSelectMode: (AppearanceMode) -> Unit,
    onSelectStyle: (VisualStyle) -> Unit,
    onSelectDensity: (LayoutDensity) -> Unit,
    onToggleReducedMotion: (Boolean) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Appearance Mode
        item {
            Text(
                text = "APPEARANCE MODE",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AppearanceMode.entries.forEach { mode ->
                    val isSel = currentConfig.appearanceMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onSelectMode(mode) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        )
                        RadioButton(
                            selected = isSel,
                            onClick = { onSelectMode(mode) },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }

        // Visual Style
        item {
            Text(
                text = "VISUAL STYLE PRESETS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                VisualStyle.entries.forEach { style ->
                    val isSel = currentConfig.visualStyle == style
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onSelectStyle(style) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = style.label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Corner Radius: ${style.cornerRadius.value.toInt()}dp • Glow: ${if (style.glowEffect) "Enabled" else "Subtle"}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RadioButton(
                            selected = isSel,
                            onClick = { onSelectStyle(style) },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }

        // Layout Density
        item {
            Text(
                text = "LAYOUT DENSITY",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LayoutDensity.entries.forEach { density ->
                    val isSel = currentConfig.layoutDensity == density
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectDensity(density) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                            contentColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Text(density.label, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }

        // Reduced Motion Toggle
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Reduced Motion",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    )
                    Text(
                        text = "Minimize system animations for comfort & performance",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                Switch(
                    checked = currentConfig.reducedMotion,
                    onCheckedChange = { onToggleReducedMotion(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun HybridCustomizerContent(
    currentConfig: ThemeConfiguration,
    onApplyHybrid: (primary: String?, secondary: String?, accent: String?, bg: String?, surf: String?) -> Unit,
    onReset: () -> Unit
) {
    var primaryHex by remember { mutableStateOf(currentConfig.customPrimaryHex ?: "#00E5FF") }
    var secondaryHex by remember { mutableStateOf(currentConfig.customSecondaryHex ?: "#A855F7") }
    var accentHex by remember { mutableStateOf(currentConfig.customAccentHex ?: "#F59E0B") }
    var bgHex by remember { mutableStateOf(currentConfig.customBackgroundHex ?: "#070A12") }
    var surfHex by remember { mutableStateOf(currentConfig.customSurfaceHex ?: "#0D1322") }

    val presetSwatches = listOf(
        "#00E5FF", "#A855F7", "#EF4444", "#F59E0B", "#10B981",
        "#38BDF8", "#EC4899", "#8B5CF6", "#14B8A6", "#F97316",
        "#000000", "#070A12", "#0D1322", "#18181B", "#FFFFFF"
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "HYBRID COLOR ROLE BUILDER",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = "Override specific UI roles with real HEX colors and live preview",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }

        // Live Preview Box
        item {
            val previewPrimary = ThemeManager.parseHexColor(primaryHex) ?: MaterialTheme.colorScheme.primary
            val previewBg = ThemeManager.parseHexColor(bgHex) ?: MaterialTheme.colorScheme.background
            val previewSurf = ThemeManager.parseHexColor(surfHex) ?: MaterialTheme.colorScheme.surface
            val previewAccent = ThemeManager.parseHexColor(accentHex) ?: MaterialTheme.colorScheme.tertiary

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = previewSurf),
                border = BorderStroke(1.dp, previewPrimary)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "LIVE THEME PREVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, color = previewPrimary)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "System Awakening Active • Rank S",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(containerColor = previewPrimary),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Primary Action", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {},
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = previewAccent),
                            border = BorderStroke(1.dp, previewAccent),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Accent Pill", fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Swatch Quick Picker for Primary
        item {
            ColorRoleField(
                label = "Primary Color",
                currentHex = primaryHex,
                swatches = presetSwatches,
                onValueChange = { primaryHex = it }
            )
        }

        item {
            ColorRoleField(
                label = "Secondary Color",
                currentHex = secondaryHex,
                swatches = presetSwatches,
                onValueChange = { secondaryHex = it }
            )
        }

        item {
            ColorRoleField(
                label = "Accent Highlight",
                currentHex = accentHex,
                swatches = presetSwatches,
                onValueChange = { accentHex = it }
            )
        }

        item {
            ColorRoleField(
                label = "Background Canvas",
                currentHex = bgHex,
                swatches = presetSwatches,
                onValueChange = { bgHex = it }
            )
        }

        item {
            ColorRoleField(
                label = "Card Surface",
                currentHex = surfHex,
                swatches = presetSwatches,
                onValueChange = { surfHex = it }
            )
        }

        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onApplyHybrid(primaryHex, secondaryHex, accentHex, bgHex, surfHex)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Hybrid Theme", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ColorRoleField(
    label: String,
    currentHex: String,
    swatches: List<String>,
    onValueChange: (String) -> Unit
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val color = ThemeManager.parseHexColor(currentHex) ?: Color.Gray
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
            )

            OutlinedTextField(
                value = currentHex,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                placeholder = { Text("#RRGGBB", fontSize = 11.sp) }
            )
        }

        // Swatches row
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(swatches) { hex ->
                val c = ThemeManager.parseHexColor(hex) ?: Color.Transparent
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(c)
                        .border(1.dp, if (hex.equals(currentHex, ignoreCase = true)) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f), CircleShape)
                        .clickable { onValueChange(hex) }
                )
            }
        }
    }
}
