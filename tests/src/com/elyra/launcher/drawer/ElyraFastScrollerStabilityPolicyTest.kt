/* Copyright (C) 2026 The Elyra Project */
package com.elyra.launcher.drawer

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ElyraFastScrollerStabilityPolicyTest {
    private val projectRoot: File by lazy {
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "src/com/android/launcher3/LauncherRootView.java").isFile }
    }

    @Test
    fun compactFrameUsesExplicitNullSafePendingOwnership() {
        val fastScroller = source(
            "src/com/android/launcher3/views/RecyclerViewFastScroller.java",
        )
        val frameApply = fastScroller.substringAfter("private void applyPendingCompactUpdate()")
            .substringBefore("public void endFastScrolling()")

        assertTrue(frameApply.contains("mCompactUpdateState.takeForFrame()"))
        assertTrue(frameApply.contains("request == null"))
        assertTrue(frameApply.contains("labelsEqual(sectionName, mPopupSectionName)"))
        assertFalse(frameApply.contains("contentEquals("))
        assertFalse(frameApply.contains("requestLayout("))
        assertFalse(frameApply.contains("setPadding("))
        assertFalse(frameApply.contains("setLayoutParams("))
    }

    @Test
    fun bottomInsetRefreshIsChangeDrivenAndDeferred() {
        val searchInput = source(
            "lawnchair/src/app/lawnchair/allapps/AllAppsSearchInput.kt",
        )
        val allApps = source(
            "src/com/android/launcher3/allapps/ActivityAllAppsContainerView.java",
        )
        val update = searchInput.substringAfter("private fun updateBottomContentInset()")
            .substringBefore("private fun updateActionButtonVisibility()")
        val refresh = allApps.substringAfter("public void refreshElyraBottomContentInsets()")
            .substringBefore("private int getBottomControlsHeightForPadding()")
        val padding = allApps.substringAfter("void applyPadding()")
            .substringBefore("private boolean isWork()")

        assertTrue(update.contains("if (appsView.setElyraBottomControlsLayout("))
        assertTrue(refresh.contains("post(() ->"))
        assertTrue(padding.contains("getPaddingBottom() != effectiveBottomPadding"))
        assertTrue(padding.contains("mRecyclerView.setPadding("))
    }

    private fun source(path: String): String = File(projectRoot, path).readText()
}
