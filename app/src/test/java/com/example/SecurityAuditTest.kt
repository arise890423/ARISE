package com.example

import com.example.data.HunterRank
import com.example.data.HunterRepository
import com.example.data.PurchaseResult
import com.example.data.ShopCategory
import com.example.data.ShopItem
import com.example.data.security.SecureStorageManager
import com.example.data.security.SecurityAuditManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SecurityAuditTest {

    @Before
    fun setUp() {
        SecurityAuditManager.resetAllLockouts()
    }

    @Test
    fun testSaltedPbkdf2PasswordHashing() {
        val rawPassword = "MonarchPassword!987"
        val hashRecord1 = SecureStorageManager.hashPassword(rawPassword)
        val hashRecord2 = SecureStorageManager.hashPassword(rawPassword)

        // Ensure salt makes hashes distinct even for identical passwords
        assertNotEquals("Distinct salts must produce distinct hashes", hashRecord1, hashRecord2)

        // Verify valid passwords against their respective records
        assertTrue("Verification must succeed with correct password for record 1",
            SecureStorageManager.verifyPassword(rawPassword, hashRecord1))
        assertTrue("Verification must succeed with correct password for record 2",
            SecureStorageManager.verifyPassword(rawPassword, hashRecord2))

        // Verify incorrect password fails
        assertFalse("Verification must fail with incorrect password",
            SecureStorageManager.verifyPassword("WrongPassword123", hashRecord1))

        // Verify tampered/corrupt hash records fail gracefully
        assertFalse("Corrupt hash format should fail gracefully without exception",
            SecureStorageManager.verifyPassword(rawPassword, "invalid_format_string"))
    }

    @Test
    fun testBruteForceAccountLockout() {
        val testEmail = "jinwoo_target@arise.io"
        val initialLockStatus = SecurityAuditManager.isAccountLocked(testEmail)
        assertFalse("Account should initially not be locked", initialLockStatus)

        // Simulate 4 failed attempts
        repeat(4) {
            val locked = SecurityAuditManager.recordFailedLoginAttempt(testEmail)
            assertFalse("Account should remain unlocked before 5 failed attempts", locked)
        }

        // 5th failed attempt should trigger lockout
        val lockedAtFifth = SecurityAuditManager.recordFailedLoginAttempt(testEmail)
        assertTrue("Account must lock out after 5 failed attempts within time window", lockedAtFifth)
        assertTrue("Account status query must confirm lockout", SecurityAuditManager.isAccountLocked(testEmail))

        // After lockout, subsequent sign-in attempt through repository is rejected
        HunterRepository.register(testEmail, "SecureHunterPass1", "TargetHunter")
        val signInAttempt = HunterRepository.signIn(testEmail, "SecureHunterPass1")
        assertTrue("Sign in must be rejected during lockout", signInAttempt.isFailure)
        assertTrue("Error should mention lockout or rate limiting",
            signInAttempt.exceptionOrNull()?.message?.contains("excessive failed attempts", ignoreCase = true) == true ||
            signInAttempt.exceptionOrNull()?.message?.contains("locked", ignoreCase = true) == true)

        // Reset lockout
        SecurityAuditManager.recordSuccessfulLogin(testEmail)
        assertFalse("Account should be unlocked after successful login reset", SecurityAuditManager.isAccountLocked(testEmail))
    }

    @Test
    fun testRateLimiterEnforcement() {
        val key = "ip_test_action"
        val maxOperations = 5
        val windowMs = 5000L

        // First 5 attempts should succeed
        for (i in 1..maxOperations) {
            val allowed = SecurityAuditManager.checkRateLimit(key, maxOperations, windowMs)
            assertTrue("Operation $i should be permitted under rate limit", allowed)
        }

        // 6th attempt within same window must be blocked
        val exceeded = SecurityAuditManager.checkRateLimit(key, maxOperations, windowMs)
        assertFalse("Operation exceeding max rate limit must be rejected", exceeded)
    }

    @Test
    fun testXpRewardBoundaryAndTamperProtection() {
        val initialXp = HunterRepository.profile.value.currentXp

        // Negative XP award attempt must be rejected
        HunterRepository.addXp(-500, "Tampered Cheat Negative XP")
        assertEquals("Negative XP award must not affect current XP", initialXp, HunterRepository.profile.value.currentXp)

        // Zero XP award attempt must be rejected
        HunterRepository.addXp(0, "Zero XP Attack")
        assertEquals("Zero XP award must not affect current XP", initialXp, HunterRepository.profile.value.currentXp)

        // Anomalous XP (> 10000) award attempt must be rejected
        HunterRepository.addXp(9999999, "Arbitrary Reward Manipulation")
        assertEquals("Anomalous high XP awards must be rejected to prevent stat inflation",
            initialXp, HunterRepository.profile.value.currentXp)

        // Legitimate XP within 1..10000 should be accepted
        HunterRepository.addXp(250, "Valid Daily Dungeon Completion")
        assertTrue("Valid XP should increase current XP", HunterRepository.profile.value.currentXp > initialXp)
    }

    @Test
    fun testShopBalanceAndDuplicatePurchaseIntegrity() {
        // Find or create an item in the shop
        val testItem = ShopItem(
            id = "test_shadow_monarch_title",
            name = "Shadow Monarch Title",
            description = "Legendary Title",
            category = ShopCategory.TITLES,
            costGold = 9999999, // Unaffordable
            costCrystals = 0,
            requiredRank = HunterRank.E
        )

        // 1. Balance check: Cannot buy with insufficient funds
        val insufficientFundsResult = HunterRepository.buyShopItem(testItem)
        assertTrue("Purchase must fail if balance is insufficient", insufficientFundsResult is PurchaseResult.Error)

        // 2. Prevent negative/corrupt price exploitation
        val negativePriceItem = ShopItem(
            id = "exploit_negative_item",
            name = "Exploit Negative Gold Item",
            description = "Exploit",
            category = ShopCategory.AVATARS,
            costGold = -500, // Negative price exploit
            costCrystals = 0,
            requiredRank = HunterRank.E
        )
        val negativeResult = HunterRepository.buyShopItem(negativePriceItem)
        assertTrue("Negative price items must be rejected", negativeResult is PurchaseResult.Error)
    }

    @Test
    fun testSensitiveLogSanitization() {
        val rawMessage = "Auth failed for user hunter_secret@arise.io with token Bearer eyJhbGciOiJIUzI1NiJ9.test and pass=SuperSecretPass123"
        val sanitized = SecurityAuditManager.sanitizeForLog(rawMessage)

        assertFalse("Sanitized log must not expose bearer token payload",
            sanitized.contains("eyJhbGciOiJIUzI1NiJ9"))
        assertFalse("Sanitized log must not contain plaintext password value",
            sanitized.contains("SuperSecretPass123"))
        assertTrue("Sanitized log should mask email or credentials",
            sanitized.contains("[REDACTED_TOKEN]") || sanitized.contains("[REDACTED_CREDENTIAL]"))
    }

    @Test
    fun testPlayIntegrityAttestationValidation() {
        // Empty token should fail integrity check
        val emptyResult = SecurityAuditManager.validatePlayIntegrityToken("")
        assertFalse("Empty Play Integrity token must fail attestation", emptyResult.isValid)

        // Short mock token should fail
        val mockResult = SecurityAuditManager.validatePlayIntegrityToken("fake-token-123")
        assertFalse("Mock or invalid token must fail verification", mockResult.isValid)
    }
}
