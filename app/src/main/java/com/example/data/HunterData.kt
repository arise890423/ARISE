package com.example.data

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class HunterRank(
    val code: String,
    val title: String,
    val minLevel: Int,
    val color: Color
) {
    E("E-Rank", "The Weakest Hunter", 1, RankEColor),
    D("D-Rank", "Awakened Striker", 10, RankDColor),
    C("C-Rank", "Dungeon Vanguard", 20, RankCColor),
    B("B-Rank", "Elite Slayer", 30, RankBColor),
    A("A-Rank", "Raid Commander", 40, RankAColor),
    S("S-Rank", "National Power Candidate", 50, RankSColor),
    NATIONAL("National-Level", "Ruler's Vessel", 70, RankNationalColor),
    MONARCH("Shadow Monarch", "Sovereign of Shadows", 100, RankMonarchColor);

    companion object {
        fun fromLevel(level: Int): HunterRank {
            return entries.reversed().firstOrNull { level >= it.minLevel } ?: E
        }
    }
}

enum class MissionCategory(val label: String, val iconName: String) {
    FITNESS("Physical Quest", "fitness_center"),
    FOCUS("Mental Sanctuary", "psychology"),
    HABIT("Daily Protocol", "repeat"),
    DUNGEON("Dungeon Raid", "shield"),
    PERSONAL("Custom Quest", "star")
}

data class Mission(
    val id: String,
    val title: String,
    val description: String,
    val category: MissionCategory,
    val difficulty: String,
    val xpReward: Int,
    val goldReward: Int,
    val crystalReward: Int = 0,
    val isCompleted: Boolean = false,
    val isDaily: Boolean = true,
    val isWeekly: Boolean = false,
    val isSpecial: Boolean = false,
    val isPersonal: Boolean = false,
    val progress: Int = 0,
    val target: Int = 1,
    val unit: String = ""
)

data class ProductivityHabit(
    val id: String,
    val title: String,
    val streak: Int,
    val isDoneToday: Boolean = false,
    val targetDaysAWeek: Int = 7
)

data class TodoTask(
    val id: String,
    val title: String,
    val priority: String, // High, Medium, Low
    val isCompleted: Boolean = false,
    val deadline: String = "Today"
)

data class FocusRecord(
    val id: String,
    val title: String,
    val minutes: Int,
    val date: String,
    val xpGained: Int
)

enum class ShopCategory(val title: String) {
    THEMES("Themes"),
    FRAMES("Profile Frames"),
    AVATARS("Avatars"),
    TITLES("Hunter Titles"),
    EQUIPMENT("Artifacts & Gear"),
    POTIONS("Elixirs & Potions")
}

data class ShopItem(
    val id: String,
    val name: String,
    val category: ShopCategory,
    val description: String,
    val lore: String = "",
    val costGold: Int,
    val costCrystals: Int = 0,
    val iconRes: Int? = null,
    val isPurchased: Boolean = false,
    val isEquipped: Boolean = false,
    val requiredRank: HunterRank = HunterRank.E,
    val statBonus: String = "",
    val isConsumable: Boolean = false
)

data class PurchaseRecord(
    val id: String,
    val itemId: String,
    val itemName: String,
    val category: ShopCategory,
    val costGold: Int,
    val costCrystals: Int,
    val timestamp: Long,
    val formattedDate: String,
    val status: String = "SUCCESS"
)

sealed class PurchaseResult {
    data object Success : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
}

data class GoalMilestone(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false
)

data class HunterGoal(
    val id: String,
    val title: String,
    val category: String,
    val targetDate: String,
    val milestones: List<GoalMilestone>,
    val xpReward: Int,
    val isCompleted: Boolean = false
)

data class DayActivityRecord(
    val dateKey: String, // e.g. "2026-10-22"
    val dayOfMonth: Int,
    val dayOfWeek: String, // "Mon", "Tue", etc.
    val focusMinutes: Int,
    val habitsCompleted: Int,
    val tasksCompleted: Int,
    val xpEarned: Int
)

data class FocusTimerState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val endTimestampMs: Long = 0L,
    val durationMs: Long = 25 * 60 * 1000L,
    val remainingPausedMs: Long = 25 * 60 * 1000L,
    val sessionTitle: String = "Deep Dungeon Focus",
    val isBreakMode: Boolean = false,
    val selectedMinutes: Int = 25
)

data class ProductivityReminders(
    val dailyProtocol: Boolean = true,
    val focusSanctuary: Boolean = true,
    val eveningReview: Boolean = false,
    val gateResetAlert: Boolean = true
)

data class SkillNode(
    val id: String,
    val name: String,
    val branch: String, // STRENGTH, AGILITY, INTELLECT, SHADOW
    val description: String,
    val level: Int,
    val maxLevel: Int = 5,
    val costPoints: Int = 1
)

data class AchievementBadge(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val isUnlocked: Boolean,
    val xpBonus: Int,
    val currentProgress: Int,
    val maxProgress: Int
)

data class GuildMember(
    val id: String,
    val name: String,
    val rank: HunterRank,
    val level: Int,
    val isOnline: Boolean,
    val role: String
)

data class CommunityMessage(
    val id: String,
    val senderName: String,
    val senderRank: HunterRank,
    val text: String,
    val timestamp: String,
    val isVoice: Boolean = false,
    val voiceDurationSec: Int = 0,
    val imageRes: Int? = null,
    val isUser: Boolean = false
)

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val type: String, // MISSION, LEVELUP, FRIEND, GUILD, REWARD, SHOP
    val timestamp: String,
    val isRead: Boolean = false
)

data class ActivityLog(
    val id: String,
    val title: String,
    val detail: String,
    val xp: Int,
    val timeAgo: String
)

data class UserAccount(
    val email: String,
    val passwordHash: String,
    val name: String
)

data class HunterProfile(
    val name: String = "Jinwoo Sung",
    val username: String = "@shadow_monarch",
    val title: String = "The Weakest Hunter",
    val bio: String = "I will rise beyond limits. From E-Rank to the apex of shadow power.",
    val level: Int = 1,
    val currentXp: Int = 120,
    val gold: Int = 850,
    val crystals: Int = 45,
    val streakDays: Int = 4,
    val bestStreak: Int = 14,
    val statPointsAvailable: Int = 3,
    val strength: Int = 14,
    val agility: Int = 12,
    val intelligence: Int = 16,
    val vitality: Int = 13,
    val perception: Int = 11,
    val equippedFrame: String = "frame_cyan",
    val equippedTheme: String = "CYAN",
    val equippedTitle: String = "The Weakest Hunter",
    val wallpaper: String = "bg_tuff",
    val avatarResName: String = "arise",
    val isVip: Boolean = false,
    val isProfilePublic: Boolean = true,
    val showOnlineStatus: Boolean = true,
    val prestigeLevel: Int = 0,
    val totalMissionsCompleted: Int = 12,
    val customAvatarUri: String? = null,
    val customCoverUri: String? = null,
    val customAiStyle: String? = null,
    val isPremium: Boolean = false
) {
    val rank: HunterRank get() = HunterRank.fromLevel(level)
    val xpForNextLevel: Int get() = 100 * level * level
    val xpProgressPercent: Float get() = (currentXp.toFloat() / xpForNextLevel.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val streakMultiplier: Float get() = when {
        streakDays >= 30 -> 2.5f
        streakDays >= 14 -> 2.0f
        streakDays >= 7 -> 1.5f
        streakDays >= 3 -> 1.2f
        else -> 1.0f
    }
}
