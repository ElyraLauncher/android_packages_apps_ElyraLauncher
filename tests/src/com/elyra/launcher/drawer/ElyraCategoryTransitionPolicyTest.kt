/*
 * Copyright 2026, The Elyra Launcher Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.elyra.launcher.drawer

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ElyraCategoryTransitionPolicyTest {
    private val projectRoot: File by lazy {
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "src/com/android/launcher3/LauncherRootView.java").isFile }
    }

    @Test
    fun categoryTransitionHasNoBlankMorphOrOverlayRuntimePath() {
        val controller = controllerSource()

        listOf(
            "MorphView",
            "getOverlay()",
            "RectF",
            "Canvas",
            "setLayoutParams(",
            "requestLayout(",
        ).forEach { forbidden -> assertFalse(forbidden, controller.contains(forbidden)) }
    }

    @Test
    fun detailIsCommittedAndMeasuredBeforeOpening() {
        val controller = controllerSource()
        val commit = controller.indexOf("beginAdapterCommit(rv, generation")
        val preDraw = controller.indexOf("waitForValidPreDraw(rv, generation")
        val prepared = controller.indexOf("mState.markPrepared()")

        assertTrue(commit >= 0)
        assertTrue(preDraw > commit)
        assertTrue(prepared > commit)
        assertTrue(controller.contains("rv.getWidth() <= 0 || rv.getHeight() <= 0"))
    }

    @Test
    fun cleanupOwnsPendingCallbacksAndAllTransformProperties() {
        val controller = controllerSource()
        val cleanup = controller.substringAfter("private void cleanupTransitionViews()")
            .substringBefore("private void clearPendingPreparation()")
        val pendingCleanup = controller.substringAfter("private void clearPendingPreparation()")
            .substringBefore("private void unregisterPendingAdapterObserver()")

        assertTrue(cleanup.contains("clearPendingPreparation()"))
        assertTrue(cleanup.contains("restoreViewProperties(rv)"))
        assertTrue(cleanup.contains("rv.setEnabled(true)"))
        assertTrue(pendingCleanup.contains("removeCallbacks"))
        assertTrue(pendingCleanup.contains("removePendingPreDrawListener()"))
    }

    private fun controllerSource(): String = File(
        projectRoot,
        "src/com/android/launcher3/allapps/ElyraCategoryMotionController.java",
    ).readText()
}
