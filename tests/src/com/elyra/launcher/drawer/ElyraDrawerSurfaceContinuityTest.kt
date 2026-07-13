/*
 * Copyright 2026, The Elyra Launcher Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.elyra.launcher.drawer

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ElyraDrawerSurfaceContinuityTest {
    private val projectRoot: File by lazy {
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "src/com/android/launcher3/LauncherRootView.java").isFile }
    }

    @Test
    fun systemTopShadowIsClippedBeforeTheTranslucentSheet() {
        val rootView = source("src/com/android/launcher3/LauncherRootView.java")
        val sysUiScrim = source("src/com/android/launcher3/graphics/SysUiScrim.java")

        assertTrue(rootView.contains("getElyraDrawerSheetTopForSystemScrim()"))
        assertTrue(rootView.contains("mSysUiScrim.draw(canvas, topScrimClipBottom)"))
        assertTrue(
            sysUiScrim.contains("topScrimClipBottom != Float.POSITIVE_INFINITY"),
        )
        assertTrue(sysUiScrim.contains("canvas.clipRect("))
    }

    @Test
    fun modernSheetSkipsLegacyHeaderRootOverlay() {
        val allApps = source(
            "src/com/android/launcher3/allapps/ActivityAllAppsContainerView.java",
        )
        val rootDraw = allApps.substringAfter("if (hasBottomSheet) {")
            .substringBefore("if (DEBUG_HEADER_PROTECTION)")

        assertTrue(rootDraw.contains("canvas.drawPath(mTmpPath, mHeaderPaint)"))
        assertTrue(rootDraw.contains("if (isElyraBottomSearch())"))
        assertTrue(rootDraw.contains("return;"))
        assertFalse(rootDraw.contains("canvas.drawRect("))
    }

    @Test
    fun safeInsetRemainsPaddingWithoutIndependentBackground() {
        val allApps = source(
            "src/com/android/launcher3/allapps/ActivityAllAppsContainerView.java",
        )
        val insetBlock = allApps.substringAfter("public void setInsets(Rect insets)")
            .substringBefore("InsettableFrameLayout.dispatchInsets(this, insets)")

        assertTrue(insetBlock.contains("drawerSheetTopPadding("))
        assertTrue(insetBlock.contains("setPadding("))
        assertFalse(insetBlock.contains("setBackground"))
    }

    private fun source(path: String): String = File(projectRoot, path).readText()
}
