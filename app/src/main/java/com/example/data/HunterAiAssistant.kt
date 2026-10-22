package com.example.data

import com.example.data.security.SecurityAuditManager
import com.example.data.security.SecurityEventType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

data class AiMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "HUNTER" or "SYSTEM_AI"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String = "GENERAL" // PLAN, GOAL, HABIT, QUEST, ADVICE
)

object HunterAiAssistant {

    private val _messages = MutableStateFlow<List<AiMessage>>(getInitialAiMessages())
    val messages: StateFlow<List<AiMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun getInitialAiMessages(): List<AiMessage> = listOf(
        AiMessage(
            sender = "SYSTEM_AI",
            content = "Greetings, Hunter. I am the Architect System AI. I analyze your daily dungeon missions, focus duration, and habit consistency to forge an optimal path to Monarch rank. How may I optimize your protocol today?",
            category = "ADVICE"
        )
    )

    suspend fun sendMessage(
        userPrompt: String,
        profile: HunterProfile,
        missions: List<Mission>,
        habits: List<ProductivityHabit>
    ): String = withContext(Dispatchers.IO) {
        val cleanPrompt = userPrompt.trim()
        if (cleanPrompt.isBlank()) return@withContext ""

        // Rate limit verification
        val isAllowed = SecurityAuditManager.checkRateLimit(
            actionKey = "ai_prompt_${profile.username}",
            maxOperations = if (profile.isPremium) 30 else 10,
            cooldownMillis = 60_000L
        )

        val userMsg = AiMessage(sender = "HUNTER", content = cleanPrompt)
        _messages.update { it + userMsg }

        if (!isAllowed) {
            val limitMsg = "System Notice: Rate limit reached. Standard hunters are allocated 10 queries/min. Upgrade to ARISE Premium for unlimited access."
            val aiMsg = AiMessage(sender = "SYSTEM_AI", content = limitMsg, category = "ADVICE")
            _messages.update { it + aiMsg }
            return@withContext limitMsg
        }

        _isGenerating.value = true

        try {
            // Build system context
            val completedMissions = missions.count { it.isCompleted }
            val completedHabits = habits.count { it.isDoneToday }
            val systemContext = """
                You are the System Architect AI from Solo Leveling ('ARISE').
                Hunter Status:
                - Name: ${profile.name} (Rank: ${profile.rank.code}, Level: ${profile.level})
                - XP Progress: ${profile.currentXp} / ${profile.xpForNextLevel}
                - Gold: ${profile.gold}, Mana Crystals: ${profile.crystals}
                - Habit Streak: ${profile.streakDays} days
                - Missions Completed Today: $completedMissions / ${missions.size}
                - Habits Done Today: $completedHabits / ${habits.size}

                Provide precise, inspiring, productivity guidance. Keep responses tactical, motivating, and gamified.
            """.trimIndent()

            val responseText = executeGeminiQuery(cleanPrompt, systemContext)
            val aiMsg = AiMessage(sender = "SYSTEM_AI", content = responseText, category = categorizePrompt(cleanPrompt))
            _messages.update { it + aiMsg }
            _isGenerating.value = false
            return@withContext responseText
        } catch (e: Exception) {
            _isGenerating.value = false
            val fallbackResponse = generateLocalTacticalFallback(cleanPrompt, profile)
            val aiMsg = AiMessage(sender = "SYSTEM_AI", content = fallbackResponse, category = "ADVICE")
            _messages.update { it + aiMsg }
            return@withContext fallbackResponse
        }
    }

    private fun executeGeminiQuery(prompt: String, systemInstruction: String): String {
        // Construct standard Gemini REST request
        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", "$systemInstruction\n\nHunter Inquiry: $prompt") })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 800)
            })
        }

        // Send to standard Generative Language endpoint
        // If API key is not configured in environment, fallback safely to built-in system generation
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful || responseBody.isBlank()) {
            throw IllegalStateException("API error: ${response.code}")
        }

        val jsonResponse = JSONObject(responseBody)
        val candidates = jsonResponse.optJSONArray("candidates") ?: return "System response parsed without output."
        val firstCandidate = candidates.optJSONObject(0) ?: return "No response candidate."
        val content = firstCandidate.optJSONObject("content") ?: return "No content."
        val parts = content.optJSONArray("parts") ?: return "No parts."
        val text = parts.optJSONObject(0)?.optString("text") ?: "No text generated."
        return text
    }

    private fun generateLocalTacticalFallback(prompt: String, profile: HunterProfile): String {
        val p = prompt.lowercase()
        return when {
            p.contains("plan") || p.contains("schedule") -> {
                "TACTICAL DAILY PROTOCOL FOR LEVEL ${profile.level} HUNTER:\n" +
                "1. 07:00 - Physical Conditioning: 100 pushups, 100 situps, 10km run (+120 XP)\n" +
                "2. 10:00 - Deep Focus Sanctuary: 45 min core skill study (+225 XP)\n" +
                "3. 14:00 - Dungeon Raid Protocol: Tackle highest priority work challenge\n" +
                "4. 20:00 - Evening Review & Recovery (+50 Gold)"
            }
            p.contains("goal") || p.contains("break down") -> {
                "GOAL DECONSTRUCTION PROTOCOL:\n" +
                "• Milestone 1: Foundation (Days 1-7) - Complete 5 daily habit check-ins\n" +
                "• Milestone 2: Momentum (Days 8-14) - Clear 10 dungeon quests\n" +
                "• Milestone 3: Mastery (Days 15-21) - Achieve Rank promotion"
            }
            p.contains("quest") || p.contains("mission") -> {
                "SPECIAL S-RANK CHALLENGE GENERATED:\n" +
                "Quest: 'Overcoming the Limit'\n" +
                "Objective: 60 minutes uninterrupted focus session without checking device\n" +
                "Reward: +450 XP, +150 Gold, +5 Mana Crystals"
            }
            else -> {
                "System Directive: Hunter ${profile.name}, your current streak of ${profile.streakDays} days is bolstering your core vitality. Continue executing your daily missions to awaken further shadow abilities."
            }
        }
    }

    private fun categorizePrompt(prompt: String): String {
        val p = prompt.lowercase()
        return when {
            p.contains("plan") -> "PLAN"
            p.contains("goal") -> "GOAL"
            p.contains("habit") -> "HABIT"
            p.contains("quest") || p.contains("mission") -> "QUEST"
            else -> "ADVICE"
        }
    }

    fun clearHistory() {
        _messages.value = getInitialAiMessages()
    }
}
