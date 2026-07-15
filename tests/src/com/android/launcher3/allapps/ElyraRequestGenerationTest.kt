/*
 * Copyright 2026, The Elyra Launcher Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.android.launcher3.allapps

import com.elyra.launcher.drawer.ElyraRequestGeneration
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ElyraRequestGenerationTest {
    @Test
    fun newestRequestWins() {
        val generation = ElyraRequestGeneration()
        val old = generation.next()
        val newest = generation.next()

        assertFalse(generation.isCurrent(old))
        assertTrue(generation.isCurrent(newest))
    }

    @Test
    fun cancellationRejectsPendingResult() {
        val generation = ElyraRequestGeneration()
        val pending = generation.next()

        generation.cancel()

        assertFalse(generation.isCurrent(pending))
    }
}
