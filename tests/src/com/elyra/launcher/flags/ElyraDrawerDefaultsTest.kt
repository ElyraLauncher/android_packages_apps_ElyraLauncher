package com.elyra.launcher.flags

import com.elyra.launcher.allapps.ElyraBottomSearch
import com.elyra.launcher.drawer.ElyraDrawerLayoutPolicy
import com.elyra.launcher.drawer.ElyraDrawerSuggestions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ElyraDrawerDefaultsTest {

    @Test
    fun coreDrawerPresentationShipsEnabled() {
        assertTrue(ElyraFeatureFlags.defaultFor(ElyraFlag.BottomSearch))
        assertTrue(ElyraFeatureFlags.defaultFor(ElyraFlag.DrawerCategories))
        assertTrue(ElyraFeatureFlags.defaultFor(ElyraFlag.DrawerSuggestions))
        assertTrue(ElyraFeatureFlags.defaultFor(ElyraFlag.AzRail))
        assertTrue(ElyraFeatureFlags.defaultFor(ElyraFlag.DrawerColorSearch))
        assertTrue(ElyraBottomSearch.localOnlyDrawerSearchEnabled())
        assertTrue(ElyraBottomSearch.shouldAcceptDrawerResult("mail", "mail", false))
        assertTrue(ElyraBottomSearch.shouldAcceptDrawerResult("", "", true))
        assertFalse(ElyraBottomSearch.shouldAcceptDrawerResult("old", "new", false))
        assertFalse(ElyraBottomSearch.shouldAcceptDrawerResult("", "", false))
    }

    @Test
    fun structuralDrawerPolicyUsesMeasuredInputs() {
        assertEquals(4, ElyraDrawerLayoutPolicy.suggestionCount(4))
        assertEquals(5, ElyraDrawerLayoutPolicy.suggestionCount(5))
        assertEquals(98, ElyraDrawerLayoutPolicy.bottomContentPadding(
            base = 20,
            controlsHeight = 52,
            controlsBottomInset = 10,
            spacing = 16,
        ))
        assertEquals('#', ElyraDrawerLayoutPolicy.INDEX_LABELS.first())
        assertEquals(0, ElyraDrawerLayoutPolicy.railIndex(0f, 432))
        assertEquals(13, ElyraDrawerLayoutPolicy.railIndex(216f, 432))
        assertEquals(26, ElyraDrawerLayoutPolicy.railIndex(432f, 432))
    }

    @Test
    fun suggestionRankingIsUniqueAndBounded() {
        val candidates = (0..7).map { index ->
            ElyraDrawerSuggestions.Candidate(
                item = "app" + (index % 6),
                score = index.toLong(),
                tiebreak = index.toString(),
            )
        }
        val result = ElyraDrawerSuggestions.rank(candidates, limit = 10)
        assertEquals(5, result.size)
        assertEquals(result.size, result.distinct().size)
    }

    @Test
    fun suggestionSignalsFavorRecentFrequentAndNewApps() {
        val now = 1_000_000_000L
        val hour = 60L * 60L * 1_000L
        val baseline = ElyraDrawerSuggestions.score(now, 0L, 0L, now - 96L * hour, null)
        val recent = ElyraDrawerSuggestions.score(now, now - hour, 1L, 0L, null)
        val frequent = ElyraDrawerSuggestions.score(now, 0L, 20L, 0L, null)
        val newlyInstalled = ElyraDrawerSuggestions.score(now, 0L, 0L, now - hour, null)
        val expiredInstall = ElyraDrawerSuggestions.score(now, 0L, 0L, now - 73L * hour, null)

        assertTrue(recent > baseline)
        assertTrue(frequent > baseline)
        assertTrue(newlyInstalled > baseline)
        assertEquals(baseline, expiredInstall)
    }

    @Test
    fun networkResultsRemainOptIn() {
        assertFalse(ElyraFeatureFlags.defaultFor(ElyraFlag.DrawerWebResults))
    }
}
