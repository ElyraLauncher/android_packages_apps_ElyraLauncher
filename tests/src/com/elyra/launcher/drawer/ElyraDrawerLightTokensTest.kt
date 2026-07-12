package com.elyra.launcher.drawer

import java.io.File
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ElyraDrawerLightTokensTest {
    private val projectRoot: File by lazy {
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "lawnchair/res/values/elyra_tokens.xml").isFile }
    }

    private val lightTokens by lazy {
        File(projectRoot, "lawnchair/res/values/elyra_tokens.xml").readText()
    }
    private val darkTokens by lazy {
        File(projectRoot, "lawnchair/res/values-night/elyra_tokens.xml").readText()
    }

    @Test
    fun lightDrawerForegroundsAreDarkAndReadable() {
        assertTrue(relativeLuminance(color(lightTokens, "elyra_drawer_text_primary")) < 0.08)
        assertTrue(relativeLuminance(color(lightTokens, "elyra_drawer_text_secondary")) < 0.16)
        assertTrue(relativeLuminance(color(lightTokens, "elyra_drawer_app_label")) < 0.06)
    }

    @Test
    fun lightRootAndElevatedSurfacesAreDistinct() {
        val root = color(lightTokens, "elyra_drawer_root_surface")
        assertNotEquals(root, color(lightTokens, "elyra_drawer_suggestion_surface"))
        assertNotEquals(root, color(lightTokens, "elyra_drawer_card_surface"))
        assertNotEquals(root, color(lightTokens, "elyra_drawer_search_surface"))
        assertNotEquals(
            color(lightTokens, "elyra_drawer_search_surface"),
            color(lightTokens, "elyra_drawer_popup_surface"),
        )
    }

    @Test
    fun lightElevatedSurfacesRemainTranslucentAndOutlineIsVisible() {
        listOf(
            "elyra_drawer_suggestion_surface",
            "elyra_drawer_card_surface",
            "elyra_drawer_search_surface",
            "elyra_drawer_button_surface",
            "elyra_drawer_popup_surface",
            "elyra_drawer_index_popup_surface",
        ).forEach { token ->
            assertTrue(token, alpha(color(lightTokens, token)) in 1..254)
        }
        assertTrue(alpha(color(lightTokens, "elyra_drawer_surface_stroke")) >= 0x40)
    }

    @Test
    fun existingDarkDrawerTokensRemainUnchanged() {
        assertEquals(0xFFEAF5F1L, color(darkTokens, "elyra_text_primary"))
        assertEquals(0xFFA9C7BEL, color(darkTokens, "elyra_text_secondary"))
        assertEquals(0x38EAF5F1L, color(darkTokens, "elyra_drawer_surface_stroke"))
        assertEquals(0xB8282C30L, color(darkTokens, "elyra_drawer_control_surface"))
        assertEquals(172, integer(darkTokens, "elyra_drawer_sheet_alpha"))
    }

    private fun color(xml: String, name: String): Long {
        val match = Regex("<color name=\"$name\">#([0-9A-Fa-f]{8})</color>").find(xml)
        requireNotNull(match) { "Missing literal color token: $name" }
        return match.groupValues[1].toLong(16)
    }

    private fun integer(xml: String, name: String): Int {
        val match = Regex("<integer name=\"$name\">(\\d+)</integer>").find(xml)
        requireNotNull(match) { "Missing integer token: $name" }
        return match.groupValues[1].toInt()
    }

    private fun alpha(argb: Long): Int = ((argb ushr 24) and 0xFF).toInt()

    private fun relativeLuminance(argb: Long): Double {
        fun channel(shift: Int): Double {
            val value = ((argb ushr shift) and 0xFF).toDouble() / 255.0
            return if (value <= 0.04045) value / 12.92
            else ((value + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
