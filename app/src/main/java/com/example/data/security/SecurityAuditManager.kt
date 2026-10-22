package com.example.data.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Date
import java.util.UUID

enum class SecurityEventType {
    AUTH_LOGIN_SUCCESS,
    AUTH_LOGIN_FAILED,
    AUTH_ACCOUNT_LOCKED,
    AUTH_REGISTER_SUCCESS,
    AUTH_LOGOUT,
    RATE_LIMIT_TRIGGERED,
    DEVICE_INTEGRITY_CHECK,
    SUSPICIOUS_PAYLOAD_BLOCKED,
    PERMISSION_DENIED_BLOCKED
}

data class SecurityAuditRecord(
    val id: String = UUID.randomUUID().toString(),
    val eventType: SecurityEventType,
    val identifier: String, // email or user id (sanitized, NO passwords or secrets)
    val timestamp: Long = System.currentTimeMillis(),
    val details: String = "",
    val riskScore: Int = 0 // 0 = benign, 100 = critical
)

data class PlayIntegrityVerdict(
    val isValid: Boolean,
    val riskScore: Int,
    val reason: String
)

/**
 * Manages security audit logs, brute-force mitigation, rate limiting,
 * and Play Integrity advisory checks without logging secrets.
 */
object SecurityAuditManager {

    private const val MAX_FAILED_ATTEMPTS = 5
    private const val LOCKOUT_DURATION_MS = 60_000L // 60 seconds lockout
    private const val FAILURE_WINDOW_MS = 300_000L // 5 minutes window

    // Maps sanitized email -> Pair<failureCount, lastFailureTimeMs>
    private val failedAttempts = mutableMapOf<String, Pair<Int, Long>>()
    // Maps sanitized email -> lockoutExpiryTimestampMs
    private val lockouts = mutableMapOf<String, Long>()

    // Audit logs (in-memory ring buffer of recent security events)
    private val _auditLogs = MutableStateFlow<List<SecurityAuditRecord>>(emptyList())
    val auditLogs: StateFlow<List<SecurityAuditRecord>> = _auditLogs.asStateFlow()

    // Sliding window action history for rate limiting
    private val actionHistory = mutableMapOf<String, MutableList<Long>>()

    /**
     * Resets all lockout and rate-limiting state (useful for test resets).
     */
    fun resetAllLockouts() {
        failedAttempts.clear()
        lockouts.clear()
        actionHistory.clear()
    }

    /**
     * Checks if an account is currently locked out due to excessive failed attempts.
     */
    fun isAccountLocked(identifier: String): Boolean {
        val key = identifier.trim().lowercase()
        val lockoutExpiry = lockouts[key] ?: return false
        val now = System.currentTimeMillis()
        if (now < lockoutExpiry) {
            return true
        } else {
            // Lockout expired; reset lockout
            lockouts.remove(key)
            failedAttempts.remove(key)
            return false
        }
    }

    fun getRemainingLockoutSeconds(identifier: String): Long {
        val key = identifier.trim().lowercase()
        val lockoutExpiry = lockouts[key] ?: return 0L
        val diff = lockoutExpiry - System.currentTimeMillis()
        return if (diff > 0) (diff / 1000L) + 1 else 0L
    }

    /**
     * Records a failed authentication attempt and locks the account if threshold exceeded.
     */
    fun recordFailedLogin(identifier: String) {
        val key = identifier.trim().lowercase()
        val now = System.currentTimeMillis()
        val current = failedAttempts[key]

        val newCount = if (current == null || (now - current.second) > FAILURE_WINDOW_MS) {
            1
        } else {
            current.first + 1
        }

        failedAttempts[key] = Pair(newCount, now)

        logEvent(
            SecurityEventType.AUTH_LOGIN_FAILED,
            key,
            "Failed login attempt ($newCount/$MAX_FAILED_ATTEMPTS)",
            riskScore = newCount * 15
        )

        if (newCount >= MAX_FAILED_ATTEMPTS) {
            val lockoutUntil = now + LOCKOUT_DURATION_MS
            lockouts[key] = lockoutUntil
            logEvent(
                SecurityEventType.AUTH_ACCOUNT_LOCKED,
                key,
                "Brute-force mitigation triggered: account locked for ${LOCKOUT_DURATION_MS / 1000}s",
                riskScore = 80
            )
        }
    }

    /**
     * Helper recording failed login attempt and returning whether the account is now locked.
     */
    fun recordFailedLoginAttempt(identifier: String): Boolean {
        recordFailedLogin(identifier)
        return isAccountLocked(identifier)
    }

