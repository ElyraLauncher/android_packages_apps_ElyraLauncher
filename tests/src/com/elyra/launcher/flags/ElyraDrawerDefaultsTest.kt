package com.elyra.launcher.flags

import com.elyra.launcher.drawer.ElyraDrawerLayoutPolicy
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
    fun networkResultsRemainOptIn() {
        assertFalse(ElyraFeatureFlags.defaultFor(ElyraFlag.DrawerWebResults))
    }
}
