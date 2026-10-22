package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.HunterProfile
import com.example.data.HunterRepository
import com.example.ui.components.SystemWindowCard
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    profile: HunterProfile,
    userEmail: String,
    onSetTheme: (String) -> Unit,
    onSetWallpaper: (String) -> Unit,
    onToggleVip: () -> Unit,
    onReplayOnboarding: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary

    var soundEffects by remember { mutableStateOf(true) }
    var hapticFeedback by remember { mutableStateOf(true) }
    var onlineStatus by remember { mutableStateOf(true) }
    var selectedLanguage by remember { mutableStateOf("English (Hunter Global)") }
    var showTutorialDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // Title
        item {
            Text(
                text = "SYSTEM CONFIGURATION & PROTOCOLS",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            )
        }

        // Hunter Account & Session Card
        item {
            SystemWindowCard(borderColor = accent.copy(alpha = 0.5f)) {
                Text(
                    text = "HUNTER CREDENTIALS & SESSION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = if (userEmail.isNotBlank()) userEmail else "hunter@arise.io",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    OutlinedButton(
                        onClick = { showLogoutConfirmDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CrimsonPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Sign Out",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SIGN OUT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Monarch Pass / VIP System Card
        item {
            SystemWindowCard(borderColor = if (profile.isVip) GoldPrimary else SystemCardBorder) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MONARCH PASS (VIP STATUS)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (profile.isVip) GoldGlow else Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "👑", fontSize = 14.sp)
                        }
                        Text(
                            text = "Unlocks exclusive aura skins, permanent stat buffs, and advanced weekly dungeon analytics.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = profile.isVip,
                        onCheckedChange = { onToggleVip() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GoldPrimary,
                            checkedTrackColor = GoldPrimary.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }

        // Accent Color Selection
        item {
            SystemWindowCard {
                Text(
                    text = "AURA ACCENT COLOR",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val colors = listOf(
                        Triple("CYAN", "Cyber Cyan", CyanPrimary),
                        Triple("VIOLET", "Monarch Violet", MonarchPurple),
                        Triple("CRIMSON", "Crimson Blood", CrimsonPrimary),
                        Triple("GOLD", "Sovereign Gold", GoldPrimary)
                    )

                    colors.forEach { (code, label, color) ->
                        val isSelected = profile.equippedTheme == code
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSetTheme(code) },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        BorderStroke(
                                            if (isSelected) 2.5.dp else 0.dp,
                                            Color.White
                                        ),
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = label.split(" ").last(),
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // App Background Wallpaper Selection
        item {
            SystemWindowCard {
                Text(
                    text = "SYSTEM WALLPAPER BACKDROP",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val walls = listOf(
                        "bg_tuff" to "Shadow Tuff",
                        "bg_violet" to "Violet Gate",
                        "obsidian" to "Deep Void"
                    )

                    walls.forEach { (bgKey, name) ->
                        val isSel = profile.wallpaper == bgKey
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSetWallpaper(bgKey) },
                            color = if (isSel) accent.copy(alpha = 0.2f) else SystemSurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSel) accent else SystemCardBorder)
                        ) {
                            Text(
                                text = name,
                                color = if (isSel) accent else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
        }

        // Audio & Haptics Toggles
        item {
            SystemWindowCard {
                Text(
                    text = "FEEDBACK & NOTIFICATIONS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("System Sound Fanfare (Level Up / Quests)", color = Color.White, fontSize = 13.sp)
                    Switch(checked = soundEffects, onCheckedChange = { soundEffects = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Haptic Awakening Pulses", color = Color.White, fontSize = 13.sp)
                    Switch(checked = hapticFeedback, onCheckedChange = { hapticFeedback = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Broadcast Online Status to Guild", color = Color.White, fontSize = 13.sp)
                        Text("When disabled, cloaks presence in stealth mode", color = TextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = profile.showOnlineStatus,
                        onCheckedChange = { HunterRepository.setShowOnlineStatus(it) }
                    )
                }
            }
        }

        // Language & Tutorial Replay
        item {
            SystemWindowCard {
                Text(
                    text = "LOCALIZATION & ONBOARDING",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "System Language", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = selectedLanguage, color = TextSecondary, fontSize = 11.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("EN", "KR", "JP").forEach { lang ->
                            Surface(
                                modifier = Modifier.clickable {
                                    selectedLanguage = when (lang) {
                                        "KR" -> "한국어 (Korean System)"
                                        "JP" -> "日本語 (Japanese System)"
                                        else -> "English (Hunter Global)"
                                    }
                                },
                                color = if (selectedLanguage.startsWith(lang)) accent.copy(alpha = 0.2f) else SystemSurfaceVariant,
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, if (selectedLanguage.startsWith(lang)) accent else SystemCardBorder)
                            ) {
                                Text(
                                    text = lang,
                                    color = if (selectedLanguage.startsWith(lang)) accent else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onReplayOnboarding,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
                ) {
                    Icon(imageVector = Icons.Default.Replay, contentDescription = "Replay")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "REPLAY FIRST-LAUNCH ONBOARDING", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }

    if (showLogoutConfirmDialog) {
        Dialog(onDismissRequest = { showLogoutConfirmDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SystemSurface),
                border = BorderStroke(1.dp, CrimsonPrimary)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "TERMINATE HUNTER SESSION?",
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You will be signed out from the ARISE Hunter Terminal. Your progression remains stored in the persistent database.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showLogoutConfirmDialog = false }) {
                            Text("CANCEL", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showLogoutConfirmDialog = false
                                onSignOut()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                        ) {
                            Text("SIGN OUT", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
