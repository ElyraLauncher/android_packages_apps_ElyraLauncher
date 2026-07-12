/*
 * Copyright 2026, The Elyra Launcher Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.elyra.launcher.drawer

/** Pure measurement policy shared by the real drawer views and unit tests. */
object ElyraDrawerLayoutPolicy {

    const val INDEX_LABELS = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ"

    /**
     * Previous opaque drawer-opacity default, retained only so the one-time
     * migration can recognise users who never left the old default.
     */
    const val LEGACY_DRAWER_OPACITY = 1f

    /**
     * Shipped translucent drawer-opacity default. Low enough that the existing
     * launcher wallpaper blur/depth stays perceptible behind the rounded drawer
     * sheet, and (crucially) <= [WALLPAPER_AWARE_OPACITY_THRESHOLD] so the
     * wallpaper-aware contrast paths engage.
     */
    const val DEFAULT_DRAWER_OPACITY = 0.25f

    /**
     * At or below this drawer opacity the drawer is treated as wallpaper-aware:
     * status-bar icon contrast follows the wallpaper and app labels switch to the
     * alternate high-contrast color. The shipped default must stay at or below
     * this value or those paths silently stop engaging.
     */
    const val WALLPAPER_AWARE_OPACITY_THRESHOLD = 0.3f

    /** Four cells on compact grids, five when the real All Apps grid has room. */
    @JvmStatic
    fun suggestionCount(columns: Int): Int = if (columns >= 5) 5 else 4

    /**
     * Clearance below scrollable content: profile-specific base, measured controls,
     * the controls' actual bottom inset (navigation or IME plus its anchor gap), and spacing.
     */
    @JvmStatic
    fun bottomContentPadding(
        base: Int,
        controlsHeight: Int,
        controlsBottomInset: Int,
        spacing: Int,
    ): Int = base.coerceAtLeast(0) +
        controlsHeight.coerceAtLeast(0) +
        controlsBottomInset.coerceAtLeast(0) +
        spacing.coerceAtLeast(0)

    /** Resolves a rail-local Y coordinate directly to # or an A-Z slot. */
    @JvmStatic
    fun railIndex(touchY: Float, trackHeight: Int): Int {
        if (trackHeight <= 0) return 0
        val progress = (touchY / trackHeight).coerceIn(0f, 0.9999f)
        return (progress * INDEX_LABELS.length).toInt()
            .coerceIn(0, INDEX_LABELS.lastIndex)
    }

    /**
     * Whether [opacity] is translucent enough to enable the wallpaper-aware
     * contrast handling (status-bar icons and drawer label color).
     */
    @JvmStatic
    fun isWallpaperAware(opacity: Float): Boolean =
        opacity <= WALLPAPER_AWARE_OPACITY_THRESHOLD

    /**
     * One-time migration decision for the translucent drawer default. A user who
     * is still on the previous opaque default migrates to the new default; any
     * deliberately chosen value is preserved. [storedOpacity] is null when the
     * preference was never written (a fresh value already resolves to the new
     * default, so no migration is needed).
     */
    @JvmStatic
    fun shouldMigrateLegacyDrawerOpacity(storedOpacity: Float?): Boolean =
        storedOpacity != null && storedOpacity == LEGACY_DRAWER_OPACITY
}
