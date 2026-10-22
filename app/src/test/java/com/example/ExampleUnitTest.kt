package com.example

import com.example.data.HunterRank
import com.example.data.HunterRepository
import com.example.data.MissionCategory
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testRankCalculation() {
        assertEquals(HunterRank.E, HunterRank.fromLevel(1))
        assertEquals(HunterRank.D, HunterRank.fromLevel(10))
        assertEquals(HunterRank.C, HunterRank.fromLevel(20))
        assertEquals(HunterRank.B, HunterRank.fromLevel(30))
        assertEquals(HunterRank.A, HunterRank.fromLevel(40))
        assertEquals(HunterRank.S, HunterRank.fromLevel(50))
        assertEquals(HunterRank.NATIONAL, HunterRank.fromLevel(75))
        assertEquals(HunterRank.MONARCH, HunterRank.fromLevel(100))
    }

    @Test
    fun testXpAndLevelProgression() {
        val initialLevel = HunterRepository.profile.value.level
        HunterRepository.addXp(500, "Unit Test Quest")
        val newLevel = HunterRepository.profile.value.level
        assertTrue("Level should increase or remain valid", newLevel >= initialLevel)
    }

    @Test
    fun testMissionCreationAndCompletion() {
        val initialCount = HunterRepository.missions.value.size
        HunterRepository.addCustomMission("Test Quest", "Test lore", MissionCategory.PERSONAL, 100, 1, "reps")
        assertEquals(initialCount + 1, HunterRepository.missions.value.size)

        val created = HunterRepository.missions.value.first()
        assertEquals("Test Quest", created.title)
        assertFalse(created.isCompleted)

        HunterRepository.completeMission(created.id)
        val updated = HunterRepository.missions.value.find { it.id == created.id }
        assertTrue(updated?.isCompleted == true)
    }

    @Test
    fun testAuthValidation() {
        // Invalid email
        val invalidEmailRes = HunterRepository.register("invalid_email", "password123", "Tester")
        assertTrue(invalidEmailRes.isFailure)

        // Short password
        val shortPassRes = HunterRepository.register("valid@arise.io", "123", "Tester")
        assertTrue(shortPassRes.isFailure)

        // Valid registration
        val validRes = HunterRepository.register("hunter_test@arise.io", "hunter_pass", "TestHunter")
        assertTrue(validRes.isSuccess)

        // Duplicate registration prevention
        val dupRes = HunterRepository.register("hunter_test@arise.io", "other_pass", "TestHunter2")
        assertTrue(dupRes.isFailure)

        // Sign in with correct credentials
        val loginRes = HunterRepository.signIn("hunter_test@arise.io", "hunter_pass")
        assertTrue(loginRes.isSuccess)

        // Sign in with wrong credentials
        val wrongPassRes = HunterRepository.signIn("hunter_test@arise.io", "wrong_pass")
        assertTrue(wrongPassRes.isFailure)
    }

    @Test
    fun testOnboardingToggle() {
        HunterRepository.setOnboardingCompleted(true)
        assertTrue(HunterRepository.onboardingCompleted.value)
        HunterRepository.resetOnboarding()
        assertFalse(HunterRepository.onboardingCompleted.value)
    }
}
