package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.RankBadge
import com.example.ui.components.SystemWindowCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialScreen(
    chatRepository: HunterChatRepository,
    currentProfile: HunterProfile,
    guildMembers: List<GuildMember>,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val accent = MaterialTheme.colorScheme.primary

    var isChatAuthenticated by remember { mutableStateOf(chatRepository.isAuthenticated) }
    var isAuthenticatingWithGoogle by remember { mutableStateOf(false) }

    // Real-time chat rooms from Firestore
    val rooms by chatRepository.observeRooms().collectAsState(initial = emptyList())
    val onlineHunters by chatRepository.observeHunters().collectAsState(initial = emptyList())
    val blockedIds by chatRepository.blockedUserIds.collectAsState()

    // Screen State
    var socialTab by remember { mutableIntStateOf(0) } // 0: Communications, 1: Hunter Registry, 2: Leaderboard
    var selectedRoomId by remember { mutableStateOf<String?>(null) }
    var roomFilterType by remember { mutableStateOf("ALL") } // ALL, PUBLIC_ROOM, DIRECT, GROUP

    // Pagination
    var messageLimit by remember { mutableLongStateOf(40L) }

    // Active room messages
    val activeRoomId = selectedRoomId ?: rooms.firstOrNull()?.id ?: ""
    val messagesFlow = remember(activeRoomId, messageLimit, isChatAuthenticated) {
        chatRepository.observeMessages(activeRoomId, limit = messageLimit)
    }
    val messages by messagesFlow.collectAsState(initial = emptyList())

    // Chat input & status
    var inputMessage by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var failedMessageAttempt by remember { mutableStateOf<String?>(null) }

    fun performGoogleSignIn() {
        coroutineScope.launch {
            isAuthenticatingWithGoogle = true
            errorMessage = null
            val res = HunterAuthManager.launchGoogleSignIn(context)
            isAuthenticatingWithGoogle = false
            res.onSuccess { (displayName, email) ->
                HunterRepository.signInWithGoogle(displayName, email)
                isChatAuthenticated = true
            }.onFailure { err ->
                errorMessage = err.message ?: "Google Sign-In failed or was cancelled."
            }
        }
    }

    // Dialogs
    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var reportTargetMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val activeRoom = rooms.find { it.id == activeRoomId }
    val currentUserId = chatRepository.currentUserId
    val listState = rememberLazyListState()

    // Auto-scroll on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 12.dp)
    ) {
        // Main Header & Tab Navigation
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            color = SystemSurface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SystemCardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf("Comms Hub", "Registry & Search", "Rankings")
                tabs.forEachIndexed { i, title ->
                    val isSel = socialTab == i
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { socialTab = i }
                            .testTag("social_tab_$i"),
                        color = if (isSel) accent.copy(alpha = 0.2f) else Color.Transparent,
                        border = if (isSel) BorderStroke(1.dp, accent) else null,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) accent else TextSecondary
                            ),
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (socialTab) {
            0 -> {
                // Communications Hub: Room Selector + Live Chat
                if (selectedRoomId == null && rooms.isNotEmpty()) {
                    // Rooms & Direct Conversations Directory View
                    RoomsDirectoryView(
                        rooms = rooms,
                        activeFilter = roomFilterType,
                        onFilterChange = { roomFilterType = it },
                        onSelectRoom = { selectedRoomId = it },
                        onCreateRoomClick = {
                            if (!isChatAuthenticated) {
                                performGoogleSignIn()
                            } else {
                                showCreateRoomDialog = true
                            }
                        },
                        currentUserId = currentUserId,
                        isAuthenticated = isChatAuthenticated,
                        isAuthenticating = isAuthenticatingWithGoogle,
                        onAuthenticate = { performGoogleSignIn() },
                        accent = accent
                    )
                } else {
                    // Inside Active Chat Channel
                    ActiveChatChannelView(
                        room = activeRoom,
                        messages = messages,
                        currentUserId = currentUserId,
                        currentProfile = currentProfile,
                        inputMessage = inputMessage,
                        isSending = isSending,
                        isAuthenticated = isChatAuthenticated,
                        isAuthenticating = isAuthenticatingWithGoogle,
                        errorMessage = errorMessage,
                        failedMessageAttempt = failedMessageAttempt,
                        messageLimit = messageLimit,
                        accent = accent,
                        listState = listState,
                        onBack = { selectedRoomId = null },
                        onInputChange = {
                            if (it.length <= 1000) inputMessage = it
                            errorMessage = null
                        },
                        onSendMessage = { text ->
                            if (!isChatAuthenticated) {
                                performGoogleSignIn()
                                return@ActiveChatChannelView
                            }
                            coroutineScope.launch {
                                isSending = true
                                errorMessage = null
                                failedMessageAttempt = null
                                val result = chatRepository.sendMessage(
                                    roomId = activeRoomId,
                                    text = text,
                                    senderName = currentProfile.name,
                                    senderRank = currentProfile.rank.title
                                )
                                isSending = false
                                if (result.isSuccess) {
                                    inputMessage = ""
                                } else {
                                    val err = result.exceptionOrNull()?.message ?: "Failed to transmit message."
                                    errorMessage = err
                                    failedMessageAttempt = text
                                }
                            }
                        },
                        onRetryMessage = { text ->
                            if (!isChatAuthenticated) {
                                performGoogleSignIn()
                                return@ActiveChatChannelView
                            }
                            coroutineScope.launch {
                                isSending = true
                                errorMessage = null
                                failedMessageAttempt = null
                                val result = chatRepository.sendMessage(
                                    roomId = activeRoomId,
                                    text = text,
                                    senderName = currentProfile.name,
                                    senderRank = currentProfile.rank.title
                                )
                                isSending = false
                                if (result.isFailure) {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Retry failed."
                                    failedMessageAttempt = text
                                }
                            }
                        },
                        onAuthenticate = { performGoogleSignIn() },
                        onLoadMore = {
                            messageLimit += 30L
                        },
                        onReportMessage = { msg ->
                            reportTargetMessage = msg
                        },
                        onBlockUser = { userId ->
                            chatRepository.blockUser(userId)
                        }
                    )
                }
            }

            1 -> {
                // Hunter Registry & Real-Time Search & Direct Message Initiator
                HunterRegistryView(
                    hunters = onlineHunters,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    blockedIds = blockedIds,
                    currentProfile = currentProfile,
                    accent = accent,
                    onStartDirectChat = { targetUser ->
                        coroutineScope.launch {
                            val res = chatRepository.getOrCreateDirectRoom(targetUser.id, targetUser.name)
                            if (res.isSuccess) {
                                selectedRoomId = res.getOrNull()
                                socialTab = 0
                            }
                        }
                    },
                    onToggleBlock = { userId ->
                        if (chatRepository.isUserBlocked(userId)) {
                            chatRepository.unblockUser(userId)
                        } else {
                            chatRepository.blockUser(userId)
                        }
                    },
                    onToggleStealth = {
                        coroutineScope.launch {
                            val newStatus = !currentProfile.showOnlineStatus
                            HunterRepository.setShowOnlineStatus(newStatus)
                            chatRepository.updatePresence(
                                name = currentProfile.name,
                                rank = currentProfile.rank.title,
                                level = currentProfile.level,
                                isOnline = newStatus,
                                showOnlineStatus = newStatus
                            )
                        }
                    }
                )
            }

            2 -> {
                // Global Rankings
                LeaderboardView(
                    guildMembers = guildMembers,
                    currentProfile = currentProfile,
                    accent = accent
                )
            }
        }
    }

    // Modal: Commission New Squad / Group Room
    if (showCreateRoomDialog) {
        CreateRoomDialog(
            onDismiss = { showCreateRoomDialog = false },
            onCreate = { name, desc, type ->
                coroutineScope.launch {
                    val res = chatRepository.createRoom(name, desc, type)
                    if (res.isSuccess) {
                        selectedRoomId = res.getOrNull()
                        showCreateRoomDialog = false
                    }
                }
            },
            accent = accent
        )
    }

    // Modal: Report Abuse & Spam Protection
    if (reportTargetMessage != null) {
        val msg = reportTargetMessage!!
        ReportAbuseDialog(
            message = msg,
            onDismiss = { reportTargetMessage = null },
            onSubmitReport = { reason ->
                coroutineScope.launch {
                    chatRepository.reportMessage(
                        reportedUserId = msg.senderId,
                        messageId = msg.id,
                        reason = reason
                    )
                    reportTargetMessage = null
                }
            },
            accent = accent
        )
    }
}

