package com.elyra.launcher.allapps

import kotlin.math.ceil

/** Pure sizing policy for the drawer color popup's compact anchored grid. */
object ElyraColorPickerLayout {
    private const val MIN_COLUMNS = 4
    private const val MAX_COLUMNS = 7

    fun columnCount(
        availableWidth: Int,
        panelPadding: Int,
        cellSize: Int,
        itemCount: Int,
    ): Int {
        if (cellSize <= 0 || itemCount <= 0) return MIN_COLUMNS
        val fitting = ((availableWidth - panelPadding * 2) / cellSize)
            .coerceAtLeast(MIN_COLUMNS)
        return minOf(fitting, MAX_COLUMNS, itemCount)
    }

    fun rowCount(itemCount: Int, columns: Int): Int =
        if (itemCount <= 0 || columns <= 0) 0 else ceil(itemCount / columns.toDouble()).toInt()

    fun popupWidth(columns: Int, panelPadding: Int, cellSize: Int): Int =
        columns.coerceAtLeast(1) * cellSize + panelPadding.coerceAtLeast(0) * 2
}
