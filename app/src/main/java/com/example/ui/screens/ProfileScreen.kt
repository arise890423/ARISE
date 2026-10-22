package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.ui.components.RankBadge
import com.example.ui.components.StatRow
import com.example.ui.components.SystemWindowCard
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
    profile: HunterProfile,
    skills: List<SkillNode>,
    achievements: List<AchievementBadge>,
    onAllocateStat: (String) -> Unit,
    onUpgradeSkill: (String) -> Unit,
    onPrestige: () -> Unit,
    onSaveProfile: (String, String, String, String) -> Unit,
    onSelectAvatar: (String) -> Unit,
    onTogglePrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    var profileTab by remember { mutableStateOf(0) } // 0: Stats & Attributes, 1: Skill Tree, 2: Achievements, 3: Prestige
    var showEditDialog by remember { mutableStateOf(false) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var showPublicPreview by remember { mutableStateOf(false) }
    var showPhotoWorkshop by remember { mutableStateOf(false) }
    var showCoverWorkshop by remember { mutableStateOf(false) }

    val avatarDrawableRes = when (profile.avatarResName) {
        "rise" -> R.drawable.rise
        "avatar_hunter" -> R.drawable.avatar_hunter
        else -> R.drawable.arise
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // Hero Profile Showcase Card with Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SystemSurface),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
            ) {
                Column {
                    // Banner Image (Clickable for Cover Workshop)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clickable { showCoverWorkshop = true }
                    ) {
                        if (profile.customCoverUri != null) {
                            coil.compose.AsyncImage(
                                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                    .data(profile.customCoverUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile Banner",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.app_hero),
                                contentDescription = "Profile Banner",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, SystemSurface)
                                    )
                                )
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Cover Workshop",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cover", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Avatar + Profile Details
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val frameColor = when (profile.equippedFrame) {
                                "frame_monarch" -> MonarchPurple
                                "frame_crimson" -> CrimsonPrimary
                                "frame_sovereign" -> GoldPrimary
                                "frame_cyan" -> CyanPrimary
                                "none" -> Color.Transparent
                                else -> accent
                            }
                            // Avatar with Click to Change Workshop
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .offset(y = (-24).dp)
                                    .border(BorderStroke(3.dp, frameColor), CircleShape)
                                    .clickable { showPhotoWorkshop = true }
                            ) {
                                if (profile.customAvatarUri != null) {
                                    coil.compose.AsyncImage(
                                        model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                            .data(profile.customAvatarUri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Avatar",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = avatarDrawableRes),
                                        contentDescription = "Avatar",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(accent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Change Avatar",
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { showPublicPreview = !showPublicPreview },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, SystemCardBorder),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (showPublicPreview) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (showPublicPreview) "MINE" else "PUBLIC", color = TextSecondary, fontSize = 10.sp)
                                }

                                OutlinedButton(
                                    onClick = { showEditDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, accent),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Profile",
                                        tint = accent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("CUSTOMIZE", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "${profile.username} • ${profile.title}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = accent,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = profile.bio,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFCBD5E1),
                                lineHeight = 18.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                RankBadge(rank = profile.rank)
                                Surface(
                                    color = SystemSurfaceVariant,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "LVL ${profile.level}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onTogglePrivacy() }
                            ) {
                                Icon(
                                    imageVector = if (profile.isProfilePublic) Icons.Default.Public else Icons.Default.Lock,
                                    contentDescription = "Privacy",
                                    tint = if (profile.isProfilePublic) SuccessGreen else WarningYellow,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (profile.isProfilePublic) "Public Registry" else "Shadow Private",
                                    color = if (profile.isProfilePublic) SuccessGreen else WarningYellow,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Completed Mission Statistics Grid
        item {
            SystemWindowCard(borderColor = accent.copy(alpha = 0.3f)) {
                Text(
                    text = "HUNTER COMBAT & PROTOCOL METRICS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = accent,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(SystemSurfaceVariant, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "CLEARS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${profile.totalMissionsCompleted}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text(text = "Quests Done", color = TextMuted, fontSize = 9.sp)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(SystemSurfaceVariant, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "STREAK", color = WarningYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${profile.streakDays}d", color = WarningYellow, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text(text = "Best: ${profile.bestStreak}d", color = TextMuted, fontSize = 9.sp)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(SystemSurfaceVariant, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "MANA CORE", color = ManaBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${profile.crystals}", color = ManaBlue, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text(text = "Crystals", color = TextMuted, fontSize = 9.sp)
                    }
                }
            }
        }

        // Profile Sub-Tabs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Stats", "Skill Tree", "Badges", "Awaken").forEachIndexed { i, title ->
                    val sel = profileTab == i
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { profileTab = i },
                        color = if (sel) accent.copy(alpha = 0.2f) else SystemSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (sel) accent else SystemCardBorder)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (sel) FontWeight.Black else FontWeight.Medium,
                                color = if (sel) accent else TextSecondary
                            ),
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }

        when (profileTab) {
            0 -> {
                // Stats & Points Allocation
                item {
                    SystemWindowCard(borderColor = accent.copy(alpha = 0.4f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "HUNTER CORE ATTRIBUTES",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            )
                            if (profile.statPointsAvailable > 0) {
                                Surface(
                                    color = WarningYellow.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, WarningYellow)
                                ) {
                                    Text(
                                        text = "+${profile.statPointsAvailable} UNALLOCATED",
                                        color = WarningYellow,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val canAdd = profile.statPointsAvailable > 0
                        StatRow("STRENGTH", profile.strength, StatStrength, canAdd, { onAllocateStat("STR") })
                        StatRow("AGILITY", profile.agility, StatAgility, canAdd, { onAllocateStat("AGI") })
                        StatRow("INTELLECT", profile.intelligence, StatIntelligence, canAdd, { onAllocateStat("INT") })
                        StatRow("VITALITY", profile.vitality, StatVitality, canAdd, { onAllocateStat("VIT") })
                        StatRow("PERCEPTION", profile.perception, StatPerception, canAdd, { onAllocateStat("PER") })
                    }
                }
            }

            1 -> {
                // Skill Tree Progression
                item {
                    Text(
                        text = "HUNTER SKILL TREE (AVAILABLE: ${profile.statPointsAvailable} PTS)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                    )
                }

                items(skills, key = { it.id }) { skill ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SystemSurface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SystemCardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = skill.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = when (skill.branch) {
                                            "SHADOW" -> MonarchDark
                                            "INTELLECT" -> ManaBlue.copy(alpha = 0.2f)
                                            "STRENGTH" -> StatStrength.copy(alpha = 0.2f)
                                            else -> StatAgility.copy(alpha = 0.2f)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = skill.branch,
                                            color = when (skill.branch) {
                                                "SHADOW" -> MonarchGlow
                                                "INTELLECT" -> ManaBlue
                                                "STRENGTH" -> StatStrength
                                                else -> StatAgility
                                            },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = skill.description,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tier ${skill.level} / ${skill.maxLevel}",
                                    color = accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (skill.level < skill.maxLevel) {
                                Button(
                                    onClick = { onUpgradeSkill(skill.id) },
                                    enabled = profile.statPointsAvailable >= skill.costPoints,
                                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("UPGRADE", fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            } else {
                                Surface(
                                    color = SuccessGreen.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "MAX",
                                        color = SuccessGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Achievement Badges
                item {
                    Text(
                        text = "UNLOCKED SYSTEM ACHIEVEMENTS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                    )
                }

                items(achievements, key = { it.id }) { ach ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (ach.isUnlocked) SystemSurface else SystemSurface.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            if (ach.isUnlocked) GoldPrimary.copy(alpha = 0.6f) else SystemCardBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (ach.isUnlocked) "🏆" else "🔒",
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ach.title,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ach.isUnlocked) GoldGlow else TextMuted
                                )
                                Text(
                                    text = ach.description,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Surface(
                                color = if (ach.isUnlocked) GoldPrimary.copy(alpha = 0.2f) else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "+${ach.xpBonus} XP",
                                    color = if (ach.isUnlocked) GoldGlow else TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            3 -> {
                // Prestige Rebirth / Sovereign Awakening
                item {
                    SystemWindowCard(borderColor = MonarchPurple.copy(alpha = 0.6f)) {
                        Text(
                            text = "SOVEREIGN AWAKENING (PRESTIGE)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = MonarchGlow
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Hunters who reach Level 20+ can sacrifice their accumulated level to undergo Sovereign Rebirth. This permanently unlocks a +10% XP multiplier, 10 bonus Stat points, and 50 Mana Crystals.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Current Prestige Level: ${profile.prestigeLevel}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onPrestige,
                            enabled = profile.level >= 20,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MonarchPurple,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (profile.level >= 20) "COMMENCE SOVEREIGN AWAKENING" else "LOCKED (REQ. LEVEL 20)",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAvatarPicker) {
        AvatarSelectionDialog(
            currentAvatar = profile.avatarResName,
            onDismiss = { showAvatarPicker = false },
            onSelect = {
                onSelectAvatar(it)
                showAvatarPicker = false
            }
        )
    }

    if (showEditDialog) {
        EditProfileDialog(
            profile = profile,
            onDismiss = { showEditDialog = false },
            onSave = { name, username, title, bio ->
                onSaveProfile(name, username, title, bio)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun AvatarSelectionDialog(
    currentAvatar: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val avatars = listOf(
        Triple("arise", "Shadow Monarch", R.drawable.arise),
        Triple("rise", "Awakened Hero", R.drawable.rise),
        Triple("avatar_hunter", "Tactical Hunter", R.drawable.avatar_hunter)
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SystemSurface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "SELECT HUNTER AVATAR",
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    avatars.forEach { (key, name, resId) ->
                        val isSel = currentAvatar == key
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onSelect(key) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .border(
                                        BorderStroke(
                                            if (isSel) 3.dp else 1.dp,
                                            if (isSel) MaterialTheme.colorScheme.primary else SystemCardBorder
                                        ),
                                        CircleShape
                                    )
                            ) {
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = name.split(" ").first(),
                                color = if (isSel) MaterialTheme.colorScheme.primary else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SystemSurfaceVariant)
                ) {
                    Text("CLOSE", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun EditProfileDialog(
    profile: HunterProfile,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var username by remember { mutableStateOf(profile.username) }
    var title by remember { mutableStateOf(profile.title) }
    var bio by remember { mutableStateOf(profile.bio) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SystemSurface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "EDIT HUNTER IDENTIFIER",
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Hunter Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Registry Handle (@handle)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Hunter Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Hunter Bio & Creed") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("CANCEL") }
                    Button(
                        onClick = { onSave(name, username, title, bio) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("SAVE CHANGES", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
