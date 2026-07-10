package com.elyra.launcher.flags

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
    fun networkResultsRemainOptIn() {
        assertFalse(ElyraFeatureFlags.defaultFor(ElyraFlag.DrawerWebResults))
    }
}
