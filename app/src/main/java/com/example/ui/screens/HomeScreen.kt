package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.*
import java.util.Calendar

data class DashboardWidgetVisibility(
    val showProfileHud: Boolean = true,
    val showXpProgress: Boolean = true,
    val showProductivityGauge: Boolean = true,
    val showQuickCommands: Boolean = true,
    val showAiBriefing: Boolean = true,
    val showStreakReward: Boolean = true,
    val showActiveQuests: Boolean = true,
    val showAchievements: Boolean = true,
    val showRecentActivity: Boolean = true
)

@Composable
fun HomeScreen(
    profile: HunterProfile,
    missions: List<Mission>,
    activityLogs: List<ActivityLog>,
    achievements: List<AchievementBadge>,
    onCompleteMission: (String) -> Unit,
    onNavigateToMissions: () -> Unit,
    onNavigateToProductivity: () -> Unit,
    onNavigateToCommunity: () -> Unit,
    onNavigateToShop: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    onNavigateToAiAssistant: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    onOpenThemeEngine: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val dailyRewardClaimed by HunterRepository.dailyRewardClaimed.collectAsState()
    val quoteIndex by HunterRepository.currentQuoteIndex.collectAsState()
    val currentQuote = HunterRepository.quotes[quoteIndex]

    var widgetConfig by remember { mutableStateOf(DashboardWidgetVisibility()) }
    var showWidgetCustomizer by remember { mutableStateOf(false) }

    // Time-based greeting
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good Morning"
        hour < 18 -> "Good Afternoon"
        else -> "Good Evening"
    }

    // Daily completion calculations
    val dailyMissions = missions.filter { it.isDaily && !it.isSpecial }
    val completedDaily = dailyMissions.count { it.isCompleted }
    val totalDaily = dailyMissions.size.coerceAtLeast(1)
    val missionCompletionPercent = (completedDaily.toFloat() / totalDaily.toFloat()).coerceIn(0f, 1f)

    // Productivity Score (0-100) & Hunter Rank Grade
    val habitBonus = (profile.streakDays * 5).coerceAtMost(30)
    val missionPoints = (missionCompletionPercent * 50).toInt()
    val streakPoints = (profile.streakMultiplier * 8).toInt().coerceAtMost(20)
    val totalProductivityScore = (missionPoints + habitBonus + streakPoints).coerceIn(10, 100)
    val productivityGrade = when {
        totalProductivityScore >= 90 -> "S"
        totalProductivityScore >= 75 -> "A"
        totalProductivityScore >= 60 -> "B"
        totalProductivityScore >= 45 -> "C"
        totalProductivityScore >= 30 -> "D"
        else -> "E"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // Top Header with Logo, Greeting, Sovereign Pill & Notification Bell
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_arise_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ARISE",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = accent.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (profile.isPremium) "SOVEREIGN" else "SYSTEM",
                                    color = accent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "$greeting, ${profile.name}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Widget Customizer button
                    IconButton(onClick = { showWidgetCustomizer = true }) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DashboardCustomize,
                                contentDescription = "Customize Dashboard",
                                tint = accent,
                                modifier = Modifier.padding(8.dp).size(20.dp)
                            )
                        }
                    }

                    // Notification bell
                    IconButton(onClick = onOpenNotifications) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(8.dp).size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Widget 1: Hunter Profile Bar with Avatar & Level
        if (widgetConfig.showProfileHud) {
            item {
                HunterHeader(
                    profile = profile,
                    onAvatarClick = onOpenProfile
                )
            }
        }

        // Widget 2: Live XP & Level Progress HUD
        if (widgetConfig.showXpProgress) {
            item {
                LevelProgressBar(profile = profile)
            }
        }

        // Widget 3: Productivity Mastery Gauge (0-100 & Rank Grade)
        if (widgetConfig.showProductivityGauge) {
            item {
                SystemWindowCard(borderColor = accent.copy(alpha = 0.5f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PRODUCTIVITY MASTERY",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = accent,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Grade $productivityGrade Hunter Performance • $completedDaily/$totalDaily Quests Cleared",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }

                        // Score Pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = accent.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, accent)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$totalProductivityScore",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = accent,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "/100",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dual progress bars
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(totalProductivityScore / 100f)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(accent.copy(alpha = 0.7f), accent)
                                    )
                                )
                        )
                    }
                }
            }
        }

        // Widget 4: Quick Action Command Bar
        if (widgetConfig.showQuickCommands) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quests
                    Surface(
                        modifier = Modifier.weight(1f).clickable { onNavigateToMissions() },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.FitnessCenter, contentDescription = "Quests", tint = accent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Quests", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Focus
                    Surface(
                        modifier = Modifier.weight(1f).clickable { onNavigateToProductivity() },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, ManaBlue.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.Timer, contentDescription = "Focus", tint = ManaBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Focus", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // AI Assistant
                    Surface(
                        modifier = Modifier.weight(1f).clickable { onNavigateToAiAssistant() },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MonarchPurple.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.SmartToy, contentDescription = "AI", tint = MonarchPurple, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "AI Advisor", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Theme Workshop
                    Surface(
                        modifier = Modifier.weight(1f).clickable { onOpenThemeEngine() },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.Palette, contentDescription = "Themes", tint = GoldPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Themes", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Widget 5: AI Architect Tactical Briefing
        if (widgetConfig.showAiBriefing) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MonarchPurple.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MonarchPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ARCHITECT SYSTEM BRIEFING",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = MonarchPurple,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                            IconButton(onClick = onNavigateToAiAssistant, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "Open AI", tint = MonarchPurple, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "\"Hunter ${profile.name}, your ${profile.streakDays}-day streak demonstrates elite stamina. Clear 2 more conditioning missions today to unlock Level ${profile.level + 1} stat upgrades.\"",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onNavigateToAiAssistant,
                            modifier = Modifier.fillMaxWidth().height(34.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MonarchPurple),
                            border = BorderStroke(1.dp, MonarchPurple.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Consult System Architect AI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Widget 6: Streak Counter & Daily Claim
        if (widgetConfig.showStreakReward) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Streak Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, WarningYellow.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🔥", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "STREAK",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = WarningYellow,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${profile.streakDays} Days",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Best: ${profile.bestStreak}d • ${profile.streakMultiplier}x XP",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Daily Reward Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, if (!dailyRewardClaimed) accent else MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🎁", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DAILY REWARD",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (!dailyRewardClaimed) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (!dailyRewardClaimed) {
                                Button(
                                    onClick = { HunterRepository.claimDailyReward() },
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = "CLAIM +300G",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.Black
                                    )
                                }
                            } else {
                                Text(
                                    text = "Claimed ✓",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = SuccessGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Next at 00:00",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Widget 7: Active Daily Quests Preview
        if (widgetConfig.showActiveQuests) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE DAILY QUESTS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.5.sp
                        )
                    )
                    Text(
                        text = "View All →",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = accent,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.clickable { onNavigateToMissions() }
                    )
                }
            }

            val topMissions = missions.take(3)
            items(topMissions, key = { it.id }) { mission ->
                MissionCard(
                    mission = mission,
                    onComplete = { onCompleteMission(mission.id) }
                )
            }
        }

        // Widget 8: Achievement Highlights
        if (widgetConfig.showAchievements) {
            item {
                Text(
                    text = "ACHIEVEMENT HIGHLIGHTS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(achievements.take(4), key = { it.id }) { ach ->
                        Surface(
                            modifier = Modifier.width(160.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (ach.isUnlocked) GoldPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = if (ach.isUnlocked) "🏆" else "🔒", fontSize = 20.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = ach.title,
                                    color = if (ach.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "+${ach.xpBonus} XP",
                                    color = GoldGlow,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Widget 9: Recent System Activity
        if (widgetConfig.showRecentActivity) {
            item {
                Text(
                    text = "RECENT SYSTEM ACTIVITY",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.5.sp
                    )
                )
            }

            items(activityLogs.take(4), key = { it.id }) { log ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = log.title,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = log.detail,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = log.timeAgo,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }

    // Dashboard Widget Customizer Dialog
    if (showWidgetCustomizer) {
        Dialog(onDismissRequest = { showWidgetCustomizer = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, accent)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CUSTOMIZE DASHBOARD",
                            fontWeight = FontWeight.Black,
                            color = accent,
                            fontSize = 14.sp
                        )
                        IconButton(onClick = { showWidgetCustomizer = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val toggles = listOf(
                        Pair("Hunter Profile HUD", widgetConfig.showProfileHud) to { b: Boolean -> widgetConfig = widgetConfig.copy(showProfileHud = b) },
                        Pair("XP & Level Progress", widgetConfig.showXpProgress) to { b: Boolean -> widgetConfig = widgetConfig.copy(showXpProgress = b) },
                        Pair("Productivity Mastery Gauge", widgetConfig.showProductivityGauge) to { b: Boolean -> widgetConfig = widgetConfig.copy(showProductivityGauge = b) },
                        Pair("Quick Command Bar", widgetConfig.showQuickCommands) to { b: Boolean -> widgetConfig = widgetConfig.copy(showQuickCommands = b) },
                        Pair("Architect AI Briefing", widgetConfig.showAiBriefing) to { b: Boolean -> widgetConfig = widgetConfig.copy(showAiBriefing = b) },
                        Pair("Streak & Daily Claim", widgetConfig.showStreakReward) to { b: Boolean -> widgetConfig = widgetConfig.copy(showStreakReward = b) },
                        Pair("Active Daily Quests", widgetConfig.showActiveQuests) to { b: Boolean -> widgetConfig = widgetConfig.copy(showActiveQuests = b) },
                        Pair("Achievement Highlights", widgetConfig.showAchievements) to { b: Boolean -> widgetConfig = widgetConfig.copy(showAchievements = b) },
                        Pair("Recent Activity Feed", widgetConfig.showRecentActivity) to { b: Boolean -> widgetConfig = widgetConfig.copy(showRecentActivity = b) }
                    )

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(toggles) { (info, onToggle) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = info.first,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Switch(
                                    checked = info.second,
                                    onCheckedChange = onToggle,
                                    colors = SwitchDefaults.colors(checkedThumbColor = accent)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showWidgetCustomizer = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        Text("Save Dashboard Layout", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