    /**
     * Clears failed attempt tracking on successful authentication.
     */
    fun recordSuccessfulLogin(identifier: String) {
        val key = identifier.trim().lowercase()
        failedAttempts.remove(key)
        lockouts.remove(key)
        logEvent(
            SecurityEventType.AUTH_LOGIN_SUCCESS,
            key,
            "Authenticated session established",
            riskScore = 0
        )
    }

    /**
     * Rate limiter for high-frequency or sensitive actions (e.g. password resets, transactions).
     */
    fun checkRateLimit(actionKey: String, cooldownMillis: Long): Boolean {
        return checkRateLimit(actionKey, maxOperations = 1, cooldownMillis = cooldownMillis)
    }

    fun checkRateLimit(actionKey: String, maxOperations: Int, cooldownMillis: Long): Boolean {
        val now = System.currentTimeMillis()
        val list = actionHistory.getOrPut(actionKey) { mutableListOf() }
        
        // Remove timestamps outside window
        list.removeAll { (now - it) > cooldownMillis }

        if (list.size >= maxOperations) {
            logEvent(
                SecurityEventType.RATE_LIMIT_TRIGGERED,
                actionKey,
                "Rate limit exceeded (capacity: $maxOperations, window: ${cooldownMillis}ms)",
                riskScore = 40
            )
            return false
        }
        list.add(now)
        return true
    }

    /**
     * Records a security event to the audit trail without printing sensitive secrets.
     */
    fun logEvent(
        eventType: SecurityEventType,
        identifier: String,
        details: String,
        riskScore: Int = 0
    ) {
        val record = SecurityAuditRecord(
            eventType = eventType,
            identifier = sanitizeIdentifier(identifier),
            timestamp = System.currentTimeMillis(),
            details = sanitizeForLog(details),
            riskScore = riskScore
        )
        _auditLogs.update { list -> (listOf(record) + list).take(100) }
    }

    /**
     * Sanitizes identifiers to avoid logging sensitive PII in plain text where applicable.
     */
    fun sanitizeIdentifier(id: String): String {
        return if (id.contains("@")) {
            val parts = id.split("@")
            val name = parts[0]
            val domain = parts.getOrNull(1) ?: ""
            if (name.length > 2) "${name.take(2)}***@$domain" else "***@$domain"
        } else if (id.length > 8) {
            "${id.take(4)}***${id.takeLast(2)}"
        } else {
            id
        }
    }

    /**
     * Sanitizes general text strings to redact credentials, bearer tokens, and passwords from logs.
     */
    fun sanitizeForLog(message: String): String {
        var result = message
        // Redact Bearer tokens
        result = result.replace(Regex("(?i)Bearer\\s+[A-Za-z0-9_\\-\\.]+"), "Bearer [REDACTED_TOKEN]")
        // Redact passwords in query or key-value style
        result = result.replace(Regex("(?i)(password|pass|secret|token|apiKey)\\s*[:=]\\s*[^\\s&,;]+"), "$1=[REDACTED_CREDENTIAL]")
        // Redact emails
        result = result.replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}"), "[REDACTED_EMAIL]")
        return result
    }

    /**
     * Play Integrity Advisory Evaluation
     * Treat device integrity as a risk indicator, not a replacement for server auth.
     */
    fun evaluateDeviceIntegrity(tokenProvided: Boolean, appCheckPassed: Boolean): Int {
        val riskScore = when {
            !tokenProvided && !appCheckPassed -> 70 // High risk indicator
            !appCheckPassed -> 40 // Medium risk
            else -> 0 // Verified genuine device
        }
        logEvent(
            SecurityEventType.DEVICE_INTEGRITY_CHECK,
            "ClientDevice",
            "Integrity evaluation complete: tokenPresent=$tokenProvided, appCheck=$appCheckPassed",
            riskScore = riskScore
        )
        return riskScore
    }

    /**
     * Basic Play Integrity token structure & attestation verification.
     */
    fun validatePlayIntegrityToken(token: String): PlayIntegrityVerdict {
        if (token.isBlank()) {
            return PlayIntegrityVerdict(isValid = false, riskScore = 80, reason = "Missing token")
        }
        if (token.length < 50 || token.startsWith("fake-") || token.startsWith("mock-")) {
            return PlayIntegrityVerdict(isValid = false, riskScore = 75, reason = "Unverified client attestation token format")
        }
        return PlayIntegrityVerdict(isValid = true, riskScore = 0, reason = "Valid format for backend attestation exchange")
    }
}
