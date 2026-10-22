package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HunterChatRepository
import com.example.data.HunterRepository
import com.example.ui.components.LevelUpDialog
import com.example.ui.screens.*
import com.example.ui.theme.*

enum class AppScreen(val label: String) {
    HOME("Home"),
    MISSIONS("Quests"),
    PRODUCTIVITY("Focus"),
    COMMUNITY("Guild"),
    SHOP("Shop"),
    PROFILE("Profile"),
    NOTIFICATIONS("Alerts"),
    SETTINGS("Config")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize persistent repository with context
        HunterRepository.init(applicationContext)

        setContent {
            val onboardingCompleted by HunterRepository.onboardingCompleted.collectAsState()
            val isLoggedIn by HunterRepository.isLoggedIn.collectAsState()
            val currentUserEmail by HunterRepository.currentUserEmail.collectAsState()

            val profile by HunterRepository.profile.collectAsState()
            val missions by HunterRepository.missions.collectAsState()
            val habits by HunterRepository.habits.collectAsState()
            val tasks by HunterRepository.tasks.collectAsState()
            val focusRecords by HunterRepository.focusRecords.collectAsState()
            val shopItems by HunterRepository.shopItems.collectAsState()
            val skills by HunterRepository.skills.collectAsState()
            val achievements by HunterRepository.achievements.collectAsState()
            val guildMembers by HunterRepository.guildMembers.collectAsState()
            val messages by HunterRepository.messages.collectAsState()
            val notifications by HunterRepository.notifications.collectAsState()
            val activityLogs by HunterRepository.activityLogs.collectAsState()
            val pendingLevelUp by HunterRepository.pendingLevelUp.collectAsState()
            val purchaseHistory by HunterRepository.purchaseHistory.collectAsState()
            val focusTimerState by HunterRepository.focusTimerState.collectAsState()
            val goals by HunterRepository.goals.collectAsState()
            val activityCalendar by HunterRepository.activityCalendar.collectAsState()
            val reminders by HunterRepository.reminders.collectAsState()

            val accentColor = when (profile.equippedTheme) {
                "VIOLET" -> MonarchPurple
                "CRIMSON" -> CrimsonPrimary
                "GOLD" -> GoldPrimary
                else -> CyanPrimary
            }

            val chatRepository = remember { HunterChatRepository(applicationContext) }

            LaunchedEffect(Unit) {
                com.example.data.HunterAuthManager.attemptAutoSignIn(applicationContext)
            }

            LaunchedEffect(isLoggedIn, profile.name, profile.rank, profile.level, profile.showOnlineStatus) {
                if (isLoggedIn) {
                    chatRepository.updatePresence(
                        name = profile.name,
                        rank = profile.rank.title,
                        level = profile.level,
                        isOnline = true,
                        showOnlineStatus = profile.showOnlineStatus
                    )
                }
            }

            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        HunterRepository.checkAndCompleteElapsedTimer()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
            var authInitialRegister by remember { mutableStateOf(false) }

            BackHandler(enabled = currentScreen != AppScreen.HOME && isLoggedIn && onboardingCompleted) {
                currentScreen = AppScreen.HOME
            }

            AriseTheme(accentColor = accentColor) {
                when {
                    // 1. First-Launch Onboarding
                    !onboardingCompleted -> {
                        OnboardingScreen(
                            onFinish = { toRegister ->
                                HunterRepository.setOnboardingCompleted(true)
                                authInitialRegister = toRegister
                            }
                        )
                    }

                    // 2. Authentication Flow
                    !isLoggedIn -> {
                        AuthScreen(
                            initialIsRegister = authInitialRegister,
                            onAuthSuccess = {
                                currentScreen = AppScreen.HOME
                            }
                        )
                    }

                    // 3. Main Application Dashboard
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(SystemBackground)
                        ) {
                            // Wallpaper Backdrop
                            when (profile.wallpaper) {
                                "bg_tuff" -> {
                                    Image(
                                        painter = painterResource(id = R.drawable.bg_tuff),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        alpha = 0.28f
                                    )
                                }
                                "bg_violet" -> {
                                    Image(
                                        painter = painterResource(id = R.drawable.bg_violet),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        alpha = 0.32f
                                    )
                                }
                                else -> {
                                    // Deep Void
                                }
                            }

                            // Scrim overlay for crisp reading
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                SystemBackground.copy(alpha = 0.85f),
                                                SystemBackground.copy(alpha = 0.95f)
                                            )
                                        )
                                    )
                            )

                            Scaffold(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding(),
                                containerColor = Color.Transparent,
                                bottomBar = {
                                    if (currentScreen != AppScreen.NOTIFICATIONS) {
                                        NavigationBar(
                                            modifier = Modifier.navigationBarsPadding(),
                                            containerColor = SystemSurface.copy(alpha = 0.95f),
                                            tonalElevation = 8.dp
                                        ) {
                                            val navItems = listOf(
                                                Triple(AppScreen.HOME, Icons.Default.Dashboard, "Home"),
                                                Triple(AppScreen.MISSIONS, Icons.Default.FitnessCenter, "Quests"),
                                                Triple(AppScreen.PRODUCTIVITY, Icons.Default.Timer, "Focus"),
                                                Triple(AppScreen.COMMUNITY, Icons.Default.Groups, "Guild"),
                                                Triple(AppScreen.SHOP, Icons.Default.ShoppingBag, "Shop"),
                                                Triple(AppScreen.PROFILE, Icons.Default.Person, "Profile"),
                                                Triple(AppScreen.SETTINGS, Icons.Default.Settings, "Config")
                                            )

                                            navItems.forEach { (screen, icon, label) ->
                                                val isSelected = currentScreen == screen
                                                NavigationBarItem(
                                                    selected = isSelected,
                                                    onClick = { currentScreen = screen },
                                                    icon = {
                                                        Icon(
                                                            imageVector = icon,
                                                            contentDescription = label,
                                                            tint = if (isSelected) accentColor else TextMuted
                                                        )
                                                    },
                                                    label = {
                                                        Text(
                                                            text = label,
                                                            color = if (isSelected) accentColor else TextMuted,
                                                            fontSize = 10.sp
                                                        )
                                                    },
                                                    colors = NavigationBarItemDefaults.colors(
                                                        indicatorColor = accentColor.copy(alpha = 0.2f)
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    when (currentScreen) {
                                        AppScreen.HOME -> HomeScreen(
                                            profile = profile,
                                            missions = missions,
                                            activityLogs = activityLogs,
                                            achievements = achievements,
                                            onCompleteMission = { HunterRepository.completeMission(it) },
                                            onNavigateToMissions = { currentScreen = AppScreen.MISSIONS },
                                            onNavigateToProductivity = { currentScreen = AppScreen.PRODUCTIVITY },
                                            onNavigateToCommunity = { currentScreen = AppScreen.COMMUNITY },
                                            onNavigateToShop = { currentScreen = AppScreen.SHOP },
                                            onOpenProfile = { currentScreen = AppScreen.PROFILE },
                                            onOpenNotifications = { currentScreen = AppScreen.NOTIFICATIONS }
                                        )

                                        AppScreen.MISSIONS -> MissionsScreen(
                                            missions = missions,
                                            onCompleteMission = { HunterRepository.completeMission(it) }
                                        )

                                        AppScreen.PRODUCTIVITY -> ProductivityScreen(
                                            habits = habits,
                                            tasks = tasks,
                                            focusRecords = focusRecords,
                                            focusTimerState = focusTimerState,
                                            goals = goals,
                                            activityCalendar = activityCalendar,
                                            reminders = reminders,
                                            onToggleHabit = { HunterRepository.toggleHabit(it) },
                                            onAddHabit = { title, days -> HunterRepository.addHabit(title, days) },
                                            onDeleteHabit = { HunterRepository.deleteHabit(it) },
                                            onToggleTask = { HunterRepository.toggleTask(it) },
                                            onAddTask = { title, priority, deadline ->
                                                HunterRepository.addTask(title, priority, deadline)
                                            },
                                            onDeleteTask = { HunterRepository.deleteTask(it) },
                                            onStartTimer = { mins, title, isBreak ->
                                                HunterRepository.startFocusTimer(mins, title, isBreak)
                                            },
                                            onPauseTimer = { HunterRepository.pauseFocusTimer() },
                                            onResumeTimer = { HunterRepository.resumeFocusTimer() },
                                            onResetTimer = { HunterRepository.resetFocusTimer() },
                                            onCheckTimerElapsed = { HunterRepository.checkAndCompleteElapsedTimer() },
                                            onAddGoal = { title, cat, date, ms, xp ->
                                                HunterRepository.addGoal(title, cat, date, ms, xp)
                                            },
                                            onToggleMilestone = { gId, mId ->
                                                HunterRepository.toggleMilestone(gId, mId)
                                            },
                                            onDeleteGoal = { HunterRepository.deleteGoal(it) },
                                            onToggleReminder = { HunterRepository.toggleReminder(it) }
                                        )

                                        AppScreen.COMMUNITY -> SocialScreen(
                                            chatRepository = chatRepository,
                                            currentProfile = profile,
                                            guildMembers = guildMembers
                                        )

                                        AppScreen.SHOP -> ShopScreen(
                                            profile = profile,
                                            shopItems = shopItems,
                                            purchaseHistory = purchaseHistory,
                                            onBuyItem = { HunterRepository.buyShopItem(it) },
                                            onEquipItem = { HunterRepository.equipShopItem(it) },
                                            onUnequipItem = { HunterRepository.unequipShopItem(it) }
                                        )

                                        AppScreen.PROFILE -> ProfileScreen(
                                            profile = profile,
                                            skills = skills,
                                            achievements = achievements,
                                            onAllocateStat = { HunterRepository.allocateStat(it) },
                                            onUpgradeSkill = { HunterRepository.upgradeSkill(it) },
                                            onPrestige = { HunterRepository.prestigeAwaken() },
                                            onSaveProfile = { name, user, title, bio ->
                                                HunterRepository.updateProfile(name, user, title, bio)
                                            },
                                            onSelectAvatar = { HunterRepository.updateAvatar(it) },
                                            onTogglePrivacy = { HunterRepository.toggleProfilePrivacy() }
                                        )

                                        AppScreen.NOTIFICATIONS -> NotificationsScreen(
                                            notifications = notifications,
                                            onMarkAllRead = { HunterRepository.markNotificationsRead() },
                                            onBack = { currentScreen = AppScreen.HOME }
                                        )

                                        AppScreen.SETTINGS -> SettingsScreen(
                                            profile = profile,
                                            userEmail = currentUserEmail,
                                            onSetTheme = { HunterRepository.setAccentColor(it) },
                                            onSetWallpaper = { HunterRepository.setWallpaper(it) },
                                            onToggleVip = { HunterRepository.toggleVip() },
                                            onReplayOnboarding = {
                                                HunterRepository.resetOnboarding()
                                            },
                                            onSignOut = {
                                                HunterRepository.signOut()
                                            }
                                        )
                                    }
                                }
                            }

                            // Level Up Dialog Banner
                            if (pendingLevelUp != null) {
                                LevelUpDialog(
                                    newLevel = pendingLevelUp ?: 1,
                                    onDismiss = { HunterRepository.dismissLevelUp() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