@Composable
private fun RoomsDirectoryView(
    rooms: List<ChatRoom>,
    activeFilter: String,
    onFilterChange: (String) -> Unit,
    onSelectRoom: (String) -> Unit,
    onCreateRoomClick: () -> Unit,
    currentUserId: String,
    isAuthenticated: Boolean,
    isAuthenticating: Boolean,
    onAuthenticate: () -> Unit,
    accent: Color
) {
    val filteredRooms = rooms.filter { room ->
        when (activeFilter) {
            "PUBLIC_ROOM" -> room.type == "PUBLIC_ROOM"
            "DIRECT" -> room.type == "DIRECT"
            "GROUP" -> room.type == "GROUP"
            else -> true
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        if (!isAuthenticated) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SystemSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "OFFLINE FREQUENCY RELAY",
                                color = GoldPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Sign in with Google to establish live real-time sync with all hunters.",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onAuthenticate,
                            enabled = !isAuthenticating,
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            if (isAuthenticating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("SIGN IN", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            // Header Action Card
            SystemWindowCard(borderColor = accent.copy(alpha = 0.5f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "COMMUNICATIONS FREQUENCY",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = accent
                            )
                        )
                        Text(
                            text = "Real-time encrypted relays with hunter squads.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = onCreateRoomClick,
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("create_room_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Squad",
                            modifier = Modifier.size(16.dp),
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("NEW SQUAD", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val filters = listOf(
                    "ALL" to "All Channels",
                    "PUBLIC_ROOM" to "Guild Halls",
                    "DIRECT" to "Direct Comms",
                    "GROUP" to "Raid Squads"
                )
                filters.forEach { (type, label) ->
                    val isSel = activeFilter == type
                    FilterChip(
                        selected = isSel,
                        onClick = { onFilterChange(type) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent.copy(alpha = 0.25f),
                            selectedLabelColor = accent,
                            containerColor = SystemSurface,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSel,
                            borderColor = if (isSel) accent else SystemCardBorder
                        )
                    )
                }
            }
        }

        if (filteredRooms.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    color = SystemSurface.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SystemCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No active channels found in this sector.", color = TextSecondary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = onCreateRoomClick) {
                            Text("Commission First Squad", color = accent)
                        }
                    }
                }
            }
        }

        items(filteredRooms, key = { it.id }) { room ->
            val isDirect = room.type == "DIRECT"
            val isGroup = room.type == "GROUP"
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val formattedTime = room.lastMessageTimestamp?.let { timeFormat.format(it) } ?: ""

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectRoom(room.id) }
                    .testTag("room_item_${room.id}"),
                color = SystemSurface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SystemCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Room Icon Avatar
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isDirect -> ElectricViolet.copy(alpha = 0.2f)
                                    isGroup -> CrimsonPrimary.copy(alpha = 0.2f)
                                    else -> accent.copy(alpha = 0.2f)
                                }
                            )
                            .border(
                                1.dp,
                                when {
                                    isDirect -> ElectricViolet
                                    isGroup -> CrimsonPrimary
                                    else -> accent
                                },
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isDirect -> Icons.Default.Lock
                                isGroup -> Icons.Default.Shield
                                else -> Icons.Default.Public
                            },
                            contentDescription = null,
                            tint = when {
                                isDirect -> ElectricViolet
                                isGroup -> CrimsonPrimary
                                else -> accent
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = room.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (formattedTime.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = formattedTime,
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = if (room.lastMessage.isNotBlank()) "${room.lastSenderName}: ${room.lastMessage}" else room.description,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (room.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = CrimsonPrimary,
                            shape = CircleShape,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${room.unreadCount}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveChatChannelView(
    room: ChatRoom?,
    messages: List<ChatMessage>,
    currentUserId: String,
    currentProfile: HunterProfile,
    inputMessage: String,
    isSending: Boolean,
    isAuthenticated: Boolean,
    isAuthenticating: Boolean,
    errorMessage: String?,
    failedMessageAttempt: String?,
    messageLimit: Long,
    accent: Color,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onBack: () -> Unit,
    onInputChange: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onRetryMessage: (String) -> Unit,
    onAuthenticate: () -> Unit,
    onLoadMore: () -> Unit,
    onReportMessage: (ChatMessage) -> Unit,
    onBlockUser: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Channel Top Bar with Back and Info
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            color = SystemSurface,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, SystemCardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Channels",
                        tint = accent
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = room?.name ?: "Hunter Channel",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = room?.description?.ifBlank { "Encrypted hunter frequency" } ?: "Live",
                        color = TextMuted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Channel Badge Indicator
                Surface(
                    color = accent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, accent)
                ) {
                    Text(
                        text = when (room?.type) {
                            "DIRECT" -> "DIRECT"
                            "GROUP" -> "SQUAD"
                            else -> "GUILD"
                        },
                        color = accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Error / Retry Banner
        if (errorMessage != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                color = CrimsonPrimary.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, CrimsonPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage,
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (failedMessageAttempt != null) {
                        TextButton(
                            onClick = { onRetryMessage(failedMessageAttempt) },
                            modifier = Modifier.testTag("retry_send_button")
                        ) {
                            Text("RETRY", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Live Chat Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Pagination Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = onLoadMore) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Load earlier",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Load earlier transmissions",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (messages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Frequency quiet. Send the opening transmission.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                val isMe = msg.senderId == currentUserId || msg.senderName == currentProfile.name
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val formattedTime = msg.timestamp?.let { timeFormat.format(it) } ?: ""

                var showMenu by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                ) {
                    if (!isMe) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Text(
                                text = msg.senderName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = SystemSurfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = msg.senderRank,
                                    color = GoldPrimary,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Box {
                        Surface(
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .clickable { if (!isMe) showMenu = true },
                            color = if (isMe) accent.copy(alpha = 0.25f) else SystemSurfaceVariant,
                            shape = RoundedCornerShape(
                                topStart = 12.dp,
                                topEnd = 12.dp,
                                bottomStart = if (isMe) 12.dp else 2.dp,
                                bottomEnd = if (isMe) 2.dp else 12.dp
                            ),
                            border = BorderStroke(1.dp, if (isMe) accent else SystemCardBorder)
                        ) {
                            Text(
                                text = msg.text,
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }

                        // Message Context Menu (Report & Block)
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(SystemSurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Report Message (Spam/Abuse)", color = CrimsonPrimary, fontSize = 12.sp) },
                                onClick = {
                                    showMenu = false
                                    onReportMessage(msg)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Flag, contentDescription = "Report", tint = CrimsonPrimary)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Block Hunter (${msg.senderName})", color = Color.White, fontSize = 12.sp) },
                                onClick = {
                                    showMenu = false
                                    onBlockUser(msg.senderId)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Block, contentDescription = "Block", tint = TextMuted)
                                }
                            )
                        }
                    }

                    // Timestamp and Delivery Status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = formattedTime,
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (isMe) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Delivered",
                                tint = accent,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Quick Tactical Broadcast Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val quickChips = listOf(
                "⚔️ Raid Ready",
                "🛡️ Need Tank",
                "📍 Sector Clear"
            )
            quickChips.forEach { chip ->
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSendMessage(chip) },
                    color = SystemSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, SystemCardBorder)
                ) {
                    Text(
                        text = chip,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Chat Input Bar or Authentication Lock
        if (!isAuthenticated) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                color = SystemSurface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, accent)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "COMMUNICATION RELAY LOCKED",
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Connect Google Account to broadcast live guild transmissions.",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onAuthenticate,
                        enabled = !isAuthenticating,
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("CONNECT", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                color = SystemSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SystemCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputMessage,
                        onValueChange = onInputChange,
                        placeholder = {
                            Text("Transmitting to hunter guild...", color = TextMuted, fontSize = 13.sp)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("message_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        maxLines = 3
                    )

                    // Character count indicator
                    Text(
                        text = "${inputMessage.length}/1000",
                        color = if (inputMessage.length > 950) CrimsonPrimary else TextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = accent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = {
                                if (inputMessage.isNotBlank()) {
                                    onSendMessage(inputMessage)
                                }
                            },
                            enabled = inputMessage.isNotBlank(),
                            modifier = Modifier.testTag("send_message_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (inputMessage.isNotBlank()) accent else TextMuted
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(70.dp))
    }
}

@Composable
private fun HunterRegistryView(
    hunters: List<OnlineUser>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    blockedIds: Set<String>,
    currentProfile: HunterProfile,
    accent: Color,
    onStartDirectChat: (OnlineUser) -> Unit,
    onToggleBlock: (String) -> Unit,
    onToggleStealth: () -> Unit
) {
    val filtered = hunters.filter { hunter ->
        hunter.name.contains(searchQuery, ignoreCase = true) ||
        hunter.username.contains(searchQuery, ignoreCase = true) ||
        hunter.rank.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Privacy / Stealth Control Banner
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SystemSurface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SystemCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (currentProfile.showOnlineStatus) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = if (currentProfile.showOnlineStatus) SuccessGreen else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentProfile.showOnlineStatus) "RADAR ACTIVE (ONLINE)" else "STEALTH CLOAK ENGAGED",
                                fontWeight = FontWeight.Bold,
                                color = if (currentProfile.showOnlineStatus) SuccessGreen else TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = if (currentProfile.showOnlineStatus) "Hunters can view your live squad status." else "Your online beacon is hidden from registry.",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }

                    Switch(
                        checked = currentProfile.showOnlineStatus,
                        onCheckedChange = { onToggleStealth() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SuccessGreen,
                            checkedTrackColor = SuccessGreen.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }

        // Search Field
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search hunters by codename or rank...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_hunters_input"),
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = accent)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = SystemCardBorder
                )
            )
        }

        if (filtered.isEmpty()) {
            item {
                Text(
                    text = "No hunters detected matching query.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        items(filtered, key = { it.id }) { hunter ->
            val isBlocked = blockedIds.contains(hunter.id)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isBlocked) SystemSurface.copy(alpha = 0.5f) else SystemSurface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (isBlocked) CrimsonPrimary.copy(alpha = 0.3f) else SystemCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar with online status ring
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SystemSurfaceVariant)
                            .border(
                                1.5.dp,
                                if (hunter.isOnline) SuccessGreen else TextMuted,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = hunter.name.take(1),
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = hunter.name,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (hunter.isOnline) SuccessGreen else TextMuted)
                            )
                        }
                        Text(
                            text = "${hunter.username} • LVL ${hunter.level} • ${hunter.rank}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    // Direct Message Button
                    IconButton(
                        onClick = { onStartDirectChat(hunter) },
                        modifier = Modifier.testTag("direct_chat_btn_${hunter.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = "Direct Comms",
                            tint = accent
                        )
                    }

                    // Block/Unblock Action
                    IconButton(onClick = { onToggleBlock(hunter.id) }) {
                        Icon(
                            imageVector = if (isBlocked) Icons.Default.LockOpen else Icons.Default.Block,
                            contentDescription = if (isBlocked) "Unblock" else "Block",
                            tint = if (isBlocked) SuccessGreen else TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardView(
    guildMembers: List<GuildMember>,
    currentProfile: HunterProfile,
    accent: Color
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            SystemWindowCard(borderColor = GoldPrimary.copy(alpha = 0.4f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "GLOBAL HUNTER LEADERBOARD",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = GoldGlow
                            )
                        )
                        Text(
                            text = "Rankings authorized by National Hunter Bureau.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Text(text = "👑", fontSize = 28.sp)
                }
            }
        }

        items(guildMembers.sortedByDescending { it.level }) { member ->
            val isUser = member.name.contains(currentProfile.name) || member.name.contains("Jinwoo")
            val index = guildMembers.indexOf(member)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isUser) accent.copy(alpha = 0.15f) else SystemSurface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (isUser) accent else SystemCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (index) {
                            0 -> "🥇"
                            1 -> "🥈"
                            2 -> "🥉"
                            else -> "#${index + 1}"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = GoldPrimary,
                        modifier = Modifier.width(36.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = member.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (isUser) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = accent,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "YOU",
                                        color = Color.Black,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = member.role,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        RankBadge(rank = member.rank)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "LVL ${member.level}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateRoomDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String, type: String) -> Unit,
    accent: Color
) {
    var roomName by remember { mutableStateOf("") }
    var roomDescription by remember { mutableStateOf("") }
    var roomType by remember { mutableStateOf("GROUP") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("COMMISSION NEW SQUAD CHANNEL", fontWeight = FontWeight.Black, color = accent, fontSize = 14.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = roomName,
                    onValueChange = { if (it.length <= 40) roomName = it },
                    label = { Text("Squad / Channel Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = roomDescription,
                    onValueChange = { if (it.length <= 150) roomDescription = it },
                    label = { Text("Directive / Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val types = listOf("GROUP" to "Raid Squad", "PUBLIC_ROOM" to "Public Hall")
                    types.forEach { (type, label) ->
                        val sel = roomType == type
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { roomType = type },
                            color = if (sel) accent.copy(alpha = 0.2f) else SystemSurface,
                            border = BorderStroke(1.dp, if (sel) accent else SystemCardBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (sel) accent else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (roomName.isNotBlank()) {
                        onCreate(roomName.trim(), roomDescription.trim(), roomType)
                    }
                },
                enabled = roomName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("ESTABLISH", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextMuted)
            }
        },
        containerColor = SystemSurface
    )
}

@Composable
private fun ReportAbuseDialog(
    message: ChatMessage,
    onDismiss: () -> Unit,
    onSubmitReport: (reason: String) -> Unit,
    accent: Color
) {
    val reasons = listOf(
        "Spam / Commercial Advertising",
        "Harassment / Threatening Speech",
        "Inappropriate Content",
        "Impersonation of Hunter Authority"
    )
    var selectedReason by remember { mutableStateOf(reasons.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("REPORT TRANSMISSION ABUSE", fontWeight = FontWeight.Black, color = CrimsonPrimary, fontSize = 14.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Reporting sender: ${message.senderName}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "\"${message.text}\"",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Select Violation Category:", color = TextMuted, fontSize = 10.sp)

                reasons.forEach { reason ->
                    val sel = selectedReason == reason
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason },
                        color = if (sel) CrimsonPrimary.copy(alpha = 0.2f) else SystemSurfaceVariant,
                        border = BorderStroke(1.dp, if (sel) CrimsonPrimary else SystemCardBorder),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = reason,
                            color = if (sel) Color.White else TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmitReport(selectedReason) },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
            ) {
                Text("SUBMIT & BLOCK SENDER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextMuted)
            }
        },
        containerColor = SystemSurface
    )
}
