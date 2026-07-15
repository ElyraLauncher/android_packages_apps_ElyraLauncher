/*
 * Copyright 2026, The Elyra Launcher Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.android.launcher3.allapps

import com.elyra.launcher.drawer.ElyraDrawerModelCoordinator
import org.junit.Assert.assertEquals
import org.junit.Test

class ElyraDrawerModelCoordinatorTest {
    private fun signature(label: String, icon: Int = 1) =
        ElyraDrawerModelCoordinator.AppSignature("pkg/.Main#user", "pkg", label, icon)

    @Test
    fun installInvalidatesOnlyInstalledComponent() {
        val next = mapOf("pkg/.Main#user" to signature("App"))
        assertEquals(next.keys, ElyraDrawerModelCoordinator.changedKeys(emptyMap(), next))
    }

    @Test
    fun metadataUpdateInvalidatesAffectedComponent() {
        val old = mapOf("pkg/.Main#user" to signature("Old"))
        val next = mapOf("pkg/.Main#user" to signature("New", icon = 2))
        assertEquals(next.keys, ElyraDrawerModelCoordinator.changedKeys(old, next))
    }

    @Test
    fun removalInvalidatesOnlyRemovedComponent() {
        val old = mapOf("pkg/.Main#user" to signature("App"))
        assertEquals(old.keys, ElyraDrawerModelCoordinator.changedKeys(old, emptyMap()))
    }

    @Test
    fun identicalSnapshotDoesNotRequestRebind() {
        val snapshot = mapOf("pkg/.Main#user" to signature("App"))
        assertEquals(emptySet<String>(), ElyraDrawerModelCoordinator.changedKeys(snapshot, snapshot))
    }
}
