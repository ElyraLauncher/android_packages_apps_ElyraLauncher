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

import android.content.Context
import android.content.pm.ApplicationInfo
import com.android.launcher3.model.data.AppInfo

/**
 * A deterministic, fully local app-drawer classifier.
 *
 * It uses only on-device signals — the developer-declared
 * [ApplicationInfo.category] and package-name hints — so it makes no network call,
 * runs no cloud/provider classifier, and returns the same category for the same
 * input every time. Apps that cannot be classified fall into [ElyraAppCategory.Other].
 *
 * The mapping rule [categoryFor] is pure (an int + a package name in, a category
 * out) so it can be unit tested without a device. Keyword lists are intentionally
 * simple and are meant to be extended over time.
 */
object ElyraAppCategorizer {

    /** How many category buckets an app is placed in: exactly one. */
    private const val ANDROID_CATEGORY_UNDEFINED = ApplicationInfo.CATEGORY_UNDEFINED

    // Package-name substring hints. Ordering of the checks in [categoryFor] is the
    // classifier's precedence and must stay stable for deterministic results.
    private val communicationHints = listOf(
        "dialer", "messaging", "messenger", "whatsapp", "telegram", "signal",
        "contacts", ".phone", "sms", "mms", "gmail", "email", ".mail", "teams",
        "zoom", "skype", "discord", "slack", "viber", ".line.",
    )
    private val financeHints = listOf(
        "bank", "wallet", "finance", ".pay", "paypal", "dana", "gopay", "ovo",
        "crypto", "coinbase", "binance", "budget", "invest", "trading", "bca",
        "mandiri", "bri", "bni", "jenius",
    )
    private val shoppingHints = listOf(
        "shop", "store", "amazon", "ebay", "shopee", "tokopedia", "lazada",
        "bukalapak", "aliexpress", "vending", "blibli",
    )
    private val travelHints = listOf(
        "maps", "navigation", "uber", "grab", "gojek", "lyft", "booking", "agoda",
        "airbnb", "hotel", "flight", "travel", ".trip", "tiket", "waze", "transit",
    )
    private val socialHints = listOf(
        "facebook", "instagram", "twitter", "tiktok", "snapchat", "reddit",
        "pinterest", "tumblr", "social", "threads", "mastodon",
    )
    private val mediaHints = listOf(
        "music", "spotify", "youtube", "netflix", "video", "player", "media",
        "podcast", "photo", "gallery", "camera", "vlc", "soundcloud", "deezer",
        "hulu", "disney", "radio",
    )
    private val gamesHints = listOf("game", "gameloft", "supercell", "miniclip")
    private val productivityHints = listOf(
        "docs", "sheets", "slides", "office", "word", "excel", "powerpoint",
        "drive", "dropbox", "notion", "evernote", ".keep", "calendar", "note",
        ".pdf", "scanner", "translate", "reader",
    )
    private val toolsHints = listOf(
        "calculator", "clock", "file", "manager", "settings", "tool", "flashlight",
        "weather", "compass", "backup", "cleaner", "antivirus", "vpn", "browser",
        "chrome", "firefox", "opera", ".edge", "terminal",
    )
    private val systemPrefixes = listOf(
        "com.android.", "android.", "com.google.android.gms", "com.qualcomm.",
        "com.mediatek.", "org.lineageos.", "com.android.systemui",
    )

    /**
     * The pure classification rule. [applicationCategory] is
     * [ApplicationInfo.category] (or [ApplicationInfo.CATEGORY_UNDEFINED]);
     * [packageName] is the app's package. Deterministic and side-effect free.
     */
    fun categoryFor(applicationCategory: Int, packageName: String): ElyraAppCategory {
        val pkg = packageName.lowercase()

        // 1. High-confidence package hints that should win over the generic
        //    Android-declared category (e.g. a bank app declared "productivity").
        if (pkg.containsAny(communicationHints)) return ElyraAppCategory.Communication
        if (pkg.containsAny(financeHints)) return ElyraAppCategory.Finance
        if (pkg.containsAny(shoppingHints)) return ElyraAppCategory.Shopping
        if (pkg.containsAny(travelHints)) return ElyraAppCategory.Travel

        // 2. The developer-declared application category.
        categoryFromApplicationCategory(applicationCategory)?.let { return it }

        // 3. Package hints for the remaining buckets.
        if (pkg.containsAny(socialHints)) return ElyraAppCategory.Social
        if (pkg.containsAny(mediaHints)) return ElyraAppCategory.Media
        if (pkg.containsAny(gamesHints)) return ElyraAppCategory.Games
        if (pkg.containsAny(productivityHints)) return ElyraAppCategory.Productivity
        if (pkg.containsAny(toolsHints)) return ElyraAppCategory.Tools

        // 4. Framework / OS packages.
        if (systemPrefixes.any { pkg.startsWith(it) }) return ElyraAppCategory.System

        // 5. Catch-all.
        return ElyraAppCategory.Other
    }

    private fun categoryFromApplicationCategory(category: Int): ElyraAppCategory? = when (category) {
        ApplicationInfo.CATEGORY_GAME -> ElyraAppCategory.Games
        ApplicationInfo.CATEGORY_AUDIO,
        ApplicationInfo.CATEGORY_VIDEO,
        ApplicationInfo.CATEGORY_IMAGE,
        -> ElyraAppCategory.Media
        ApplicationInfo.CATEGORY_SOCIAL,
        ApplicationInfo.CATEGORY_NEWS,
        -> ElyraAppCategory.Social
        ApplicationInfo.CATEGORY_MAPS -> ElyraAppCategory.Travel
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> ElyraAppCategory.Productivity
        ApplicationInfo.CATEGORY_ACCESSIBILITY -> ElyraAppCategory.Tools
        else -> null
    }

    private fun String.containsAny(hints: List<String>): Boolean = hints.any { contains(it) }

    // Cache the resolved category per package so repeated drawer rebuilds do not
    // re-query PackageManager. Cleared implicitly with the process; app installs
    // change the package set, and a new package simply misses the cache once.
    private val categoryCache = HashMap<String, ElyraAppCategory>()

    private fun resolveCategory(context: Context, app: AppInfo): ElyraAppCategory {
        val pkg = app.targetPackage ?: return ElyraAppCategory.Other
        categoryCache[pkg]?.let { return it }
        val applicationCategory = try {
            context.packageManager.getApplicationInfo(pkg, 0).category
        } catch (_: Exception) {
            ANDROID_CATEGORY_UNDEFINED
        }
        return categoryFor(applicationCategory, pkg).also { categoryCache[pkg] = it }
    }

    /**
     * Groups [apps] into localized category buckets in the stable
     * [ElyraAppCategory] order, skipping empty categories. The key is the localized
     * category label (usable directly as a drawer folder/section title); apps within
     * a bucket keep their incoming order (already alphabetical from the caller).
     */
    fun categorize(apps: List<AppInfo>, context: Context): LinkedHashMap<String, List<AppInfo>> {
        val byCategory = LinkedHashMap<ElyraAppCategory, MutableList<AppInfo>>()
        apps.forEach { app ->
            byCategory.getOrPut(resolveCategory(context, app)) { mutableListOf() }.add(app)
        }
        val result = LinkedHashMap<String, List<AppInfo>>()
        ElyraAppCategory.entries.forEach { category ->
            val bucket = byCategory[category]
            if (!bucket.isNullOrEmpty()) {
                result[context.getString(category.labelRes)] = bucket
            }
        }
        return result
    }
}
