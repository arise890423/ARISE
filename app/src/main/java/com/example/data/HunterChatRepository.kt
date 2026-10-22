package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

data class ChatRoom(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val type: String = "PUBLIC_ROOM", // PUBLIC_ROOM, GROUP, DIRECT
    val creatorId: String = "",
    val memberIds: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastSenderName: String = "",
    val lastMessageTimestamp: Date? = null,
    val unreadCount: Int = 0
)

data class ChatMessage(
    val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRank: String = "E-Rank",
    val text: String = "",
    val timestamp: Date? = null,
    val isPending: Boolean = false,
    val isFailed: Boolean = false
)

data class OnlineUser(
    val id: String = "",
    val name: String = "",
    val username: String = "",
    val rank: String = "E-Rank",
    val level: Int = 1,
    val isOnline: Boolean = true,
    val showOnlineStatus: Boolean = true
)

class HunterChatRepository(private val context: Context) {
    private val TAG = "HunterChatRepository"

    private val db: FirebaseFirestore? by lazy {
        try {
            val dbId = context.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(dbId)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve firestore_database_id: ${e.message}")
            null
        }
    }

    private val auth: FirebaseAuth? by lazy {
        HunterAuthManager.getFirebaseAuth(context)
    }

    val currentUserId: String
        get() = auth?.currentUser?.uid.orEmpty()

    val isAuthenticated: Boolean
        get() = !auth?.currentUser?.uid.isNullOrBlank()

    // Blocked users set
    private val _blockedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedUserIds = _blockedUserIds.asStateFlow()

    // Last message timestamp sent by current client for spam protection
    private var lastMessageSentMillis = 0L

    fun blockUser(userId: String) {
        _blockedUserIds.update { it + userId }
    }

    fun unblockUser(userId: String) {
        _blockedUserIds.update { it - userId }
    }

    fun isUserBlocked(userId: String): Boolean {
        return _blockedUserIds.value.contains(userId)
    }

    // 1. Observe Community & Group & Direct Rooms with Real-time Snapshot Listener
    fun observeRooms(): Flow<List<ChatRoom>> = callbackFlow {
        val firestore = db
        val uid = auth?.currentUser?.uid
        if (firestore == null || uid.isNullOrBlank()) {
            // Offline/unauthenticated fallback: provide standard room templates
            trySend(getFallbackRooms())
            awaitClose { }
            return@callbackFlow
        }

        val listener = firestore.collection("community_rooms")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error observing rooms: ${error.message}")
                    trySend(getFallbackRooms())
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    // Seed initial rooms on cloud backend so rooms persist across all devices
                    seedInitialRoomsOnBackend(uid)
                    trySend(getFallbackRooms())
                    return@addSnapshotListener
                }

                val rooms = snapshot.documents.mapNotNull { doc ->
                    val id = doc.id
                    val name = doc.getString("name") ?: "Unnamed Room"
                    val desc = doc.getString("description") ?: ""
                    val type = doc.getString("type") ?: "PUBLIC_ROOM"
                    val creatorId = doc.getString("creatorId") ?: ""
                    val members = (doc.get("memberIds") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                    val lastMsg = doc.getString("lastMessage") ?: ""
                    val lastSender = doc.getString("lastSenderName") ?: ""
                    val lastTime = doc.getTimestamp("lastMessageTimestamp")?.toDate()
                        ?: doc.getTimestamp("createdAt")?.toDate()

                    ChatRoom(
                        id = id,
                        name = name,
                        description = desc,
                        type = type,
                        creatorId = creatorId,
                        memberIds = members,
                        lastMessage = lastMsg,
                        lastSenderName = lastSender,
                        lastMessageTimestamp = lastTime,
                        unreadCount = 0
                    )
                }
                trySend(if (rooms.isEmpty()) getFallbackRooms() else rooms)
            }

