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

    @Test
    fun translucentDrawerDefaultStaysWallpaperAware() {
        // The shipped default must remain at or below the threshold, or the
        // wallpaper-aware status-bar and label contrast paths silently disengage.
        assertTrue(
            ElyraDrawerLayoutPolicy.DEFAULT_DRAWER_OPACITY
                <= ElyraDrawerLayoutPolicy.WALLPAPER_AWARE_OPACITY_THRESHOLD,
        )
        assertTrue(ElyraDrawerLayoutPolicy.isWallpaperAware(ElyraDrawerLayoutPolicy.DEFAULT_DRAWER_OPACITY))
        assertTrue(ElyraDrawerLayoutPolicy.isWallpaperAware(ElyraDrawerLayoutPolicy.WALLPAPER_AWARE_OPACITY_THRESHOLD))
        assertFalse(ElyraDrawerLayoutPolicy.isWallpaperAware(0.31f))
        assertFalse(ElyraDrawerLayoutPolicy.isWallpaperAware(ElyraDrawerLayoutPolicy.LEGACY_DRAWER_OPACITY))
    }

    @Test
    fun drawerSafeTopUsesLargerInsetAndAddsGapOnce() {
        // Safe top = max(status bar, cutout): a tall/centered cutout wins.
        assertEquals(120, ElyraDrawerLayoutPolicy.safeTopInset(80, 120))
        assertEquals(80, ElyraDrawerLayoutPolicy.safeTopInset(80, 60))
        // A device with no cutout still respects the status-bar inset.
        assertEquals(80, ElyraDrawerLayoutPolicy.safeTopInset(80, 0))
        // The visual gap is added exactly once on top of the safe inset.
        assertEquals(130, ElyraDrawerLayoutPolicy.drawerSheetTopPadding(120, 10))
        assertEquals(90, ElyraDrawerLayoutPolicy.drawerSheetTopPadding(80, 10))
        // Negative/degenerate inputs are clamped, never negative.
        assertEquals(10, ElyraDrawerLayoutPolicy.drawerSheetTopPadding(-5, 10))
    }

    @Test
    fun drawerOpacityMigrationPreservesDeliberateChoices() {
        // Never-written preference: already resolves to the new default, so no migrate.
        assertFalse(ElyraDrawerLayoutPolicy.shouldMigrateLegacyDrawerOpacity(null))
        // Still on the old opaque default: migrate to the translucent default.
        assertTrue(
            ElyraDrawerLayoutPolicy.shouldMigrateLegacyDrawerOpacity(
                ElyraDrawerLayoutPolicy.LEGACY_DRAWER_OPACITY,
            ),
        )
        // Deliberately chosen values are preserved (including the new default).
        assertFalse(ElyraDrawerLayoutPolicy.shouldMigrateLegacyDrawerOpacity(0.5f))
        assertFalse(
            ElyraDrawerLayoutPolicy.shouldMigrateLegacyDrawerOpacity(
                ElyraDrawerLayoutPolicy.DEFAULT_DRAWER_OPACITY,
            ),
        )
    }
}
