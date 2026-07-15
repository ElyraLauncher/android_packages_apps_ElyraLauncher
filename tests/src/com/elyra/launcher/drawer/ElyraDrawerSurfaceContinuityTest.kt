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
    fun systemUiShadowsRemainOutsideTheCompleteTranslucentSheet() {
        val rootView = source("src/com/android/launcher3/LauncherRootView.java")
        val sysUiScrim = source("src/com/android/launcher3/graphics/SysUiScrim.java")

        assertTrue(rootView.contains("getElyraDrawerSheetBoundsForSystemScrim("))
        assertTrue(rootView.contains("mSysUiScrim.draw(canvas, drawerSheetBounds)"))
        assertTrue(
            sysUiScrim.contains("excludedContentSurface != null"),
        )
        assertTrue(sysUiScrim.contains("canvas.clipRect("))
        assertTrue(sysUiScrim.contains("canvas.clipOutRect(excludedContentSurface)"))
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
    fun rootPaintAndSystemExclusionShareCompleteSheetBounds() {
        val allApps = source(
            "src/com/android/launcher3/allapps/ActivityAllAppsContainerView.java",
        )
        val boundsMethod = allApps.substringAfter(
            "private void getBottomSheetSurfaceBounds(",
        ).substringBefore("public boolean setElyraBottomControlsLayout(")
        val rootDraw = allApps.substringAfter(
            "public void drawOnScrimWithScaleAndBottomOffset(",
        ).substringBefore("if (isElyraBottomSearch())")

        assertTrue(boundsMethod.contains("panel.getTop()"))
        assertTrue(boundsMethod.contains("panel.getBottom()"))
        assertTrue(boundsMethod.contains("bottomOffsetPx"))
        assertTrue(rootDraw.contains("getBottomSheetSurfaceBounds("))
        assertTrue(rootDraw.contains("mTmpPath.addRoundRect(mTmpRectF"))
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

    @Test
    fun modernSheetUsesTheSameRootTokenInBothThemes() {
        val allApps = source(
            "src/com/android/launcher3/allapps/ActivityAllAppsContainerView.java",
        )
        val resolver = allApps.substringAfter("private int resolveElyraDrawerRootColor()")
            .substringBefore("protected boolean isSearchBarFloating()")

        assertTrue(resolver.contains("R.color.elyra_drawer_root_surface"))
        assertFalse(resolver.contains("Utilities.isDarkTheme"))
        assertTrue(resolver.contains("ColorUtils.setAlphaComponent(color, 255)"))
    }

    @Test
    fun modernSheetDoesNotPaintAnIndependentBottomRootBand() {
        val allApps = source(
            "src/com/android/launcher3/allapps/ActivityAllAppsContainerView.java",
        )
        val dispatchDraw = allApps.substringAfter(
            "protected void dispatchDraw(Canvas canvas)",
        ).substringBefore("protected void updateSearchResultsVisibility()")

        assertTrue(dispatchDraw.contains("!isElyraBottomSearch()"))
        assertTrue(dispatchDraw.contains("mNavBarScrimPaint"))
    }

    @Test
    fun drawerContentContainersRemainTransparent() {
        listOf(
            "res/layout/all_apps.xml",
            "res/layout/all_apps_content.xml",
            "res/layout/all_apps_rv_layout.xml",
            "res/layout/search_results_rv_layout.xml",
        ).forEach { path ->
            assertFalse(path, source(path).contains("android:background="))
        }
    }

    @Test
    fun modernStateDoesNotStackAFullScreenWorkspaceScrim() {
        val allAppsState = source(
            "quickstep/src/com/android/launcher3/uioverrides/states/AllAppsState.java",
        )
        val scrimPolicy = allAppsState.substringAfter(
            "public int getWorkspaceScrimColor(Launcher launcher)",
        )

        assertTrue(scrimPolicy.contains("ElyraBottomSearch.isEnabled(launcher)"))
        assertTrue(scrimPolicy.contains("Color.TRANSPARENT"))
    }

    @Test
    fun frostedPolicyReusesSystemDepthBlurAndKeepsOneRootPaint() {
        val allApps = source(
            "src/com/android/launcher3/allapps/ActivityAllAppsContainerView.java",
        )
        val frostedPolicy = allApps.substringAfter(
            "private boolean isElyraSystemBlurAvailable()",
        ).substringBefore("protected boolean isSearchBarFloating()")

        assertTrue(frostedPolicy.contains("BlurUtils.supportsBlursOnWindows()"))
        assertTrue(frostedPolicy.contains("CrossWindowBlurListeners.getInstance()"))
        assertTrue(frostedPolicy.contains("mElyraBottomSheetMaxAlpha = effectiveAlpha"))
        assertFalse(frostedPolicy.contains("setBackgroundBlurRadius"))
        assertFalse(frostedPolicy.contains("setBackground"))
    }

    private fun source(path: String): String = File(projectRoot, path).readText()
}
