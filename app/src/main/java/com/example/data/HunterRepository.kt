package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.R
import com.example.data.security.SecureStorageManager
import com.example.data.security.SecurityAuditManager
import com.example.data.security.SecurityEventType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object HunterRepository {

    private const val PREFS_NAME = "arise_hunter_prefs"
    private var prefs: SharedPreferences? = null

    // Onboarding & Auth State
    private val _onboardingCompleted = MutableStateFlow(false)
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUserEmail = MutableStateFlow("")
    val currentUserEmail: StateFlow<String> = _currentUserEmail.asStateFlow()

    private val _registeredAccounts = MutableStateFlow<Map<String, UserAccount>>(emptyMap())

    // Game Core State
    private val _profile = MutableStateFlow(HunterProfile())
    val profile: StateFlow<HunterProfile> = _profile.asStateFlow()

    private val _missions = MutableStateFlow(getInitialMissions())
    val missions: StateFlow<List<Mission>> = _missions.asStateFlow()

    private val _habits = MutableStateFlow(getInitialHabits())
    val habits: StateFlow<List<ProductivityHabit>> = _habits.asStateFlow()

    private val _tasks = MutableStateFlow(getInitialTasks())
    val tasks: StateFlow<List<TodoTask>> = _tasks.asStateFlow()

    private val _focusRecords = MutableStateFlow(getInitialFocusRecords())
    val focusRecords: StateFlow<List<FocusRecord>> = _focusRecords.asStateFlow()

    private val _shopItems = MutableStateFlow(getInitialShopItems())
    val shopItems: StateFlow<List<ShopItem>> = _shopItems.asStateFlow()

    private val _purchaseHistory = MutableStateFlow(getInitialPurchaseHistory())
    val purchaseHistory: StateFlow<List<PurchaseRecord>> = _purchaseHistory.asStateFlow()

    private val _focusTimerState = MutableStateFlow(FocusTimerState())
    val focusTimerState: StateFlow<FocusTimerState> = _focusTimerState.asStateFlow()

    private val _goals = MutableStateFlow(getInitialGoals())
    val goals: StateFlow<List<HunterGoal>> = _goals.asStateFlow()

    private val _activityCalendar = MutableStateFlow(getInitialActivityCalendar())
    val activityCalendar: StateFlow<Map<String, DayActivityRecord>> = _activityCalendar.asStateFlow()

    private val _reminders = MutableStateFlow(ProductivityReminders())
    val reminders: StateFlow<ProductivityReminders> = _reminders.asStateFlow()

    private val _skills = MutableStateFlow(getInitialSkills())
    val skills: StateFlow<List<SkillNode>> = _skills.asStateFlow()

    private val _achievements = MutableStateFlow(getInitialAchievements())
    val achievements: StateFlow<List<AchievementBadge>> = _achievements.asStateFlow()

    private val _guildMembers = MutableStateFlow(getInitialGuildMembers())
    val guildMembers: StateFlow<List<GuildMember>> = _guildMembers.asStateFlow()

    private val _messages = MutableStateFlow(getInitialMessages())
    val messages: StateFlow<List<CommunityMessage>> = _messages.asStateFlow()

    private val _notifications = MutableStateFlow(getInitialNotifications())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _activityLogs = MutableStateFlow(getInitialActivityLogs())
    val activityLogs: StateFlow<List<ActivityLog>> = _activityLogs.asStateFlow()

    // Level Up dialog event
    private val _pendingLevelUp = MutableStateFlow<Int?>(null)
    val pendingLevelUp: StateFlow<Int?> = _pendingLevelUp.asStateFlow()

    // Daily reward claimed today
    private val _dailyRewardClaimed = MutableStateFlow(false)
    val dailyRewardClaimed: StateFlow<Boolean> = _dailyRewardClaimed.asStateFlow()

    // Active Quotes
    val quotes = listOf(
        "I will no longer run away. Only I can level up." to "Sung Jinwoo",
        "Arise. Even death cannot hold my shadows." to "Shadow Monarch",
        "Discipline is the bridge between E-Rank and Sovereign." to "Hunter Association",
        "Do not fear the gate. Conquer what lies within." to "Guild Master Baek",
        "Consistency is the true essence of awakening." to "Chairman Go Gunhee",
        "The system chose me for a reason. I will answer with power." to "Sung Jinwoo"
    )
    private val _currentQuoteIndex = MutableStateFlow(0)
    val currentQuoteIndex: StateFlow<Int> = _currentQuoteIndex.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        SecureStorageManager.init(context)
        loadFromPrefs()
        checkAndCompleteElapsedTimer()
    }

    private fun getTodayDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun loadFromPrefs() {
        val p = prefs ?: return
        _onboardingCompleted.value = p.getBoolean("onboarding_completed", false)
        _isLoggedIn.value = p.getBoolean("is_logged_in", false)
        _currentUserEmail.value = p.getString("current_user_email", "") ?: ""

        // Load Accounts (Migrate legacy plaintext to salted PBKDF2 hashes)
        val accountsJson = p.getString("registered_accounts", null)
        val accountsMap = mutableMapOf<String, UserAccount>()
        var needsMigrationSave = false
        if (accountsJson != null) {
            try {
                val jsonArr = JSONArray(accountsJson)
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    val email = obj.getString("email")
                    val storedPass = obj.getString("password")
                    val secureHash = if (storedPass.contains(":") || storedPass == "google_auth_token") {
                        storedPass
                    } else {
                        // Upgrade legacy plaintext to salted hash
                        needsMigrationSave = true
                        SecureStorageManager.hashPassword(storedPass)
                    }
                    accountsMap[email.lowercase()] = UserAccount(
                        email = email,
                        passwordHash = secureHash,
                        name = obj.getString("name")
                    )
                }
            } catch (_: Exception) {}
        }
        if (!accountsMap.containsKey("hunter@arise.io")) {
            accountsMap["hunter@arise.io"] = UserAccount(
                email = "hunter@arise.io",
                passwordHash = SecureStorageManager.hashPassword("hunter123"),
                name = "Jinwoo Sung"
            )
            needsMigrationSave = true
        }
        _registeredAccounts.value = accountsMap
        if (needsMigrationSave) {
            savePrefs()
        }

        // Load Profile
        val profJson = p.getString("hunter_profile", null)
        if (profJson != null) {
            try {
                val obj = JSONObject(profJson)
                _profile.value = HunterProfile(
                    name = obj.optString("name", "Jinwoo Sung"),
                    username = obj.optString("username", "@shadow_monarch"),
                    title = obj.optString("title", "The Weakest Hunter"),
                    bio = obj.optString("bio", "I will rise beyond limits. From E-Rank to the apex of shadow power."),
                    level = obj.optInt("level", 1),
                    currentXp = obj.optInt("currentXp", 120),
                    gold = obj.optInt("gold", 850),
                    crystals = obj.optInt("crystals", 45),
                    streakDays = obj.optInt("streakDays", 4),
                    bestStreak = obj.optInt("bestStreak", 14),
                    statPointsAvailable = obj.optInt("statPointsAvailable", 3),
                    strength = obj.optInt("strength", 14),
                    agility = obj.optInt("agility", 12),
                    intelligence = obj.optInt("intelligence", 16),
                    vitality = obj.optInt("vitality", 13),
                    perception = obj.optInt("perception", 11),
                    equippedFrame = obj.optString("equippedFrame", "frame_cyan"),
                    equippedTheme = obj.optString("equippedTheme", "CYAN"),
                    equippedTitle = obj.optString("equippedTitle", "The Weakest Hunter"),
                    wallpaper = obj.optString("wallpaper", "bg_tuff"),
                    avatarResName = obj.optString("avatarResName", "arise"),
                    isVip = obj.optBoolean("isVip", false),
                    isProfilePublic = obj.optBoolean("isProfilePublic", true),
                    showOnlineStatus = obj.optBoolean("showOnlineStatus", true),
                    prestigeLevel = obj.optInt("prestigeLevel", 0),
                    totalMissionsCompleted = obj.optInt("totalMissionsCompleted", 12),
                    customAvatarUri = obj.optString("customAvatarUri", "").takeIf { it.isNotBlank() },
                    customCoverUri = obj.optString("customCoverUri", "").takeIf { it.isNotBlank() },
                    customAiStyle = obj.optString("customAiStyle", "").takeIf { it.isNotBlank() },
                    isPremium = obj.optBoolean("isPremium", false)
                )
            } catch (_: Exception) {}
        }

        // Load Shop Items
        val shopJson = p.getString("shop_items", null)
        if (shopJson != null) {
            try {
                val arr = JSONArray(shopJson)
                val current = _shopItems.value.toMutableList()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.getString("id")
                    val isPurchased = obj.optBoolean("isPurchased", false)
                    val isEquipped = obj.optBoolean("isEquipped", false)
                    val idx = current.indexOfFirst { it.id == id }
                    if (idx >= 0) {
                        current[idx] = current[idx].copy(isPurchased = isPurchased, isEquipped = isEquipped)
                    }
                }
                _shopItems.value = current
            } catch (_: Exception) {}
        }

        // Load Purchase History
        val purchaseJson = p.getString("purchase_history", null)
        if (purchaseJson != null) {
            try {
                val arr = JSONArray(purchaseJson)
                val list = mutableListOf<PurchaseRecord>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        PurchaseRecord(
                            id = obj.getString("id"),
                            itemId = obj.getString("itemId"),
                            itemName = obj.getString("itemName"),
                            category = ShopCategory.valueOf(obj.optString("category", "THEMES")),
                            costGold = obj.optInt("costGold", 0),
                            costCrystals = obj.optInt("costCrystals", 0),
                            timestamp = obj.optLong("timestamp", 0L),
                            formattedDate = obj.optString("formattedDate", "Recent"),
                            status = obj.optString("status", "SUCCESS")
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    _purchaseHistory.value = list
                }
            } catch (_: Exception) {}
        }

        // Load Focus Timer
        val timerRunning = p.getBoolean("focus_timer_is_running", false)
        val timerPaused = p.getBoolean("focus_timer_is_paused", false)
        val timerEnd = p.getLong("focus_timer_end_timestamp", 0L)
        val timerDur = p.getLong("focus_timer_duration_ms", 25 * 60 * 1000L)
        val timerRem = p.getLong("focus_timer_remaining_paused_ms", 25 * 60 * 1000L)
        val timerTitle = p.getString("focus_timer_session_title", "Deep Dungeon Focus") ?: "Deep Dungeon Focus"
        val timerBreak = p.getBoolean("focus_timer_is_break", false)
        val timerMins = p.getInt("focus_timer_selected_minutes", 25)

        _focusTimerState.value = FocusTimerState(
            isRunning = timerRunning,
            isPaused = timerPaused,
            endTimestampMs = timerEnd,
            durationMs = timerDur,
            remainingPausedMs = timerRem,
            sessionTitle = timerTitle,
            isBreakMode = timerBreak,
            selectedMinutes = timerMins
        )

        // Load Goals
        val goalsJson = p.getString("hunter_goals", null)
        if (goalsJson != null) {
            try {
                val arr = JSONArray(goalsJson)
                val list = mutableListOf<HunterGoal>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val mArr = obj.optJSONArray("milestones") ?: JSONArray()
                    val mList = mutableListOf<GoalMilestone>()
                    for (j in 0 until mArr.length()) {
                        val mObj = mArr.getJSONObject(j)
                        mList.add(
                            GoalMilestone(
                                id = mObj.getString("id"),
                                title = mObj.getString("title"),
                                isCompleted = mObj.optBoolean("isCompleted", false)
                            )
                        )
                    }
                    list.add(
                        HunterGoal(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            category = obj.optString("category", "HUNTER RANK"),
                            targetDate = obj.optString("targetDate", "30 Days"),
                            milestones = mList,
                            xpReward = obj.optInt("xpReward", 500),
                            isCompleted = obj.optBoolean("isCompleted", false)
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    _goals.value = list
                }
            } catch (_: Exception) {}
        }

        // Load Reminders
        _reminders.value = ProductivityReminders(
            dailyProtocol = p.getBoolean("reminder_daily", true),
            focusSanctuary = p.getBoolean("reminder_focus", true),
            eveningReview = p.getBoolean("reminder_evening", false),
            gateResetAlert = p.getBoolean("reminder_reset", true)
        )
    }

    private fun savePrefs() {
        val p = prefs ?: return
        val editor = p.edit()
        editor.putBoolean("onboarding_completed", _onboardingCompleted.value)
        editor.putBoolean("is_logged_in", _isLoggedIn.value)
        editor.putString("current_user_email", _currentUserEmail.value)

        // Save accounts
        val jsonArr = JSONArray()
        _registeredAccounts.value.values.forEach { acc ->
            val obj = JSONObject()
            obj.put("email", acc.email)
            obj.put("password", acc.passwordHash)
            obj.put("name", acc.name)
            jsonArr.put(obj)
        }
        editor.putString("registered_accounts", jsonArr.toString())

        // Save profile
        val prof = _profile.value
        val pObj = JSONObject().apply {
            put("name", prof.name)
            put("username", prof.username)
            put("title", prof.title)
            put("bio", prof.bio)
            put("level", prof.level)
            put("currentXp", prof.currentXp)
            put("gold", prof.gold)
            put("crystals", prof.crystals)
            put("streakDays", prof.streakDays)
            put("bestStreak", prof.bestStreak)
            put("statPointsAvailable", prof.statPointsAvailable)
            put("strength", prof.strength)
            put("agility", prof.agility)
            put("intelligence", prof.intelligence)
            put("vitality", prof.vitality)
            put("perception", prof.perception)
            put("equippedFrame", prof.equippedFrame)
            put("equippedTheme", prof.equippedTheme)
            put("equippedTitle", prof.equippedTitle)
            put("wallpaper", prof.wallpaper)
            put("avatarResName", prof.avatarResName)
            put("isVip", prof.isVip)
            put("isProfilePublic", prof.isProfilePublic)
            put("showOnlineStatus", prof.showOnlineStatus)
            put("prestigeLevel", prof.prestigeLevel)
            put("totalMissionsCompleted", prof.totalMissionsCompleted)
            put("customAvatarUri", prof.customAvatarUri ?: "")
            put("customCoverUri", prof.customCoverUri ?: "")
            put("customAiStyle", prof.customAiStyle ?: "")
            put("isPremium", prof.isPremium)
        }
        editor.putString("hunter_profile", pObj.toString())

        // Save Shop Items
        val shopArr = JSONArray()
        _shopItems.value.forEach { item ->
            val sObj = JSONObject().apply {
                put("id", item.id)
                put("isPurchased", item.isPurchased)
                put("isEquipped", item.isEquipped)
            }
            shopArr.put(sObj)
        }
        editor.putString("shop_items", shopArr.toString())

        // Save Purchase History
        val pArr = JSONArray()
        _purchaseHistory.value.take(40).forEach { rec ->
            val o = JSONObject().apply {
                put("id", rec.id)
                put("itemId", rec.itemId)
                put("itemName", rec.itemName)
                put("category", rec.category.name)
                put("costGold", rec.costGold)
                put("costCrystals", rec.costCrystals)
                put("timestamp", rec.timestamp)
                put("formattedDate", rec.formattedDate)
                put("status", rec.status)
            }
            pArr.put(o)
        }
        editor.putString("purchase_history", pArr.toString())

        // Save Timer State
        val tState = _focusTimerState.value
        editor.putBoolean("focus_timer_is_running", tState.isRunning)
        editor.putBoolean("focus_timer_is_paused", tState.isPaused)
        editor.putLong("focus_timer_end_timestamp", tState.endTimestampMs)
        editor.putLong("focus_timer_duration_ms", tState.durationMs)
        editor.putLong("focus_timer_remaining_paused_ms", tState.remainingPausedMs)
        editor.putString("focus_timer_session_title", tState.sessionTitle)
        editor.putBoolean("focus_timer_is_break", tState.isBreakMode)
        editor.putInt("focus_timer_selected_minutes", tState.selectedMinutes)

        // Save Goals
        val gArr = JSONArray()
        _goals.value.forEach { g ->
            val gObj = JSONObject()
            gObj.put("id", g.id)
            gObj.put("title", g.title)
            gObj.put("category", g.category)
            gObj.put("targetDate", g.targetDate)
            gObj.put("xpReward", g.xpReward)
            gObj.put("isCompleted", g.isCompleted)
            val msArr = JSONArray()
            g.milestones.forEach { m ->
                val mObj = JSONObject()
                mObj.put("id", m.id)
                mObj.put("title", m.title)
                mObj.put("isCompleted", m.isCompleted)
                msArr.put(mObj)
            }
            gObj.put("milestones", msArr)
            gArr.put(gObj)
        }
        editor.putString("hunter_goals", gArr.toString())

        // Save Reminders
        val r = _reminders.value
        editor.putBoolean("reminder_daily", r.dailyProtocol)
        editor.putBoolean("reminder_focus", r.focusSanctuary)
        editor.putBoolean("reminder_evening", r.eveningReview)
        editor.putBoolean("reminder_reset", r.gateResetAlert)

        editor.apply()
    }

    // ==========================================
    // SECTION 9: SHOP & INVENTORY IMPLEMENTATION
    // ==========================================

    @Synchronized
    fun buyShopItem(item: ShopItem): PurchaseResult {
        val currentProfile = _profile.value
        val currentItems = _shopItems.value
        val targetItem = currentItems.find { it.id == item.id } ?: item

        // 0. Currency input sanitization & negative value prevention
        if (targetItem.costGold < 0 || targetItem.costCrystals < 0) {
            SecurityAuditManager.logEvent(
                SecurityEventType.SUSPICIOUS_PAYLOAD_BLOCKED,
                _currentUserEmail.value,
                "Negative currency purchase attempt rejected on item ${targetItem.id}",
                riskScore = 90
            )
            return PurchaseResult.Error("Security Error: Invalid currency parameter.")
        }

        // 1. Prevent duplicate purchases of unique cosmetic items
        if (!targetItem.isConsumable && targetItem.isPurchased) {
            return PurchaseResult.Error("Item is already owned in your inventory vault.")
        }

        // 2. Rank eligibility check
        if (currentProfile.level < targetItem.requiredRank.minLevel) {
            return PurchaseResult.Error(
                "Rank Requirement Unmet: Requires ${targetItem.requiredRank.code} (Level ${targetItem.requiredRank.minLevel}). You are Level ${currentProfile.level}."
            )
        }

        // 3. Virtual currency balance check
        if (currentProfile.gold < targetItem.costGold) {
            val missing = targetItem.costGold - currentProfile.gold
            return PurchaseResult.Error("Insufficient Gold. You need $missing more Gold.")
        }
        if (currentProfile.crystals < targetItem.costCrystals) {
            val missing = targetItem.costCrystals - currentProfile.crystals
            return PurchaseResult.Error("Insufficient Mana Crystals. You need $missing more Crystals.")
        }

        // 4. Atomic transaction: Deduct currencies
        _profile.update {
            it.copy(
                gold = it.gold - targetItem.costGold,
                crystals = it.crystals - targetItem.costCrystals
            )
        }

        // 5. Update item ownership
        _shopItems.update { list ->
            list.map {
                if (it.id == targetItem.id) {
                    if (it.isConsumable) it else it.copy(isPurchased = true)
                } else it
            }
        }

        // 6. Record transaction receipt in persistent purchase history
        val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())
        val record = PurchaseRecord(
            id = UUID.randomUUID().toString(),
            itemId = targetItem.id,
            itemName = targetItem.name,
            category = targetItem.category,
            costGold = targetItem.costGold,
            costCrystals = targetItem.costCrystals,
            timestamp = System.currentTimeMillis(),
            formattedDate = dateStr,
            status = "ACQUIRED"
        )
        _purchaseHistory.update { listOf(record) + it }

        // 7. Dispatch system notification
        _notifications.update {
            listOf(
                NotificationItem(
                    id = UUID.randomUUID().toString(),
                    title = "VAULT ACQUISITION",
                    message = "Successfully purchased ${targetItem.name}. Ready in your inventory.",
                    type = "SHOP",
                    timestamp = "Just now"
                )
            ) + it
        }

        // Consumable effects (potions)
        if (targetItem.isConsumable) {
            if (targetItem.id == "pot_health") {
                addXp(75, "Consumable: Full Recovery Elixir")
            } else if (targetItem.id == "pot_double") {
                addXp(150, "Consumable: Dungeon Double XP Draught")
            }
        }

        savePrefs()
        return PurchaseResult.Success
    }

    fun equipShopItem(item: ShopItem) {
        if (!item.isPurchased && !item.isConsumable) return

        _shopItems.update { list ->
            list.map {
                if (it.category == item.category) {
                    it.copy(isEquipped = it.id == item.id)
                } else it
            }
        }

        when (item.category) {
            ShopCategory.THEMES -> _profile.update { it.copy(equippedTheme = item.id) }
            ShopCategory.FRAMES -> _profile.update { it.copy(equippedFrame = item.id) }
            ShopCategory.AVATARS -> {
                val resName = when (item.id) {
                    "avatar_rise" -> "rise"
                    "avatar_hunter" -> "avatar_hunter"
                    "avatar_arise" -> "arise"
                    "avatar_igris" -> "rise"
                    else -> "arise"
                }
                _profile.update { it.copy(avatarResName = resName) }
            }
            ShopCategory.TITLES -> _profile.update {
                it.copy(equippedTitle = item.name, title = item.name)
            }
            else -> {}
        }

        _notifications.update {
            listOf(
                NotificationItem(
                    id = UUID.randomUUID().toString(),
                    title = "EQUIPMENT UPDATED",
                    message = "Active cosmetic changed to ${item.name}.",
                    type = "SHOP",
                    timestamp = "Just now"
                )
            ) + it
        }

        savePrefs()
    }

    fun unequipShopItem(item: ShopItem) {
        _shopItems.update { list ->
            list.map {
                if (it.id == item.id) it.copy(isEquipped = false) else it
            }
        }

        when (item.category) {
            ShopCategory.THEMES -> _profile.update { it.copy(equippedTheme = "CYAN") }
            ShopCategory.FRAMES -> _profile.update { it.copy(equippedFrame = "none") }
            ShopCategory.AVATARS -> _profile.update { it.copy(avatarResName = "arise") }
            ShopCategory.TITLES -> _profile.update {
                it.copy(equippedTitle = "The Weakest Hunter", title = "The Weakest Hunter")
            }
            else -> {}
        }

        savePrefs()
    }

    // ==========================================
    // SECTION 10: PRODUCTIVITY TOOLS LOGIC
    // ==========================================

    // Focus Timer Lifecycle Persistence Handling
    fun checkAndCompleteElapsedTimer(): Boolean {
        val state = _focusTimerState.value
        if (state.isRunning && state.endTimestampMs > 0) {
            val now = System.currentTimeMillis()
            if (now >= state.endTimestampMs) {
                // Completed while backgrounded or reopened!
                val mins = state.selectedMinutes
                val title = state.sessionTitle
                _focusTimerState.value = FocusTimerState(
                    isRunning = false,
                    isPaused = false,
                    endTimestampMs = 0L,
                    durationMs = mins * 60 * 1000L,
                    remainingPausedMs = mins * 60 * 1000L,
                    sessionTitle = title,
                    isBreakMode = false,
                    selectedMinutes = mins
                )
                savePrefs()
                if (!state.isBreakMode) {
                    logFocusSession(mins, title)
                } else {
                    _notifications.update {
                        listOf(
                            NotificationItem(
                                id = UUID.randomUUID().toString(),
                                title = "RECOVERY COMPLETE",
                                message = "Break finished. Your mana core is fully recharged.",
                                type = "FOCUS",
                                timestamp = "Just now"
                            )
                        ) + it
                    }
                }
                return true
            }
        }
        return false
    }

    fun startFocusTimer(minutes: Int, title: String, isBreak: Boolean = false) {
        val durationMs = minutes * 60 * 1000L
        val endMs = System.currentTimeMillis() + durationMs
        _focusTimerState.value = FocusTimerState(
            isRunning = true,
            isPaused = false,
            endTimestampMs = endMs,
            durationMs = durationMs,
            remainingPausedMs = durationMs,
            sessionTitle = title.ifBlank { if (isBreak) "Hunter Recovery Break" else "Deep Dungeon Focus" },
            isBreakMode = isBreak,
            selectedMinutes = minutes
        )
        savePrefs()
    }

    fun pauseFocusTimer() {
        val current = _focusTimerState.value
        if (!current.isRunning) return
        val remaining = (current.endTimestampMs - System.currentTimeMillis()).coerceAtLeast(0L)
        _focusTimerState.value = current.copy(
            isRunning = false,
            isPaused = true,
            remainingPausedMs = remaining
        )
        savePrefs()
    }

    fun resumeFocusTimer() {
        val current = _focusTimerState.value
        if (!current.isPaused) return
        val endMs = System.currentTimeMillis() + current.remainingPausedMs
        _focusTimerState.value = current.copy(
            isRunning = true,
            isPaused = false,
            endTimestampMs = endMs
        )
        savePrefs()
    }

    fun resetFocusTimer() {
        val current = _focusTimerState.value
        val dur = current.selectedMinutes * 60 * 1000L
        _focusTimerState.value = FocusTimerState(
            isRunning = false,
            isPaused = false,
            endTimestampMs = 0L,
            durationMs = dur,
            remainingPausedMs = dur,
            sessionTitle = current.sessionTitle,
            isBreakMode = current.isBreakMode,
            selectedMinutes = current.selectedMinutes
        )
        savePrefs()
    }

    fun logFocusSession(minutes: Int, title: String) {
        val xp = minutes * 5
        val record = FocusRecord(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { "Deep Shadow Focus" },
            minutes = minutes,
            date = "Today",
            xpGained = xp
        )
        _focusRecords.update { listOf(record) + it }
        addXp(xp, "Focus: $minutes min ($title)")
        _profile.update { it.copy(gold = it.gold + minutes * 4) }

        // Update Calendar Activity
        recordActivityIncrement(focusMins = minutes, habitsCount = 0, tasksCount = 0, xpEarned = xp)

        _notifications.update {
            listOf(
                NotificationItem(
                    id = UUID.randomUUID().toString(),
                    title = "FOCUS SANCTUARY CLEARED",
                    message = "Completed $minutes min deep session ($title). +$xp XP awarded.",
                    type = "FOCUS",
                    timestamp = "Just now"
                )
            ) + it
        }

        savePrefs()
    }

    // Habits
    fun toggleHabit(habitId: String) {
        _habits.update { list ->
            list.map {
                if (it.id == habitId) {
                    val newState = !it.isDoneToday
                    val newStreak = if (newState) it.streak + 1 else (it.streak - 1).coerceAtLeast(0)
                    it.copy(isDoneToday = newState, streak = newStreak)
                } else it
            }
        }
        val habit = _habits.value.find { it.id == habitId }
        if (habit?.isDoneToday == true) {
            addXp(40, "Habit: ${habit.title}")
            _profile.update { it.copy(gold = it.gold + 50) }
            recordActivityIncrement(focusMins = 0, habitsCount = 1, tasksCount = 0, xpEarned = 40)
        }
        savePrefs()
    }

    fun addHabit(title: String, targetDays: Int = 7) {
        _habits.update {
            listOf(
                ProductivityHabit(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    streak = 1,
                    isDoneToday = true,
                    targetDaysAWeek = targetDays
                )
            ) + it
        }
        addXp(30, "Habit established: $title")
        recordActivityIncrement(focusMins = 0, habitsCount = 1, tasksCount = 0, xpEarned = 30)
        savePrefs()
    }

    fun deleteHabit(habitId: String) {
        _habits.update { it.filterNot { h -> h.id == habitId } }
        savePrefs()
    }

    // Tasks & Daily Planner
    fun toggleTask(taskId: String) {
        _tasks.update { list ->
            list.map {
                if (it.id == taskId) {
                    val newState = !it.isCompleted
                    it.copy(isCompleted = newState)
                } else it
            }
        }
        val task = _tasks.value.find { it.id == taskId }
        if (task?.isCompleted == true) {
            addXp(25, "Task completed: ${task.title}")
            _profile.update { it.copy(gold = it.gold + 30) }
            recordActivityIncrement(focusMins = 0, habitsCount = 0, tasksCount = 1, xpEarned = 25)
        }
        savePrefs()
    }

    fun addTask(title: String, priority: String, deadline: String = "Today") {
        _tasks.update {
            listOf(
                TodoTask(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    priority = priority,
                    isCompleted = false,
                    deadline = deadline
                )
            ) + it
        }
        savePrefs()
    }

    fun deleteTask(taskId: String) {
        _tasks.update { it.filterNot { t -> t.id == taskId } }
        savePrefs()
    }

    // Goal-setting
    fun addGoal(title: String, category: String, targetDate: String, milestoneTitles: List<String>, xpReward: Int) {
        val ms = milestoneTitles.filter { it.isNotBlank() }.map {
            GoalMilestone(id = UUID.randomUUID().toString(), title = it, isCompleted = false)
        }
        val goal = HunterGoal(
            id = UUID.randomUUID().toString(),
            title = title,
            category = category,
            targetDate = targetDate,
            milestones = ms,
            xpReward = xpReward,
            isCompleted = false
        )
        _goals.update { listOf(goal) + it }
        _notifications.update {
            listOf(
                NotificationItem(
                    id = UUID.randomUUID().toString(),
                    title = "HUNTER GOAL SEALED",
                    message = "Target: \"$title\". Complete milestones to harvest $xpReward XP.",
                    type = "MISSION",
                    timestamp = "Just now"
                )
            ) + it
        }
        savePrefs()
    }

    fun toggleMilestone(goalId: String, milestoneId: String) {
        _goals.update { list ->
            list.map { goal ->
                if (goal.id == goalId) {
                    val updatedMs = goal.milestones.map { m ->
                        if (m.id == milestoneId) m.copy(isCompleted = !m.isCompleted) else m
                    }
                    val allDone = updatedMs.isNotEmpty() && updatedMs.all { it.isCompleted }
                    if (allDone && !goal.isCompleted) {
                        addXp(goal.xpReward, "Goal Accomplished: ${goal.title}")
                        _profile.update { it.copy(gold = it.gold + goal.xpReward * 2) }
                    }
                    goal.copy(milestones = updatedMs, isCompleted = allDone)
                } else goal
            }
        }
        savePrefs()
    }

    fun deleteGoal(goalId: String) {
        _goals.update { it.filterNot { g -> g.id == goalId } }
        savePrefs()
    }

    // Activity Calendar Increments
    private fun recordActivityIncrement(focusMins: Int, habitsCount: Int, tasksCount: Int, xpEarned: Int) {
        val key = getTodayDateKey()
        val current = _activityCalendar.value.toMutableMap()
        val existing = current[key] ?: DayActivityRecord(
            dateKey = key,
            dayOfMonth = SimpleDateFormat("d", Locale.getDefault()).format(Date()).toIntOrNull() ?: 22,
            dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(Date()),
            focusMinutes = 0,
            habitsCompleted = 0,
            tasksCompleted = 0,
            xpEarned = 0
        )
        current[key] = existing.copy(
            focusMinutes = existing.focusMinutes + focusMins,
            habitsCompleted = existing.habitsCompleted + habitsCount,
            tasksCompleted = existing.tasksCompleted + tasksCount,
            xpEarned = existing.xpEarned + xpEarned
        )
        _activityCalendar.value = current
    }

    // Reminders
    fun toggleReminder(type: String) {
        _reminders.update {
            when (type) {
                "daily" -> it.copy(dailyProtocol = !it.dailyProtocol)
                "focus" -> it.copy(focusSanctuary = !it.focusSanctuary)
                "evening" -> it.copy(eveningReview = !it.eveningReview)
                "reset" -> it.copy(gateResetAlert = !it.gateResetAlert)
                else -> it
            }
        }
        savePrefs()
    }

    // ==========================================
    // OTHER CORE GAME METHODS (XP, STATS, AUTH)
    // ==========================================

    fun setShowOnlineStatus(show: Boolean) {
        _profile.update { it.copy(showOnlineStatus = show) }
        savePrefs()
    }

    fun setOnboardingCompleted(completed: Boolean) {
        _onboardingCompleted.value = completed
        savePrefs()
    }

    fun resetOnboarding() {
        _onboardingCompleted.value = false
        savePrefs()
    }

    fun register(email: String, pass: String, name: String): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (pass.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters long."))
        }
        if (!SecurityAuditManager.checkRateLimit("reg_$cleanEmail", 1500L)) {
            return Result.failure(IllegalStateException("Registration rate limit exceeded. Please wait a moment."))
        }
        if (_registeredAccounts.value.containsKey(cleanEmail)) {
            return Result.failure(IllegalArgumentException("An account with this email already exists."))
        }

        // Salted PBKDF2 hash - password is NEVER stored in plaintext
        val saltedHash = SecureStorageManager.hashPassword(pass)
        val account = UserAccount(email = cleanEmail, passwordHash = saltedHash, name = name.ifBlank { "Hunter" })
        _registeredAccounts.update { it + (cleanEmail to account) }
        _currentUserEmail.value = cleanEmail
        _isLoggedIn.value = true
        _profile.update { it.copy(name = name.ifBlank { it.name }) }
        SecurityAuditManager.logEvent(SecurityEventType.AUTH_REGISTER_SUCCESS, cleanEmail, "Hunter account created")
        savePrefs()
        return Result.success(Unit)
    }

    fun signIn(email: String, pass: String): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please enter your email."))
        }

        // Check Brute-force Account Lockout
        if (SecurityAuditManager.isAccountLocked(cleanEmail)) {
            val remainingSecs = SecurityAuditManager.getRemainingLockoutSeconds(cleanEmail)
            return Result.failure(IllegalStateException("Account temporarily locked due to excessive failed attempts. Try again in ${remainingSecs}s."))
        }

        val account = _registeredAccounts.value[cleanEmail]
        if (account == null) {
            SecurityAuditManager.recordFailedLogin(cleanEmail)
            return Result.failure(IllegalArgumentException("Invalid email or password."))
        }

        // Constant-time salted verification with legacy migration support
        val isPasswordCorrect = if (account.passwordHash.contains(":")) {
            SecureStorageManager.verifyPassword(pass, account.passwordHash)
        } else {
            // Legacy match & upgrade
            (account.passwordHash == pass).also { matched ->
                if (matched) {
                    val upgraded = account.copy(passwordHash = SecureStorageManager.hashPassword(pass))
                    _registeredAccounts.update { it + (cleanEmail to upgraded) }
                }
            }
        }

        if (!isPasswordCorrect) {
            SecurityAuditManager.recordFailedLogin(cleanEmail)
            return Result.failure(IllegalArgumentException("Invalid email or password."))
        }

        SecurityAuditManager.recordSuccessfulLogin(cleanEmail)
        _currentUserEmail.value = cleanEmail
        _isLoggedIn.value = true
        _profile.update { it.copy(name = account.name) }
        savePrefs()
        return Result.success(Unit)
    }

    fun signInWithGoogle(displayName: String, email: String): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        if (!_registeredAccounts.value.containsKey(cleanEmail)) {
            val account = UserAccount(email = cleanEmail, passwordHash = "google_auth_token", name = displayName)
            _registeredAccounts.update { it + (cleanEmail to account) }
        }
        SecurityAuditManager.recordSuccessfulLogin(cleanEmail)
        _currentUserEmail.value = cleanEmail
        _isLoggedIn.value = true
        _profile.update { it.copy(name = displayName) }
        savePrefs()
        return Result.success(Unit)
    }

    fun resetPassword(email: String): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Enter a valid email address."))
        }
        if (!SecurityAuditManager.checkRateLimit("pwd_reset_$cleanEmail", 5000L)) {
            return Result.failure(IllegalStateException("Password reset request rate-limited. Please wait."))
        }
        if (!_registeredAccounts.value.containsKey(cleanEmail)) {
            // Safe response to prevent account enumeration
            return Result.success(Unit)
        }
        _notifications.update {
            listOf(
                NotificationItem(
                    id = UUID.randomUUID().toString(),
                    title = "Security Dispatch",
                    message = "Password reset instructions transmitted to $cleanEmail.",
                    type = "SYSTEM",
                    timestamp = "Just now"
                )
            ) + it
        }
        return Result.success(Unit)
    }

    fun signOut() {
        _isLoggedIn.value = false
        savePrefs()
    }

    fun nextQuote() {
        _currentQuoteIndex.update { (it + 1) % quotes.size }
    }

    fun dismissLevelUp() {
        _pendingLevelUp.value = null
    }

    fun addXp(amount: Int, sourceTitle: String) {
        if (amount <= 0 || amount > 10000) {
            SecurityAuditManager.logEvent(
                SecurityEventType.SUSPICIOUS_PAYLOAD_BLOCKED,
                _currentUserEmail.value,
                "Rejected anomalous XP gain: $amount from $sourceTitle",
                riskScore = 80
            )
            return
        }
        val multiplier = _profile.value.streakMultiplier
        val actualXp = (amount * multiplier).toInt().coerceAtLeast(1)
        var newXp = _profile.value.currentXp + actualXp
        var level = _profile.value.level
        var statPoints = _profile.value.statPointsAvailable
        var leveledUp = false
        var levelsGained = 0

        while (newXp >= (100 * level * level)) {
            newXp -= (100 * level * level)
            level += 1
            statPoints += 3
            leveledUp = true
            levelsGained += 1
        }

        val previousRank = _profile.value.rank
        _profile.update {
            it.copy(
                level = level,
                currentXp = newXp,
                statPointsAvailable = statPoints
            )
        }

        _activityLogs.update {
            listOf(
                ActivityLog(
                    id = UUID.randomUUID().toString(),
                    title = "XP Acquired: +$actualXp",
                    detail = sourceTitle,
                    xp = actualXp,
                    timeAgo = "Just now"
                )
            ) + it.take(19)
        }

        if (leveledUp) {
            _pendingLevelUp.value = level
            val newRank = HunterRank.fromLevel(level)
            _notifications.update {
                listOf(
                    NotificationItem(
                        id = UUID.randomUUID().toString(),
                        title = "SYSTEM: LEVEL UP!",
                        message = "You have reached Level $level! (+${levelsGained * 3} Stat Points awarded).",
                        type = "LEVELUP",
                        timestamp = "Just now"
                    )
                ) + it
            }
            if (newRank != previousRank) {
                _notifications.update {
                    listOf(
                        NotificationItem(
                            id = UUID.randomUUID().toString(),
                            title = "RANK ADVANCEMENT: ${newRank.code}",
                            message = "The Association has re-evaluated your mana core: ${newRank.title}.",
                            type = "LEVELUP",
                            timestamp = "Just now"
                        )
                    ) + it
                }
            }
        }
        savePrefs()
    }

    fun completeMission(missionId: String) {
        val m = _missions.value.find { it.id == missionId } ?: return
        if (m.isCompleted) return

        _missions.update { list ->
            list.map {
                if (it.id == missionId) it.copy(isCompleted = true, progress = it.target) else it
            }
        }

        addXp(m.xpReward, m.title)

        _profile.update {
            it.copy(
                gold = it.gold + m.goldReward,
                crystals = it.crystals + m.crystalReward,
                totalMissionsCompleted = it.totalMissionsCompleted + 1
            )
        }

        val dailyIncomplete = _missions.value.filter { it.isDaily && !it.isCompleted }
        if (dailyIncomplete.isEmpty()) {
            _profile.update {
                it.copy(
                    streakDays = it.streakDays + 1,
                    bestStreak = maxOf(it.bestStreak, it.streakDays + 1)
                )
            }
        }
        savePrefs()
    }

    fun addCustomMission(title: String, desc: String, category: MissionCategory, xp: Int, target: Int, unit: String) {
        val newMission = Mission(
            id = UUID.randomUUID().toString(),
            title = title,
            description = desc,
            category = category,
            difficulty = if (xp > 300) "A" else if (xp > 150) "B" else "C",
            xpReward = xp,
            goldReward = xp * 2,
            crystalReward = if (xp >= 200) 5 else 0,
            isCompleted = false,
            isPersonal = true,
            isDaily = true,
            progress = 0,
            target = target.coerceAtLeast(1),
            unit = unit
        )
        _missions.update { listOf(newMission) + it }
        _notifications.update {
            listOf(
                NotificationItem(
                    id = UUID.randomUUID().toString(),
                    title = "Personal Quest Created",
                    message = "Quest \"$title\" added to your daily protocol.",
                    type = "MISSION",
                    timestamp = "Just now"
                )
            ) + it
        }
        savePrefs()
    }

    fun allocateStat(statName: String) {
        if (_profile.value.statPointsAvailable <= 0) return
        _profile.update {
            val remaining = it.statPointsAvailable - 1
            when (statName) {
                "STR" -> it.copy(strength = it.strength + 1, statPointsAvailable = remaining)
                "AGI" -> it.copy(agility = it.agility + 1, statPointsAvailable = remaining)
                "INT" -> it.copy(intelligence = it.intelligence + 1, statPointsAvailable = remaining)
                "VIT" -> it.copy(vitality = it.vitality + 1, statPointsAvailable = remaining)
                "PER" -> it.copy(perception = it.perception + 1, statPointsAvailable = remaining)
                else -> it
            }
        }
        savePrefs()
    }

    fun upgradeSkill(skillId: String) {
        val skill = _skills.value.find { it.id == skillId } ?: return
        if (skill.level >= skill.maxLevel) return
        if (_profile.value.statPointsAvailable < skill.costPoints) return

        _profile.update { it.copy(statPointsAvailable = it.statPointsAvailable - skill.costPoints) }
        _skills.update { list ->
            list.map {
                if (it.id == skillId) it.copy(level = it.level + 1) else it
            }
        }
        savePrefs()
    }

    fun claimDailyReward() {
        if (_dailyRewardClaimed.value) return
        _dailyRewardClaimed.value = true
        _profile.update {
            it.copy(
                gold = it.gold + 300,
                crystals = it.crystals + 15
            )
        }
        addXp(150, "Daily Hunter Login Protocol")
        savePrefs()
    }

    fun sendMessage(text: String) {
        val userRank = _profile.value.rank
        val userMsg = CommunityMessage(
            id = UUID.randomUUID().toString(),
            senderName = _profile.value.name,
            senderRank = userRank,
            text = text,
            timestamp = "Just now",
            isUser = true
        )
        _messages.update { it + userMsg }

        val responses = listOf(
            "Acknowledged, Hunter! Meet us at the Gate raid tonight.",
            "Great work on clearing those quests. The Shadow Monarch is advancing.",
            "Nice stat growth! Don't neglect your daily endurance workout.",
            "System alert: A red gate has been detected in Sector 4."
        )
        val hunterReplies = listOf(
            Triple("Cha Hae-In", HunterRank.S, responses[0]),
            Triple("Woo Jin-Chul", HunterRank.A, responses[1]),
            Triple("Baek Yoonho", HunterRank.S, responses[2]),
            Triple("Guild Monitor", HunterRank.B, responses[3])
        )
        val reply = hunterReplies.random()
        val guildReply = CommunityMessage(
            id = UUID.randomUUID().toString(),
            senderName = reply.first,
            senderRank = reply.second,
            text = reply.third,
            timestamp = "Just now",
            isUser = false
        )
        _messages.update { it + guildReply }
    }

    fun updateProfile(name: String, username: String, title: String, bio: String) {
        _profile.update {
            it.copy(name = name, username = username, title = title, bio = bio)
        }
        savePrefs()
    }

    fun updateAvatar(avatarName: String) {
        _profile.update { it.copy(avatarResName = avatarName) }
        savePrefs()
    }

    fun toggleProfilePrivacy() {
        _profile.update { it.copy(isProfilePublic = !it.isProfilePublic) }
        savePrefs()
    }

    fun setWallpaper(bg: String) {
        _profile.update { it.copy(wallpaper = bg) }
        savePrefs()
    }

    fun setAccentColor(theme: String) {
        _profile.update { it.copy(equippedTheme = theme) }
        savePrefs()
    }

    fun toggleVip() {
        _profile.update { it.copy(isVip = !it.isVip) }
        savePrefs()
    }

    fun setCustomAvatar(uri: String?, style: String? = null) {
        _profile.update {
            it.copy(
                customAvatarUri = uri,
                customAiStyle = style ?: it.customAiStyle
            )
        }
        savePrefs()
    }

    fun setCustomCover(uri: String?) {
        _profile.update { it.copy(customCoverUri = uri) }
        savePrefs()
    }

    fun setPremiumStatus(isPremium: Boolean) {
        _profile.update { it.copy(isPremium = isPremium) }
        savePrefs()
    }

    fun prestigeAwaken() {
        if (_profile.value.level < 20) return
        _profile.update {
            it.copy(
                level = 1,
                currentXp = 0,
                prestigeLevel = it.prestigeLevel + 1,
                statPointsAvailable = it.statPointsAvailable + 10,
                crystals = it.crystals + 50
            )
        }
        _notifications.update {
            listOf(
                NotificationItem(
                    id = UUID.randomUUID().toString(),
                    title = "SOVEREIGN AWAKENING (PRESTIGE)",
                    message = "You have undergone prestige rebirth! Permanent +10% XP multiplier unlocked.",
                    type = "LEVELUP",
                    timestamp = "Just now"
                )
            ) + it
        }
        savePrefs()
    }

    fun markNotificationsRead() {
        _notifications.update { list -> list.map { it.copy(isRead = true) } }
        savePrefs()
    }

    // ==========================================
    // INITIAL SEED DATA
    // ==========================================

    private fun getInitialMissions(): List<Mission> = listOf(
        Mission(
            id = "m1",
            title = "Physical Quest: 100 Push-ups",
            description = "Fundamental strength regimen demanded by the System.",
            category = MissionCategory.FITNESS,
            difficulty = "E",
            xpReward = 120,
            goldReward = 200,
            crystalReward = 2,
            target = 100,
            unit = "reps"
        ),
        Mission(
            id = "m2",
            title = "Endurance Quest: 10km Run",
            description = "Build the cardiovascular stamina required to escape dungeon traps.",
            category = MissionCategory.FITNESS,
            difficulty = "D",
            xpReward = 200,
            goldReward = 350,
            crystalReward = 5,
            target = 10,
            unit = "km"
        ),
        Mission(
            id = "m3",
            title = "Core Awakening: 100 Sit-ups & Squats",
            description = "Fortify agility and core power to wield heavy daggers.",
            category = MissionCategory.FITNESS,
            difficulty = "E",
            xpReward = 150,
            goldReward = 250,
            crystalReward = 2,
            target = 200,
            unit = "reps"
        ),
        Mission(
            id = "m4",
            title = "Deep Study: 45 Min Intellect Protocol",
            description = "Study coding, tactics, or languages to sharpen Intelligence.",
            category = MissionCategory.FOCUS,
            difficulty = "C",
            xpReward = 180,
            goldReward = 300,
            crystalReward = 4,
            target = 45,
            unit = "mins"
        ),
        Mission(
            id = "m5",
            title = "Weekly Raid: Clear 5 High-Focus Dungeons",
            description = "Log over 150 minutes of deep focus timer sessions this week.",
            category = MissionCategory.DUNGEON,
            difficulty = "B",
            xpReward = 500,
            goldReward = 1200,
            crystalReward = 15,
            isDaily = false,
            isWeekly = true,
            target = 5,
            unit = "clears"
        ),
        Mission(
            id = "m6",
            title = "Special Challenge: Red Gate Boss - Igris Trial",
            description = "Complete 3 intense fitness and study sessions in 24 hours.",
            category = MissionCategory.DUNGEON,
            difficulty = "A",
            xpReward = 800,
            goldReward = 2500,
            crystalReward = 25,
            isDaily = false,
            isSpecial = true,
            target = 3,
            unit = "trials"
        )
    )

    private fun getInitialHabits(): List<ProductivityHabit> = listOf(
        ProductivityHabit("h1", "Hydration (3L Water)", 5, isDoneToday = true),
        ProductivityHabit("h2", "Morning Physical Conditioning", 4, isDoneToday = false),
        ProductivityHabit("h3", "Pomodoro Focus Study (2 Sessions)", 12, isDoneToday = true),
        ProductivityHabit("h4", "Cold Shower Recovery", 3, isDoneToday = false),
        ProductivityHabit("h5", "Evening Shadow Reflection & Journal", 7, isDoneToday = false)
    )

    private fun getInitialTasks(): List<TodoTask> = listOf(
        TodoTask("t1", "Complete Software Architecture Module", "High", isCompleted = false, deadline = "14:00 PM"),
        TodoTask("t2", "Workout: Calisthenics & 5k run", "High", isCompleted = true, deadline = "10:30 AM"),
        TodoTask("t3", "Read 20 pages of System Lore", "Medium", isCompleted = false, deadline = "18:00 PM"),
        TodoTask("t4", "Plan next week's raid targets", "Low", isCompleted = false, deadline = "21:00 PM")
    )

    private fun getInitialFocusRecords(): List<FocusRecord> = listOf(
        FocusRecord("f1", "Deep Algorithm Practice", 45, "Today", 225),
        FocusRecord("f2", "Android Architecture Study", 30, "Yesterday", 150),
        FocusRecord("f3", "Solo Training Session", 25, "2 days ago", 125)
    )

    private fun getInitialShopItems(): List<ShopItem> = listOf(
        // THEMES
        ShopItem(
            id = "CYAN",
            name = "Cyber Cyan Aura",
            category = ShopCategory.THEMES,
            description = "Electric neon cyan system glow.",
            lore = "The foundational mana hue of the Architect's System.",
            costGold = 0,
            costCrystals = 0,
            isPurchased = true,
            isEquipped = true,
            requiredRank = HunterRank.E,
            statBonus = "+5% Focus Efficiency"
        ),
        ShopItem(
            id = "VIOLET",
            name = "Monarch Violet Aura",
            category = ShopCategory.THEMES,
            description = "Shadow power purple interface.",
            lore = "Condensation of monarch mana summoned from the abyss.",
            costGold = 1200,
            costCrystals = 10,
            isPurchased = false,
            isEquipped = false,
            requiredRank = HunterRank.C,
            statBonus = "+10% All Quest XP"
        ),
        ShopItem(
            id = "CRIMSON",
            name = "Red Gate Crimson Aura",
            category = ShopCategory.THEMES,
            description = "High-lethal blood gate red interface.",
            lore = "Tempered inside the blizzard and blood of unranked red gates.",
            costGold = 2200,
            costCrystals = 25,
            isPurchased = false,
            isEquipped = false,
            requiredRank = HunterRank.B,
            statBonus = "+15% Gold from Missions"
        ),
        ShopItem(
            id = "GOLD",
            name = "Sovereign Gold Aura",
            category = ShopCategory.THEMES,
            description = "Glorious golden sovereign interface.",
            lore = "The incandescent brilliance of ruler vessels who dominate nations.",
            costGold = 4500,
            costCrystals = 50,
            isPurchased = false,
            isEquipped = false,
            requiredRank = HunterRank.S,
            statBonus = "+20% Quest XP & Stat Boost"
        ),

        // PROFILE FRAMES
        ShopItem(
            id = "frame_cyan",
            name = "System Cyan Glow",
            category = ShopCategory.FRAMES,
            description = "Futuristic pulsing cyan border.",
            lore = "Standard association containment border with energy pulse.",
            costGold = 400,
            costCrystals = 0,
            isPurchased = true,
            isEquipped = true,
            requiredRank = HunterRank.E,
            statBonus = "Clean Energy Shield"
        ),
        ShopItem(
            id = "frame_monarch",
            name = "Monarch Purple Aegis",
            category = ShopCategory.FRAMES,
            description = "Dark shadow essence radiating monarch power.",
            lore = "Carved from high-tier shadow stones extracted in the double dungeon.",
            costGold = 1500,
            costCrystals = 15,
            isPurchased = false,
            isEquipped = false,
            requiredRank = HunterRank.C,
            statBonus = "+5 Perception"
        ),
        ShopItem(
            id = "frame_crimson",
            name = "Red Gate Slayer Frame",
            category = ShopCategory.FRAMES,
            description = "Ominous crimson flames from the Red Gate boss.",
            lore = "Forged directly from the armor helm of Commander Igris.",
            costGold = 2500,
            costCrystals = 30,
            isPurchased = false,
            isEquipped = false,
            requiredRank = HunterRank.B,
            statBonus = "+8 Strength"
        ),
        ShopItem(
            id = "frame_sovereign",
            name = "Golden Sovereign Aegis",
            category = ShopCategory.FRAMES,
            description = "Supreme radiant aura fit for national-level hunters.",
            lore = "Recognized globally as the mark of a hunter possessing world power.",
            costGold = 5500,
            costCrystals = 60,
            isPurchased = false,
            isEquipped = false,
            requiredRank = HunterRank.S,
            statBonus = "+15 All Core Stats"
        ),

        // AVATARS
        ShopItem(
            id = "avatar_arise",
            name = "Shadow Monarch Silhouette",
            category = ShopCategory.AVATARS,
            description = "Ascended commander with piercing azure glow.",
            lore = "The sovereign silhouette that commands millions of shadows.",
            costGold = 800,
            costCrystals = 5,
            iconRes = R.drawable.arise,
            isPurchased = true,
            isEquipped = true,
            requiredRank = HunterRank.E,
            statBonus = "Shadow Command"
        ),
        ShopItem(
            id = "avatar_rise",
            name = "Awakened Dark Sovereign",
            category = ShopCategory.AVATARS,
            description = "Full dark sovereign stance shrouded in mist.",
            lore = "Unleashed sovereign presence during high-rank dungeon raids.",
            costGold = 1800,
            costCrystals = 20,
            iconRes = R.drawable.rise,
            isPurchased = false,
            isEquipped = false,
            requiredRank = HunterRank.C,
            statBonus = "+10% Mana Storage"
        ),
        ShopItem(
            id = "avatar_hunter",
            name = "Guild Vanguard Attire",
            category = ShopCategory.AVATARS,
            description = "Formal association tactical vest and heavy combat gear.",
            lore = "Standard tactical armor built for frontline gate containment.",
            costGold = 600,
            costCrystals = 0,
            iconRes = R.drawable.avatar_hunter,
            isPurchased = false,
            isEquipped = false,
            requiredRank = HunterRank.E,
            statBonus = "+5 Vitality"
        ),
        ShopItem(
            id = "avatar_igris",
            name = "Shadow Knight Vanguard",
            category = ShopCategory.AVATARS,
            description = "Honored blood knight wielding the great longsword.",
            lore = "The first knight to swear unwavering fealty to the Monarch.",
            costGold = 3200,
            costCrystals = 40,
            iconRes = R.drawable.rise,
            isPurchased = false,
            isEquipped = false,
            requiredRank = HunterRank.B,
            statBonus = "+12 Agility"
        ),

        // TITLES
        ShopItem(
            id = "title_weakest",
            name = "The Weakest Hunter",
            category = ShopCategory.TITLES,
            description = "E-Rank survivor who defied fate.",
            lore = "The humble starting moniker before true awakening.",
            costGold = 0,
            costCrystals = 0,
            isPurchased = true,
            isEquipped = true,
            requiredRank = HunterRank.E,
            statBonus = "+5% Motivation"
        ),
        ShopItem(
            id = "title_awakened",
            name = "Awakened Striker",
            category = ShopCategory.TITLES,
            description = "A hunter whose magical core has successfully ignited.",
            lore = "Certified striker verified by the Hunter Association appraisal.",
            costGold = 500,
            costCrystals = 0,
            isPurchased = false,
            isEquipped = false,
            requiredRank = HunterRank.D,
            statBonus = "+3% Gold Drop"
        ),
        ShopItem(
            id = "title_slayer",
            name = "Red Gate Survivor",
            category = ShopCategory.TITLES,
            description = "Survived the isolation of an unranked red gate.",
            lore = "A testament to psychological endurance and boss elimination.",
            costGold = 1600,
            costCrystals = 15,
            isPurchased = false,
            isEquipped = false,
            requiredRank = HunterRank.B,
            statBonus = "+7% Raid XP"
        ),
        ShopItem(
            id = "title_monarch",
            name = "Shadow Monarch",
            category = ShopCategory.TITLES,
            description = "The Supreme Sovereign who commands death.",
            lore = "Even death cannot hold the shadows sworn to this moniker.",
            costGold = 4500,
            costCrystals = 50,
            isPurchased = false,
            isEquipped = false,
            requiredRank = HunterRank.S,
            statBonus = "+15% Focus XP"
        ),
        ShopItem(
            id = "title_national",
            name = "National-Level Hunter",
            category = ShopCategory.TITLES,
            description = "A vessel of unmatched world power.",
            lore = "Designated equal in authority and destructive output to an army.",
            costGold = 8500,
            costCrystals = 100,
            isPurchased = false,
            isEquipped = false,
            requiredRank = HunterRank.NATIONAL,
            statBonus = "+25% All Rewards"
        ),

        // EQUIPMENT & ARTIFACTS
        ShopItem(
            id = "gear_pushup",
            name = "Push-up Training Board",
            category = ShopCategory.EQUIPMENT,
            description = "Increases fitness mission XP by 15%.",
            lore = "Precision ergonomic conditioning board designed for high-rep routines.",
            costGold = 500,
            costCrystals = 0,
            iconRes = R.drawable.shop_pushupboard,
            isPurchased = false,
            isEquipped = false,
            statBonus = "+15% Fitness XP"
        ),
        ShopItem(
            id = "gear_dagger",
            name = "Kasaka's Venom Fang",
            category = ShopCategory.EQUIPMENT,
            description = "Legendary dagger that induces paralytic focus.",
            lore = "A short dagger fashioned from the blue venom gland of Great Kasaka.",
            costGold = 3000,
            costCrystals = 30,
            iconRes = R.drawable.shop_hi,
            isPurchased = false,
            isEquipped = false,
            requiredRank = HunterRank.D,
            statBonus = "+20% Focus Productivity"
        ),
        ShopItem(
            id = "gear_orb",
            name = "Orb of Avarice",
            category = ShopCategory.EQUIPMENT,
            description = "Doubles mana crystal gain from dungeon challenges.",
            lore = "A spell orb recovered from vulcan dungeons that multiplies magical yield.",
            costGold = 4500,
            costCrystals = 60,
            iconRes = R.drawable.shop_mi,
            isPurchased = false,
            isEquipped = false,
            requiredRank = HunterRank.B,
            statBonus = "2x Crystal Yield"
        ),

        // POTIONS (Consumable)
        ShopItem(
            id = "pot_health",
            name = "Full Recovery Elixir",
            category = ShopCategory.POTIONS,
            description = "Instantly restores stamina and grants +75 XP.",
            lore = "Blessed water that heals muscle fatigue and recharges mana channels.",
            costGold = 250,
            costCrystals = 0,
            iconRes = R.drawable.shop_hey,
            isPurchased = false,
            isEquipped = false,
            isConsumable = true,
            statBonus = "+75 XP Instant"
        ),
        ShopItem(
            id = "pot_double",
            name = "Dungeon Double XP Draught",
            category = ShopCategory.POTIONS,
            description = "Instantly triggers deep awakening granting +150 XP.",
            lore = "Distilled essence of concentrated blue gate mana.",
            costGold = 600,
            costCrystals = 5,
            iconRes = R.drawable.shop_re,
            isPurchased = false,
            isEquipped = false,
            isConsumable = true,
            statBonus = "+150 XP Instant"
        )
    )

    private fun getInitialPurchaseHistory(): List<PurchaseRecord> = listOf(
        PurchaseRecord(
            id = "p0",
            itemId = "CYAN",
            itemName = "Cyber Cyan Aura",
            category = ShopCategory.THEMES,
            costGold = 0,
            costCrystals = 0,
            timestamp = System.currentTimeMillis() - 86400000L * 4,
            formattedDate = "4 days ago",
            status = "DEFAULT"
        ),
        PurchaseRecord(
            id = "p1",
            itemId = "frame_cyan",
            itemName = "System Cyan Glow",
            category = ShopCategory.FRAMES,
            costGold = 400,
            costCrystals = 0,
            timestamp = System.currentTimeMillis() - 86400000L * 3,
            formattedDate = "3 days ago",
            status = "ACQUIRED"
        ),
        PurchaseRecord(
            id = "p2",
            itemId = "avatar_arise",
            itemName = "Shadow Monarch Silhouette",
            category = ShopCategory.AVATARS,
            costGold = 800,
            costCrystals = 5,
            timestamp = System.currentTimeMillis() - 86400000L,
            formattedDate = "Yesterday",
            status = "ACQUIRED"
        )
    )

    private fun getInitialGoals(): List<HunterGoal> = listOf(
        HunterGoal(
            id = "g1",
            title = "Awaken to B-Rank Elite Slayer",
            category = "HUNTER ASCENSION",
            targetDate = "14 Days",
            milestones = listOf(
                GoalMilestone("m1", "Reach Hunter Level 30", isCompleted = false),
                GoalMilestone("m2", "Clear 15 Physical Conditioning Quests", isCompleted = true),
                GoalMilestone("m3", "Log 300 minutes in Focus Sanctuary", isCompleted = true),
                GoalMilestone("m4", "Maintain a 7-day streak without break", isCompleted = false)
            ),
            xpReward = 1200,
            isCompleted = false
        ),
        HunterGoal(
            id = "g2",
            title = "Shadow Monarch Army Expansion",
            category = "DISCIPLINE & HABITS",
            targetDate = "30 Days",
            milestones = listOf(
                GoalMilestone("m5", "Complete 50 daily habit protocols", isCompleted = true),
                GoalMilestone("m6", "Accumulate 5,000 dungeon gold", isCompleted = false),
                GoalMilestone("m7", "Acquire kasaka dagger artifact", isCompleted = false)
            ),
            xpReward = 2000,
            isCompleted = false
        ),
        HunterGoal(
            id = "g3",
            title = "Supreme Mental Fortitude Mastery",
            category = "FOCUS PROTOCOL",
            targetDate = "21 Days",
            milestones = listOf(
                GoalMilestone("m8", "Complete 20 deep focus sessions (45m+)", isCompleted = false),
                GoalMilestone("m9", "Clear Red Gate Boss Challenge trial", isCompleted = false),
                GoalMilestone("m10", "Zero missed planner tasks for 5 days", isCompleted = false)
            ),
            xpReward = 1500,
            isCompleted = false
        )
    )

    private fun getInitialActivityCalendar(): Map<String, DayActivityRecord> {
        val map = mutableMapOf<String, DayActivityRecord>()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val days = listOf(
            DayActivityRecord(today, 22, "Thu", 75, 4, 3, 420),
            DayActivityRecord("2026-10-21", 21, "Wed", 90, 5, 4, 510),
            DayActivityRecord("2026-10-20", 20, "Tue", 45, 3, 2, 280),
            DayActivityRecord("2026-10-19", 19, "Mon", 60, 4, 3, 350),
            DayActivityRecord("2026-10-18", 18, "Sun", 120, 5, 5, 680),
            DayActivityRecord("2026-10-17", 17, "Sat", 50, 3, 2, 310),
            DayActivityRecord("2026-10-16", 16, "Fri", 85, 4, 4, 490)
        )
        days.forEach { map[it.dateKey] = it }
        return map
    }

    private fun getInitialSkills(): List<SkillNode> = listOf(
        SkillNode("sk1", "Iron Body (Strength)", "STRENGTH", "Enhances stamina for long endurance workouts.", 2, 5),
        SkillNode("sk2", "Sprint Step (Agility)", "AGILITY", "Quicker response and faster habit execution.", 1, 5),
        SkillNode("sk3", "Ruler's Authority (Focus)", "INTELLECT", "Allows 25+ min focus sessions without fatigue.", 2, 5),
        SkillNode("sk4", "Shadow Extraction", "SHADOW", "Convert completed daily tasks into persistent Shadow soldiers.", 1, 5),
        SkillNode("sk5", "Perception Zone", "SHADOW", "Detect distractions before they disrupt your deep work.", 1, 5)
    )

    private fun getInitialAchievements(): List<AchievementBadge> = listOf(
        AchievementBadge("ac1", "First Awakening", "Completed your initial System mission.", "verified", true, 100, 1, 1),
        AchievementBadge("ac2", "Iron Will", "Maintained a 7-day habit streak.", "local_fire_department", false, 300, 4, 7),
        AchievementBadge("ac3", "Dungeon Conqueror", "Cleared 20 missions total.", "military_tech", false, 500, 8, 20),
        AchievementBadge("ac4", "Shadow General", "Reached Level 25 and C-Rank status.", "shield", false, 800, 1, 25),
        AchievementBadge("ac5", "Deep Focus Monarch", "Logged 500 total minutes in Focus Timer.", "timer", false, 1000, 100, 500)
    )

    private fun getInitialGuildMembers(): List<GuildMember> = listOf(
        GuildMember("g1", "Sung Jinwoo", HunterRank.MONARCH, 100, true, "Shadow Sovereign"),
        GuildMember("g2", "Cha Hae-In", HunterRank.S, 58, true, "Sword Dance"),
        GuildMember("g3", "Choi Jong-In", HunterRank.S, 52, false, "Ultimate Soldier"),
        GuildMember("g4", "Baek Yoonho", HunterRank.S, 50, true, "Beast King"),
        GuildMember("g5", "Woo Jin-Chul", HunterRank.A, 44, true, "Chief Inspector"),
        GuildMember("g6", "Yoo Jinho", HunterRank.D, 18, true, "Vice-Chairman")
    )

    private fun getInitialMessages(): List<CommunityMessage> = listOf(
        CommunityMessage("m1", "Woo Jin-Chul", HunterRank.A, "The Association has issued today's global mission alerts. Hunters, prepare your gears.", "08:15 AM"),
        CommunityMessage("m2", "Cha Hae-In", HunterRank.S, "Completed morning sword conditioning. 10km run cleared.", "08:30 AM"),
        CommunityMessage("m3", "Yoo Jinho", HunterRank.D, "Big brother Jinwoo! Our focus sessions in the study dungeon are already paying off!", "08:42 AM"),
        CommunityMessage("m4", "Baek Yoonho", HunterRank.S, "Don't slack off on pushups. High-rank beasts will test every fiber of your muscle.", "08:50 AM")
    )

    private fun getInitialNotifications(): List<NotificationItem> = listOf(
        NotificationItem("n1", "Daily Mission Reset", "Your daily quests are refreshed. Complete them to maintain your 1.2x streak bonus.", "MISSION", "Today"),
        NotificationItem("n2", "Guild Message Received", "Woo Jin-Chul posted in the Hunter Association channel.", "GUILD", "1h ago"),
        NotificationItem("n3", "Level Up Available", "You have stat points ready to allocate in your hunter profile.", "LEVELUP", "2h ago")
    )

    private fun getInitialActivityLogs(): List<ActivityLog> = listOf(
        ActivityLog("a1", "XP Acquired: +120", "Physical Quest: 100 Push-ups", 120, "15m ago"),
        ActivityLog("a2", "Habit Completed: Hydration (3L Water)", "Daily Protocol", 40, "1h ago"),
        ActivityLog("a3", "Focus Session Completed: 45 min", "Deep Algorithm Practice", 225, "3h ago"),
        ActivityLog("a4", "Daily Reward Claimed", "Login Protocol (+300 Gold)", 150, "Yesterday")
    )
}
