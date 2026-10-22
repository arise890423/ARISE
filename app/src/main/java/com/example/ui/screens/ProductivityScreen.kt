package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.ui.components.SystemWindowCard
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProductivityScreen(
    habits: List<ProductivityHabit>,
    tasks: List<TodoTask>,
    focusRecords: List<FocusRecord>,
    focusTimerState: FocusTimerState,
    goals: List<HunterGoal>,
    activityCalendar: Map<String, DayActivityRecord>,
    reminders: ProductivityReminders,
    onToggleHabit: (String) -> Unit,
    onAddHabit: (String, Int) -> Unit,
    onDeleteHabit: (String) -> Unit,
    onToggleTask: (String) -> Unit,
    onAddTask: (String, String, String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onStartTimer: (Int, String, Boolean) -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onCheckTimerElapsed: () -> Boolean,
    onAddGoal: (String, String, String, List<String>, Int) -> Unit,
    onToggleMilestone: (String, String) -> Unit,
    onDeleteGoal: (String) -> Unit,
    onToggleReminder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    var subTab by remember { mutableStateOf(0) } // 0: Focus, 1: Habits, 2: Planner, 3: Goals, 4: Analytics & Calendar

    var showAddHabitDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("productivity_screen_list"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // Top Navigation SubTabs
        item {
            ScrollableTabRow(
                selectedTabIndex = subTab,
                containerColor = Color.Transparent,
                contentColor = accent,
                edgePadding = 0.dp,
                divider = {}
            ) {
                val tabs = listOf(
                    Triple(0, "Focus Sanctuary", Icons.Default.Timer),
                    Triple(1, "Daily Habits", Icons.Default.FitnessCenter),
                    Triple(2, "Task Planner", Icons.Default.Checklist),
                    Triple(3, "Hunter Goals", Icons.Default.Flag),
                    Triple(4, "Analytics & Logs", Icons.Default.BarChart)
                )

                tabs.forEach { (index, title, icon) ->
                    val isSelected = subTab == index
                    Tab(
                        selected = isSelected,
                        onClick = { subTab = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) accent else TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                        color = if (isSelected) accent else TextSecondary
                                    )
                                )
                            }
                        },
                        modifier = Modifier.testTag("subtab_$index")
                    )
                }
            }
        }

        when (subTab) {
            0 -> {
                // Focus Sanctuary (Timer)
                item {
                    FocusSanctuarySection(
                        timerState = focusTimerState,
                        onStart = onStartTimer,
                        onPause = onPauseTimer,
                        onResume = onResumeTimer,
                        onReset = onResetTimer,
                        onCheckElapsed = onCheckTimerElapsed
                    )
                }

                item {
                    Text(
                        text = "RECENT FOCUS EXPEDITIONS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                    )
                }

                if (focusRecords.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = SystemSurface,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SystemCardBorder)
                        ) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No focus records yet. Complete your first session above to harvest mana and XP.",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(focusRecords, key = { it.id }) { record ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = SystemSurface,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SystemCardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = record.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${record.minutes} min session • ${record.date}",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Surface(
                                    color = ManaBlue.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, ManaBlue.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "+${record.xpGained} XP",
                                        color = ManaBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Daily Habits Tracker
                item {
                    HabitsSummaryCard(habits = habits, onOpenAdd = { showAddHabitDialog = true })
                }

                item {
                    Text(
                        text = "ACTIVE REPETITION PROTOCOLS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                    )
                }

                if (habits.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = SystemSurface,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SystemCardBorder)
                        ) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No active habit protocols. Tap the '+' button above to establish daily routines.",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(habits, key = { it.id }) { habit ->
                        HabitItemCard(
                            habit = habit,
                            onToggle = { onToggleHabit(habit.id) },
                            onDelete = { onDeleteHabit(habit.id) }
                        )
                    }
                }
            }

            2 -> {
                // Task Planner & Daily To-Dos
                item {
                    PlannerSummaryCard(tasks = tasks, onOpenAdd = { showAddTaskDialog = true })
                }

                item {
                    Text(
                        text = "DUNGEON OBJECTIVES & COMMANDS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                    )
                }

                if (tasks.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = SystemSurface,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SystemCardBorder)
                        ) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No pending commands. Tap the '+' icon above to create to-do tasks.",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(tasks, key = { it.id }) { task ->
                        TaskItemCard(
                            task = task,
                            onToggle = { onToggleTask(task.id) },
                            onDelete = { onDeleteTask(task.id) }
                        )
                    }
                }
            }

            3 -> {
                // Long-Term Hunter Goals
                item {
                    GoalsHeaderCard(goals = goals, onOpenAdd = { showAddGoalDialog = true })
                }

                item {
                    Text(
                        text = "STRATEGIC OBJECTIVES & MILESTONES",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                    )
                }

                if (goals.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = SystemSurface,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SystemCardBorder)
                        ) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No long-term goals declared yet. Establish targets to guide your awakening.",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(goals, key = { it.id }) { goal ->
                        GoalItemCard(
                            goal = goal,
                            onToggleMilestone = { mId -> onToggleMilestone(goal.id, mId) },
                            onDelete = { onDeleteGoal(goal.id) }
                        )
                    }
                }
            }

            4 -> {
                // Analytics, Calendar & Reminders
                item {
                    ProductivityStatsSection(
                        focusRecords = focusRecords,
                        habits = habits,
                        tasks = tasks,
                        goals = goals
                    )
                }

                item {
                    WeeklyProgressChartCard(focusRecords = focusRecords)
                }

                item {
                    ActivityCalendarCard(calendarMap = activityCalendar)
                }

                item {
                    RemindersConfigCard(
                        reminders = reminders,
                        onToggle = onToggleReminder
                    )
                }
            }
        }
    }

    if (showAddHabitDialog) {
        AddHabitDialog(
            onDismiss = { showAddHabitDialog = false },
            onAdd = { title, days ->
                onAddHabit(title, days)
                showAddHabitDialog = false
            }
        )
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onAdd = { title, priority, deadline ->
                onAddTask(title, priority, deadline)
                showAddTaskDialog = false
            }
        )
    }

    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onAdd = { title, category, date, milestones, xp ->
                onAddGoal(title, category, date, milestones, xp)
                showAddGoalDialog = false
            }
        )
    }
}

