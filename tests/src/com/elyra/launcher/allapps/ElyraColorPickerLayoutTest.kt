package com.elyra.launcher.allapps

import org.junit.Assert.assertEquals
import org.junit.Test

class ElyraColorPickerLayoutTest {
    @Test
    fun compactGridAdaptsToAvailableWidth() {
        assertEquals(4, ElyraColorPickerLayout.columnCount(216, 12, 48, 14))
        assertEquals(6, ElyraColorPickerLayout.columnCount(312, 12, 48, 14))
        assertEquals(7, ElyraColorPickerLayout.columnCount(400, 12, 48, 14))
    }

    @Test
    fun fourteenActionsUseOnlyRequiredRows() {
        assertEquals(4, ElyraColorPickerLayout.rowCount(14, 4))
        assertEquals(3, ElyraColorPickerLayout.rowCount(14, 6))
        assertEquals(2, ElyraColorPickerLayout.rowCount(14, 7))
    }

    @Test
    fun popupWidthIncludesSharedPanelPadding() {
        assertEquals(312, ElyraColorPickerLayout.popupWidth(6, 12, 48))
    }
}