        awaitClose { listener.remove() }
    }

    // 2. Observe Messages in a Room with Real-Time Snapshot Listener & Pagination Limit
    fun observeMessages(roomId: String, limit: Long = 50): Flow<List<ChatMessage>> = callbackFlow {
        val firestore = db
        val uid = auth?.currentUser?.uid
        if (firestore == null || roomId.isBlank() || uid.isNullOrBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener = firestore.collection("community_rooms")
            .document(roomId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error observing messages for room $roomId: ${error.message}")
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    val senderId = doc.getString("senderId") ?: ""
                    // Filter out blocked users
                    if (_blockedUserIds.value.contains(senderId)) return@mapNotNull null

                    ChatMessage(
                        id = doc.id,
                        roomId = roomId,
                        senderId = senderId,
                        senderName = doc.getString("senderName") ?: "Hunter",
                        senderRank = doc.getString("senderRank") ?: "E-Rank",
                        text = doc.getString("text") ?: "",
                        timestamp = doc.getTimestamp("createdAt")?.toDate() ?: Date()
                    )
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    // 3. Send Message with Anti-Spam Validation & Real-time Persistence
    suspend fun sendMessage(
        roomId: String,
        text: String,
        senderName: String,
        senderRank: String
    ): Result<Unit> {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) {
            return Result.failure(IllegalArgumentException("Message cannot be blank."))
        }
        if (cleanText.length > 1000) {
            return Result.failure(IllegalArgumentException("Message exceeds maximum allowed length of 1000 characters."))
        }

        val uid = auth?.currentUser?.uid
        if (uid.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Google Sign-In required. Please authenticate with Google to transmit guild messages."))
        }

        // Spam rate-limiting: max 1 message every 800ms
        val now = System.currentTimeMillis()
        if (now - lastMessageSentMillis < 800) {
            return Result.failure(IllegalStateException("Rate limit exceeded. Please wait a moment before sending another message."))
        }
        lastMessageSentMillis = now

        val firestore = db ?: return Result.failure(IllegalStateException("Firestore is uninitialized."))

        return try {
            val messageDoc = firestore.collection("community_rooms")
                .document(roomId)
                .collection("messages")
                .document()

            val msgData = hashMapOf(
                "roomId" to roomId,
                "senderId" to uid,
                "senderName" to senderName,
                "senderRank" to senderRank,
                "text" to cleanText,
                "createdAt" to FieldValue.serverTimestamp()
            )

            messageDoc.set(msgData).await()

            // Update parent room metadata if room exists
            try {
                firestore.collection("community_rooms").document(roomId).update(
                    mapOf(
                        "lastMessage" to cleanText,
                        "lastSenderName" to senderName,
                        "lastMessageTimestamp" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (updateErr: Exception) {
                Log.w(TAG, "Parent room metadata update skipped: ${updateErr.message}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message: ${e.message}", e)
            Result.failure(e)
        }
    }

    // 4. Create Community Room / Guild Squad
    suspend fun createRoom(name: String, description: String, type: String = "GROUP"): Result<String> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore uninitialized"))
        val uid = auth?.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Google Sign-In required to commission squads."))
        return try {
            val roomDoc = firestore.collection("community_rooms").document()
            val roomData = hashMapOf(
                "name" to name,
                "description" to description,
                "type" to type,
                "creatorId" to uid,
                "memberIds" to listOf(uid),
                "lastMessage" to "Room established by Guild Commander.",
                "lastSenderName" to "System",
                "createdAt" to FieldValue.serverTimestamp(),
                "lastMessageTimestamp" to FieldValue.serverTimestamp()
            )
            roomDoc.set(roomData).await()
            Result.success(roomDoc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 5. Direct 1-on-1 Conversation Finder or Creator
    suspend fun getOrCreateDirectRoom(targetUserId: String, targetUserName: String): Result<String> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore uninitialized"))
        val uid = auth?.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Google Sign-In required to establish direct comms."))

        return try {
            // Find existing direct room
            val existing = firestore.collection("community_rooms")
                .whereEqualTo("type", "DIRECT")
                .whereArrayContains("memberIds", uid)
                .get()
                .await()

            val found = existing.documents.firstOrNull { doc ->
                val members = (doc.get("memberIds") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                members.contains(targetUserId)
            }

            if (found != null) {
                Result.success(found.id)
            } else {
                // Create direct conversation
                val directDoc = firestore.collection("community_rooms").document()
                val data = hashMapOf(
                    "name" to "Direct: $targetUserName",
                    "description" to "Secure hunter direct communication channel",
                    "type" to "DIRECT",
                    "creatorId" to uid,
                    "memberIds" to listOf(uid, targetUserId),
                    "lastMessage" to "Direct link established.",
                    "lastSenderName" to "System",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "lastMessageTimestamp" to FieldValue.serverTimestamp()
                )
                directDoc.set(data).await()
                Result.success(directDoc.id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 6. User Presence & Online Status with Privacy Controls
    suspend fun updatePresence(name: String, rank: String, level: Int, isOnline: Boolean, showOnlineStatus: Boolean) {
        val firestore = db ?: return
        val uid = auth?.currentUser?.uid ?: return
        try {
            val userRef = firestore.collection("users").document(uid)
            val data = hashMapOf(
                "name" to name,
                "rank" to rank,
                "level" to level,
                "isOnline" to (isOnline && showOnlineStatus),
                "showOnlineStatus" to showOnlineStatus,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            userRef.set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Presence update skipped: ${e.message}")
        }
    }

    // 7. Observe Hunter Registry for User Search & Status
    fun observeHunters(): Flow<List<OnlineUser>> = callbackFlow {
        val firestore = db
        val uid = auth?.currentUser?.uid
        if (firestore == null || uid.isNullOrBlank()) {
            trySend(getFallbackHunters())
            awaitClose { }
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .limit(30)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(getFallbackHunters())
                    return@addSnapshotListener
                }
                val hunters = snapshot?.documents?.mapNotNull { doc ->
                    val showOnline = doc.getBoolean("showOnlineStatus") ?: true
                    OnlineUser(
                        id = doc.id,
                        name = doc.getString("name") ?: "Hunter",
                        username = doc.getString("username") ?: "@hunter_${doc.id.take(4)}",
                        rank = doc.getString("rank") ?: "E-Rank",
                        level = doc.getLong("level")?.toInt() ?: 1,
                        isOnline = if (showOnline) doc.getBoolean("isOnline") ?: false else false,
                        showOnlineStatus = showOnline
                    )
                } ?: emptyList()

                trySend(if (hunters.isEmpty()) getFallbackHunters() else hunters)
            }

        awaitClose { listener.remove() }
    }

    // 8. Report Abuse and Spam Protection
    suspend fun reportMessage(reportedUserId: String, messageId: String, reason: String): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException("Firestore uninitialized"))
        val uid = auth?.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Google Sign-In required to submit reports."))
        return try {
            val reportDoc = firestore.collection("reports").document()
            val data = hashMapOf(
                "reporterId" to uid,
                "reportedUserId" to reportedUserId,
                "reportedMessageId" to messageId,
                "reason" to reason,
                "createdAt" to FieldValue.serverTimestamp()
            )
            reportDoc.set(data).await()
            // Automatically block user upon reporting
            blockUser(reportedUserId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun seedInitialRoomsOnBackend(uid: String) {
        val firestore = db ?: return
        val initial = listOf(
            Triple("room_guild_hall", "Ahjin Guild Hall", "Main headquarters for all Hunter Association communications."),
            Triple("room_solo_hunters", "Solo Hunters Lounge", "Open hub for unattached hunters seeking raid partners."),
            Triple("room_red_gate", "Red Gate Strike Squad", "Tactical combat discussions for high-difficulty dungeons.")
        )

        initial.forEach { (roomId, name, desc) ->
            val doc = firestore.collection("community_rooms").document(roomId)
            val data = hashMapOf(
                "name" to name,
                "description" to desc,
                "type" to if (roomId == "room_red_gate") "GROUP" else "PUBLIC_ROOM",
                "creatorId" to uid,
                "memberIds" to listOf(uid),
                "lastMessage" to "Welcome to $name. Maintain the Hunter Code.",
                "lastSenderName" to "System",
                "createdAt" to FieldValue.serverTimestamp(),
                "lastMessageTimestamp" to FieldValue.serverTimestamp()
            )
            doc.set(data, SetOptions.merge())
        }
    }

    private fun getFallbackRooms(): List<ChatRoom> = listOf(
        ChatRoom(
            id = "room_guild_hall",
            name = "Ahjin Guild Hall",
            description = "Main headquarters for all Hunter Association communications.",
            type = "PUBLIC_ROOM",
            lastMessage = "Hunter Association has verified all sector gates.",
            lastSenderName = "System",
            lastMessageTimestamp = Date(),
            unreadCount = 2
        ),
        ChatRoom(
            id = "room_solo_hunters",
            name = "Solo Hunters Lounge",
            description = "Open hub for unattached hunters seeking raid partners.",
            type = "PUBLIC_ROOM",
            lastMessage = "Anyone available for a C-Rank dungeon run?",
            lastSenderName = "Yoo Jinho",
            lastMessageTimestamp = Date(),
            unreadCount = 0
        ),
        ChatRoom(
            id = "room_red_gate",
            name = "Red Gate Strike Squad",
            description = "High-threat tactical squad channel.",
            type = "GROUP",
            lastMessage = "Boss chamber coordinates locked in.",
            lastSenderName = "Cha Hae-In",
            lastMessageTimestamp = Date(),
            unreadCount = 1
        )
    )

    private fun getFallbackHunters(): List<OnlineUser> = listOf(
        OnlineUser("u1", "Cha Hae-In", "@sword_dancer", "S-Rank", 58, isOnline = true),
        OnlineUser("u2", "Woo Jin-Chul", "@inspector_woo", "A-Rank", 44, isOnline = true),
        OnlineUser("u3", "Baek Yoonho", "@beast_king", "S-Rank", 50, isOnline = true),
        OnlineUser("u4", "Yoo Jinho", "@vice_chair", "D-Rank", 18, isOnline = true),
        OnlineUser("u5", "Choi Jong-In", "@ultimate_mage", "S-Rank", 52, isOnline = false)
    )
}