// ==========================================
// 1. FOCUS SANCTUARY (LIFECYCLE-PERSISTENT TIMER)
// ==========================================

@Composable
fun FocusSanctuarySection(
    timerState: FocusTimerState,
    onStart: (Int, String, Boolean) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onCheckElapsed: () -> Boolean,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    var isBreakMode by remember { mutableStateOf(timerState.isBreakMode) }
    var selectedMinutes by remember { mutableStateOf(timerState.selectedMinutes) }
    var customTitle by remember { mutableStateOf(timerState.sessionTitle) }

    // Live clock ticker
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(timerState.isRunning) {
        while (timerState.isRunning) {
            currentTimeMs = System.currentTimeMillis()
            if (timerState.endTimestampMs > 0 && currentTimeMs >= timerState.endTimestampMs) {
                onCheckElapsed()
                break
            }
            delay(500L)
        }
    }

    // Dynamic calculation of remaining time based on actual clock timestamps
    val remainingMs = when {
        timerState.isRunning -> (timerState.endTimestampMs - currentTimeMs).coerceAtLeast(0L)
        timerState.isPaused -> timerState.remainingPausedMs
        else -> timerState.durationMs
    }

    val remainingSecs = (remainingMs / 1000).toInt()
    val minutesLeft = remainingSecs / 60
    val secondsLeft = remainingSecs % 60
    val totalMs = if (timerState.durationMs > 0) timerState.durationMs else (selectedMinutes * 60 * 1000L)
    val progress = ((totalMs - remainingMs).toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)

    SystemWindowCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = if (timerState.isBreakMode) SuccessGreen.copy(alpha = 0.5f) else ManaBlue.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (timerState.isBreakMode) "SYSTEM RECOVERY CHAMBER" else "SYSTEM FOCUS SANCTUARY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = if (timerState.isBreakMode) SuccessGreen else ManaBlue,
                        letterSpacing = 1.5.sp
                    )
                )
                Surface(
                    color = (if (timerState.isBreakMode) SuccessGreen else ManaBlue).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (timerState.isRunning) "RUNNING" else if (timerState.isPaused) "PAUSED" else "IDLE",
                        color = if (timerState.isRunning) (if (timerState.isBreakMode) SuccessGreen else ManaBlue) else if (timerState.isPaused) WarningYellow else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Focus vs Break mode selector (when not running)
            if (!timerState.isRunning && !timerState.isPaused) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                isBreakMode = false
                                selectedMinutes = 25
                                customTitle = "Deep Dungeon Focus"
                            },
                        color = if (!isBreakMode) ManaBlue.copy(alpha = 0.2f) else SystemSurfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (!isBreakMode) ManaBlue else SystemCardBorder)
                    ) {
                        Text(
                            text = "Focus Mode",
                            color = if (!isBreakMode) ManaBlue else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                isBreakMode = true
                                selectedMinutes = 5
                                customTitle = "Hunter Mana Rest"
                            },
                        color = if (isBreakMode) SuccessGreen.copy(alpha = 0.2f) else SystemSurfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (isBreakMode) SuccessGreen else SystemCardBorder)
                    ) {
                        Text(
                            text = "Break Mode",
                            color = if (isBreakMode) SuccessGreen else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Presets
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val presets = if (isBreakMode) listOf(5, 10, 15, 20) else listOf(15, 25, 45, 60)
                    presets.forEach { mins ->
                        val isSel = selectedMinutes == mins
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedMinutes = mins },
                            color = if (isSel) (if (isBreakMode) SuccessGreen else ManaBlue).copy(alpha = 0.2f) else SystemSurfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, if (isSel) (if (isBreakMode) SuccessGreen else ManaBlue) else SystemCardBorder)
                        ) {
                            Text(
                                text = "${mins}m",
                                color = if (isSel) (if (isBreakMode) SuccessGreen else ManaBlue) else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = customTitle,
                    onValueChange = { customTitle = it },
                    label = { Text("Session Objective", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isBreakMode) SuccessGreen else ManaBlue,
                        unfocusedBorderColor = SystemCardBorder
                    )
                )
            } else {
                Text(
                    text = "\"${timerState.sessionTitle}\"",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Circular Visual Timer
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .border(
                        BorderStroke(
                            4.dp,
                            (if (timerState.isBreakMode) SuccessGreen else ManaBlue).copy(alpha = 0.35f)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%02d:%02d", minutesLeft, secondsLeft),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (timerState.isRunning) "ACTIVE EXPEDITION" else if (timerState.isPaused) "PAUSED" else "STANDBY",
                        color = if (timerState.isRunning) (if (timerState.isBreakMode) SuccessGreen else ManaBlue) else if (timerState.isPaused) WarningYellow else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    val sessionMins = if (timerState.durationMs > 0) (timerState.durationMs / 60000L).toInt() else selectedMinutes
                    val rewardXp = sessionMins * 5
                    Text(
                        text = "Reward: +$rewardXp XP",
                        color = GoldGlow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when {
                    timerState.isRunning -> {
                        Button(
                            onClick = onPause,
                            modifier = Modifier.weight(1f).testTag("pause_timer_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WarningYellow,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PAUSE", fontWeight = FontWeight.Black)
                        }

                        OutlinedButton(
                            onClick = onReset,
                            modifier = Modifier.weight(1f).testTag("cancel_timer_button"),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SystemCardBorder)
                        ) {
                            Text("ABORT", color = CrimsonPrimary)
                        }
                    }

                    timerState.isPaused -> {
                        Button(
                            onClick = onResume,
                            modifier = Modifier.weight(1f).testTag("resume_timer_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ManaBlue,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RESUME", fontWeight = FontWeight.Black)
                        }

                        OutlinedButton(
                            onClick = onReset,
                            modifier = Modifier.weight(1f).testTag("reset_timer_button"),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SystemCardBorder)
                        ) {
                            Text("RESET", color = TextSecondary)
                        }
                    }

                    else -> {
                        Button(
                            onClick = {
                                onStart(selectedMinutes, customTitle, isBreakMode)
                            },
                            modifier = Modifier.fillMaxWidth().testTag("start_timer_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBreakMode) SuccessGreen else ManaBlue,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBreakMode) "COMMENCE RECOVERY ($selectedMinutes MIN)" else "COMMENCE FOCUS ($selectedMinutes MIN)",
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. HABITS TRACKER COMPONENT
// ==========================================

@Composable
fun HabitsSummaryCard(habits: List<ProductivityHabit>, onOpenAdd: () -> Unit) {
    val completedCount = habits.count { it.isDoneToday }
    val total = habits.size
    val pct = if (total > 0) (completedCount.toFloat() / total) else 0f

    SystemWindowCard(borderColor = SuccessGreen.copy(alpha = 0.4f)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DAILY REPETITION STATUS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = SuccessGreen
                        )
                    )
                    Text(
                        text = "$completedCount of $total protocols cleared today",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                IconButton(
                    onClick = onOpenAdd,
                    modifier = Modifier.testTag("add_habit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Add Habit",
                        tint = SuccessGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { pct },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = SuccessGreen,
                trackColor = SystemSurfaceVariant
            )
        }
    }
}

@Composable
fun HabitItemCard(
    habit: ProductivityHabit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("habit_card_${habit.id}"),
        color = if (habit.isDoneToday) SystemSurface.copy(alpha = 0.6f) else SystemSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (habit.isDoneToday) SuccessGreen.copy(alpha = 0.5f) else SystemCardBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = habit.isDoneToday,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = SuccessGreen,
                        checkmarkColor = Color.Black
                    ),
                    modifier = Modifier.testTag("habit_checkbox_${habit.id}")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (habit.isDoneToday) TextMuted else Color.White,
                            textDecoration = if (habit.isDoneToday) TextDecoration.LineThrough else TextDecoration.None
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🔥", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${habit.streak} day streak",
                            color = WarningYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Target: ${habit.targetDaysAWeek}d/wk",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (habit.isDoneToday) SuccessGreen.copy(alpha = 0.15f) else SystemSurfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (habit.isDoneToday) "+40 XP" else "READY",
                        color = if (habit.isDoneToday) SuccessGreen else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp).testTag("delete_habit_${habit.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// 3. TASK PLANNER COMPONENT
// ==========================================

@Composable
fun PlannerSummaryCard(tasks: List<TodoTask>, onOpenAdd: () -> Unit) {
    val completedCount = tasks.count { it.isCompleted }
    val total = tasks.size
    val accent = MaterialTheme.colorScheme.primary

    SystemWindowCard(borderColor = accent.copy(alpha = 0.4f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "COMMAND DISPATCH LIST",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = accent
                    )
                )
                Text(
                    text = "$completedCount of $total missions executed",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            IconButton(
                onClick = onOpenAdd,
                modifier = Modifier.testTag("add_task_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Task",
                    tint = accent,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun TaskItemCard(
    task: TodoTask,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val priorityColor = when (task.priority) {
        "High" -> CrimsonPrimary
        "Medium" -> WarningYellow
        else -> CyanPrimary
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("task_card_${task.id}"),
        color = if (task.isCompleted) SystemSurface.copy(alpha = 0.6f) else SystemSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (task.isCompleted) accent.copy(alpha = 0.4f) else SystemCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = accent),
                modifier = Modifier.testTag("task_checkbox_${task.id}")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (task.isCompleted) TextMuted else Color.White,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = priorityColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = "${task.priority.uppercase()} PRIORITY",
                            color = priorityColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Text(
                        text = "• Due: ${task.deadline}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp).testTag("delete_task_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ==========================================
// 4. HUNTER GOALS & MILESTONES COMPONENT
// ==========================================

@Composable
fun GoalsHeaderCard(goals: List<HunterGoal>, onOpenAdd: () -> Unit) {
    val completedCount = goals.count { it.isCompleted }
    val total = goals.size

    SystemWindowCard(borderColor = GoldPrimary.copy(alpha = 0.4f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "HUNTER CAMPAIGN TARGETS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = GoldGlow
                    )
                )
                Text(
                    text = "$completedCount of $total major campaigns achieved",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            IconButton(
                onClick = onOpenAdd,
                modifier = Modifier.testTag("add_goal_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AddBox,
                    contentDescription = "Add Goal",
                    tint = GoldGlow,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun GoalItemCard(
    goal: HunterGoal,
    onToggleMilestone: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completedMilestones = goal.milestones.count { it.isCompleted }
    val totalMilestones = goal.milestones.size
    val progress = if (totalMilestones > 0) completedMilestones.toFloat() / totalMilestones else 0f
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("goal_card_${goal.id}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SystemSurface),
        border = BorderStroke(
            1.dp,
            if (goal.isCompleted) SuccessGreen.copy(alpha = 0.6f) else SystemCardBorder
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = if (goal.isCompleted) SuccessGreen.copy(alpha = 0.2f) else GoldPrimary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = goal.category.uppercase(),
                            color = if (goal.isCompleted) SuccessGreen else GoldGlow,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp).testTag("delete_goal_${goal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Milestones: $completedMilestones / $totalMilestones",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = "Deadline: ${goal.targetDate}",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (goal.isCompleted) SuccessGreen else GoldPrimary,
                trackColor = SystemSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reward: +${goal.xpReward} XP",
                    color = GoldGlow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (expanded) "Hide Steps" else "Show Milestones",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Expanded Milestone Checklist
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HorizontalDivider(color = SystemCardBorder, modifier = Modifier.padding(vertical = 4.dp))
                    goal.milestones.forEach { milestone ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleMilestone(milestone.id) }
                                .padding(vertical = 4.dp)
                                .testTag("milestone_${milestone.id}")
                        ) {
                            Checkbox(
                                checked = milestone.isCompleted,
                                onCheckedChange = { onToggleMilestone(milestone.id) },
                                colors = CheckboxDefaults.colors(checkedColor = SuccessGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = milestone.title,
                                color = if (milestone.isCompleted) TextMuted else Color.White,
                                fontSize = 12.sp,
                                textDecoration = if (milestone.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. ANALYTICS, PROGRESS CHARTS & CALENDAR
// ==========================================

@Composable
fun ProductivityStatsSection(
    focusRecords: List<FocusRecord>,
    habits: List<ProductivityHabit>,
    tasks: List<TodoTask>,
    goals: List<HunterGoal>
) {
    val totalFocusMins = focusRecords.sumOf { it.minutes }
    val completedHabits = habits.count { it.isDoneToday }
    val totalHabits = habits.size
    val completedTasks = tasks.count { it.isCompleted }
    val completedGoals = goals.count { it.isCompleted }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "HUNTER PRODUCTIVITY METRICS",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SystemWindowCard(
                modifier = Modifier.weight(1f),
                borderColor = ManaBlue.copy(alpha = 0.35f)
            ) {
                Text(text = "TOTAL FOCUS", color = TextSecondary, fontSize = 10.sp)
                Text(
                    text = "${totalFocusMins}m",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = ManaBlue
                    )
                )
                Text(text = "${focusRecords.size} sessions cleared", color = TextMuted, fontSize = 10.sp)
            }

            SystemWindowCard(
                modifier = Modifier.weight(1f),
                borderColor = SuccessGreen.copy(alpha = 0.35f)
            ) {
                Text(text = "HABIT PROTOCOL", color = TextSecondary, fontSize = 10.sp)
                Text(
                    text = "$completedHabits / $totalHabits",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = SuccessGreen
                    )
                )
                val rate = if (totalHabits > 0) ((completedHabits * 100) / totalHabits) else 0
                Text(text = "$rate% Daily Completion", color = TextMuted, fontSize = 10.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SystemWindowCard(
                modifier = Modifier.weight(1f),
                borderColor = WarningYellow.copy(alpha = 0.35f)
            ) {
                Text(text = "COMMANDS EXECUTED", color = TextSecondary, fontSize = 10.sp)
                Text(
                    text = "$completedTasks / ${tasks.size}",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = WarningYellow
                    )
                )
                Text(text = "Tasks accomplished", color = TextMuted, fontSize = 10.sp)
            }

            SystemWindowCard(
                modifier = Modifier.weight(1f),
                borderColor = GoldPrimary.copy(alpha = 0.35f)
            ) {
                Text(text = "CAMPAIGNS WON", color = TextSecondary, fontSize = 10.sp)
                Text(
                    text = "$completedGoals",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = GoldGlow
                    )
                )
                Text(text = "Goals accomplished", color = TextMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun WeeklyProgressChartCard(focusRecords: List<FocusRecord>) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    // Values in minutes
    val dayMinutes = listOf(35, 60, 45, 90, 75, 50, 80)
    val maxMins = 100f
    val accent = MaterialTheme.colorScheme.primary

    SystemWindowCard(borderColor = accent.copy(alpha = 0.4f)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WEEKLY EXPEDITION MANA CHART",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                )
                Text(
                    text = "Avg: 62 min/day",
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bar Chart Representation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                days.forEachIndexed { i, day ->
                    val mins = dayMinutes[i]
                    val fraction = (mins / maxMins).coerceIn(0.1f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${mins}m",
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(accent, accent.copy(alpha = 0.4f))
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = day,
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityCalendarCard(calendarMap: Map<String, DayActivityRecord>) {
    var selectedDateKey by remember {
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        mutableStateOf(todayKey)
    }

    val selectedRecord = calendarMap[selectedDateKey] ?: DayActivityRecord(
        dateKey = selectedDateKey,
        dayOfMonth = 22,
        dayOfWeek = "Wed",
        focusMinutes = 50,
        habitsCompleted = 3,
        tasksCompleted = 4,
        xpEarned = 250
    )

    SystemWindowCard(borderColor = ManaBlue.copy(alpha = 0.4f)) {
        Column {
            Text(
                text = "OCTOBER 2026 ACTIVITY MATRIX",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            )
            Text(
                text = "Tap a day cell to inspect field deployment telemetry.",
                color = TextSecondary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Calendar Grid for October (31 days)
            // Day of week labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { d ->
                    Text(
                        text = d,
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Render 4 weeks sample (Days 1 to 28)
            val daysList = (1..28).toList()
            val chunkedDays = daysList.chunked(7)

            chunkedDays.forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    week.forEach { dayNum ->
                        val dateKey = "2026-10-${if (dayNum < 10) "0$dayNum" else "$dayNum"}"
                        val isSelected = selectedDateKey == dateKey
                        val record = calendarMap[dateKey]
                        val hasActivity = record != null && (record.focusMinutes > 0 || record.habitsCompleted > 0)

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        isSelected -> ManaBlue.copy(alpha = 0.35f)
                                        hasActivity -> SuccessGreen.copy(alpha = 0.15f)
                                        else -> SystemSurfaceVariant
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) ManaBlue else if (hasActivity) SuccessGreen.copy(alpha = 0.3f) else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedDateKey = dateKey }
                                .testTag("calendar_day_$dayNum"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$dayNum",
                                    color = if (isSelected) Color.White else if (hasActivity) SuccessGreen else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected || hasActivity) FontWeight.Bold else FontWeight.Normal
                                )
                                if (hasActivity) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(SuccessGreen)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Day Detail Inspector Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SystemSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SystemCardBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "FIELD DEPLOYMENT: $selectedDateKey",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Focus: ${selectedRecord.focusMinutes} min", color = ManaBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Habits: ${selectedRecord.habitsCompleted}", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Tasks: ${selectedRecord.tasksCompleted}", color = WarningYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = "+${selectedRecord.xpEarned} XP", color = GoldGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RemindersConfigCard(
    reminders: ProductivityReminders,
    onToggle: (String) -> Unit
) {
    SystemWindowCard(borderColor = WarningYellow.copy(alpha = 0.4f)) {
        Column {
            Text(
                text = "HUNTER PROTOCOL LOCAL REMINDERS",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = WarningYellow
                )
            )
            Text(
                text = "System alerts configured for tactical wakefulness.",
                color = TextSecondary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            listOf(
                Triple("daily", "Daily Awakening Alert (08:00 AM)", reminders.dailyProtocol),
                Triple("focus", "Focus Sanctuary Block (02:00 PM)", reminders.focusSanctuary),
                Triple("evening", "Night Mission Debrief (09:00 PM)", reminders.eveningReview),
                Triple("reset", "Gate Daily Reset Warning (Midnight)", reminders.gateResetAlert)
            ).forEach { (key, label, enabled) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = { onToggle(key) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = WarningYellow
                        ),
                        modifier = Modifier.testTag("reminder_switch_$key")
                    )
                }
            }
        }
    }
}

// ==========================================
// 6. DIALOGS
// ==========================================

@Composable
fun AddHabitDialog(onDismiss: () -> Unit, onAdd: (String, Int) -> Unit) {
    var title by remember { mutableStateOf("") }
    var targetDays by remember { mutableIntStateOf(7) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SystemSurface),
            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "ESTABLISH DAILY PROTOCOL",
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Protocol Name (e.g. 100 Pushups)") },
                    modifier = Modifier.fillMaxWidth().testTag("add_habit_title_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "Target Frequency: $targetDays days/week", color = TextSecondary, fontSize = 11.sp)
                Slider(
                    value = targetDays.toFloat(),
                    onValueChange = { targetDays = it.toInt() },
                    valueRange = 1f..7f,
                    steps = 5,
                    colors = SliderDefaults.colors(thumbColor = SuccessGreen, activeTrackColor = SuccessGreen)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("CANCEL", color = TextSecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (title.isNotBlank()) onAdd(title, targetDays) },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        modifier = Modifier.testTag("confirm_add_habit_button")
                    ) {
                        Text("ESTABLISH", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }
    var deadline by remember { mutableStateOf("Today") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SystemSurface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "DISPATCH NEW COMMAND",
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Objective details") },
                    modifier = Modifier.fillMaxWidth().testTag("add_task_title_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = { Text("Due deadline (e.g. 5:00 PM)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "Priority Level", color = TextSecondary, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("High", "Medium", "Low").forEach { p ->
                        val isSel = priority == p
                        FilterChip(
                            selected = isSel,
                            onClick = { priority = p },
                            label = { Text(p, fontSize = 11.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("CANCEL", color = TextSecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (title.isNotBlank()) onAdd(title, priority, deadline) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("confirm_add_task_button")
                    ) {
                        Text("DISPATCH", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, List<String>, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Dungeon") }
    var targetDate by remember { mutableStateOf("Nov 15, 2026") }
    var milestone1 by remember { mutableStateOf("") }
    var milestone2 by remember { mutableStateOf("") }
    var milestone3 by remember { mutableStateOf("") }
    var xpReward by remember { mutableIntStateOf(500) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SystemSurface),
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "SEAL STRATEGIC GOAL",
                    fontWeight = FontWeight.Black,
                    color = GoldGlow
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Campaign Goal (e.g. Clear Red Gate)") },
                    modifier = Modifier.fillMaxWidth().testTag("add_goal_title_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    label = { Text("Target Date") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Campaign Milestones (Sub-tasks)", color = TextSecondary, fontSize = 11.sp)
                OutlinedTextField(
                    value = milestone1,
                    onValueChange = { milestone1 = it },
                    label = { Text("Milestone 1") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = milestone2,
                    onValueChange = { milestone2 = it },
                    label = { Text("Milestone 2 (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = milestone3,
                    onValueChange = { milestone3 = it },
                    label = { Text("Milestone 3 (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("CANCEL", color = TextSecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val msList = listOf(milestone1, milestone2, milestone3).filter { it.isNotBlank() }
                                onAdd(title, category, targetDate, msList, xpReward)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        modifier = Modifier.testTag("confirm_add_goal_button")
                    ) {
                        Text("SEAL TARGET", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
