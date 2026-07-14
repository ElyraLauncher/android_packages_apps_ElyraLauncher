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
        val level1 = color(lightTokens, "elyra_drawer_surface_level_1")
        val level2 = color(lightTokens, "elyra_drawer_surface_level_2")
        assertNotEquals(root, level1)
        assertNotEquals(level1, level2)
        level1Aliases.forEach { assertEquals(it, level1, color(lightTokens, it)) }
        level2Aliases.forEach { assertEquals(it, level2, color(lightTokens, it)) }
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
        assertTrue(alpha(color(lightTokens, "elyra_drawer_outline_level_1")) in 0x30..0x70)
        assertTrue(alpha(color(lightTokens, "elyra_drawer_outline_level_2")) in 0x30..0x70)
        assertTrue(alpha(color(lightTokens, "elyra_drawer_surface_level_1")) in 0xC0..0xE8)
        assertTrue(alpha(color(lightTokens, "elyra_drawer_surface_level_2")) in 0xD0..0xF4)
        assertOutlineAliases(lightTokens)
    }

    @Test
    fun darkCardTokensRemainTranslucentLayeredAndReadable() {
        val root = color(darkTokens, "elyra_drawer_root_surface")
        val level1 = color(darkTokens, "elyra_drawer_surface_level_1")
        val level2 = color(darkTokens, "elyra_drawer_surface_level_2")
        assertNotEquals(root, level1)
        assertNotEquals(level1, level2)
        level1Aliases.forEach { assertEquals(it, level1, color(darkTokens, it)) }
        level2Aliases.forEach { assertEquals(it, level2, color(darkTokens, it)) }
        assertTrue(alpha(level1) in 0xA0..0xE0)
        assertTrue(alpha(level2) in 0xD0..0xF4)
        assertTrue(alpha(color(darkTokens, "elyra_drawer_outline_level_1")) in 0x30..0x70)
        assertTrue(alpha(color(darkTokens, "elyra_drawer_outline_level_2")) in 0x30..0x70)
        assertTrue(relativeLuminance(color(darkTokens, "elyra_drawer_text_primary")) > 0.75)
        assertTrue(relativeLuminance(color(darkTokens, "elyra_drawer_text_secondary")) > 0.4)
        assertOutlineAliases(darkTokens)
    }

    @Test
    fun unrelatedDarkFoundationTokensRemainUnchanged() {
        assertEquals(0xFFEAF5F1L, color(darkTokens, "elyra_text_primary"))
        assertEquals(0xFFA9C7BEL, color(darkTokens, "elyra_text_secondary"))
        assertEquals(
            color(darkTokens, "elyra_drawer_outline_level_1"),
            color(darkTokens, "elyra_drawer_surface_stroke"),
        )
        assertEquals(
            color(darkTokens, "elyra_drawer_surface_level_1"),
            color(darkTokens, "elyra_drawer_control_surface"),
        )
        assertEquals(172, integer(darkTokens, "elyra_drawer_sheet_alpha"))
    }

    private fun color(xml: String, name: String): Long {
        val match = Regex("<color name=\"$name\">#([0-9A-Fa-f]{8})</color>").find(xml)
        if (match != null) return match.groupValues[1].toLong(16)
        val alias = Regex("<color name=\"$name\">@color/([^<]+)</color>").find(xml)
        requireNotNull(alias) { "Missing color token: $name" }
        return color(xml, alias.groupValues[1])
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

    private fun assertOutlineAliases(xml: String) {
        val level1 = color(xml, "elyra_drawer_outline_level_1")
        assertEquals(level1, color(xml, "elyra_drawer_card_outline"))
        assertEquals(level1, color(xml, "elyra_drawer_surface_stroke"))
    }

    private val level1Aliases = listOf(
        "elyra_drawer_capsule_surface",
        "elyra_drawer_elevated_card_surface",
        "elyra_drawer_suggestion_surface",
        "elyra_drawer_card_surface",
        "elyra_drawer_search_surface",
        "elyra_drawer_button_surface",
        "elyra_drawer_control_surface",
    )

    private val level2Aliases = listOf(
        "elyra_drawer_selected_pill_surface",
        "elyra_drawer_popup_surface",
        "elyra_drawer_index_popup_surface",
    )
}
