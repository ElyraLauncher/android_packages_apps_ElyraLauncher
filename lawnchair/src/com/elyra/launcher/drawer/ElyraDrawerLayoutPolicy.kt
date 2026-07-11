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
}
