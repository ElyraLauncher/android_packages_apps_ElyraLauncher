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

import android.content.pm.ApplicationInfo

/**
 * The deterministic, fully local app classifier.
 *
 * It combines four on-device signals in a fixed precedence: package prefixes,
 * package-name/label keyword hints, and — as a fallback — the developer-declared
 * [ApplicationInfo.category]. There is no network call, no cloud classifier, and no
 * Play Store category API at runtime; the same input always yields the same category.
 *
 * Precedence (first matching rule wins):
 *  1. System (launcher / settings / systemui / installer / updater / …)
 *  2. Google (com.google.* packages, Chrome, "google")
 *  3. Games
 *  4. Communication, Social, Music & Audio, Photo & Video, Media
 *  5. Finance, Shopping, Travel, Education, Health
 *  6. Productivity, Tools
 *  7. Android-declared application category
 *  8. [ElyraAppCategory.Other]
 *
 * [classify] is pure so it can be unit tested without a device. The rule lists live
 * here (not scattered across UI classes) and are meant to be extended over time.
 */
object ElyraAppCategoryClassifier {

    // Ordered rules; more specific / higher-priority categories come first.
    private val rules: List<ElyraCategoryRule> = listOf(
        ElyraCategoryRule(
            ElyraAppCategory.System,
            hints = listOf(
                "systemui", "packageinstaller", "package.installer", "permissioncontroller",
                "setupwizard", "provision", "carrierconfig", "wallpaperpicker", "themepicker",
                "system update", "software update", "device settings",
            ),
            packagePrefixes = listOf(
                "com.android.systemui", "com.android.settings", "com.android.launcher",
                "com.android.packageinstaller", "com.google.android.packageinstaller",
                "com.android.wallpaper", "com.android.provision", "com.android.emergency",
                "com.android.traceur", "com.android.intentresolver",
            ),
        ),
        ElyraCategoryRule(
            ElyraAppCategory.Google,
            hints = listOf("google"),
            packagePrefixes = listOf("com.google.", "com.android.chrome", "com.chrome."),
        ),
        ElyraCategoryRule(
            ElyraAppCategory.Games,
            hints = listOf(
                "mobile.legends", "mlbb", "tencent.ig", "pubg", "bgmi", "freefire", "garena",
                "roblox", "minecraft", "mojang", "supercell", "clash", "brawlstars", "genshin",
                "honkai", "hoyoverse", "mihoyo", "riotgames", "valorant", "codm", "callofduty",
                "fifa", "efootball", ".pes", "asphalt", "8ballpool", "candycrush", "game",
            ),
            packagePrefixes = listOf("com.gameloft.", "com.miniclip.", "com.king."),
        ),
        ElyraCategoryRule(
            ElyraAppCategory.Communication,
            hints = listOf(
                "whatsapp", "telegram", "messenger", "com.facebook.orca", "discord", "signal",
                ".line.", "wechat", "skype", "truecaller", "viber", "dialer", "incallui",
                "contacts", "com.android.mms", "messaging", ".sms.", "imo", "botim", "outlook",
                "email", "yahoomail",
            ),
        ),
        ElyraCategoryRule(
            ElyraAppCategory.Social,
            hints = listOf(
                "instagram", "com.facebook.katana", "facebook.lite", "threads", "twitter",
                "com.twitter", "tiktok", "musically", "snapchat", "pinterest", "reddit",
                "tumblr", "kwai", "likee", "snackvideo", "social", "mastodon", "bluesky", "quora",
            ),
        ),
        ElyraCategoryRule(
            ElyraAppCategory.MusicAudio,
            hints = listOf(
                "spotify", "joox", "soundcloud", "shazam", "deezer", "resso", "music", "audio",
                "podcast", "equalizer",
            ),
        ),
        ElyraCategoryRule(
            ElyraAppCategory.PhotoVideo,
            hints = listOf(
                "camera", "gallery", "snapseed", "lightroom", "capcut", "canva", "picsart",
                "kinemaster", "inshot", "vsco", "filmora", "gcam", "photo", "video", "editor",
            ),
        ),
        ElyraCategoryRule(
            ElyraAppCategory.Media,
            hints = listOf(
                "netflix", "primevideo", "com.amazon.avod", "disney", "hotstar", "viu", "vidio",
                "mxplayer", "com.mxtech", "vlc", "iptv", "streaming", "player",
            ),
        ),
        ElyraCategoryRule(
            ElyraAppCategory.Finance,
            hints = listOf(
                "brimo", "bankmandiri", "mandiri", "livin", "bca", "bni.", "jago", "seabank",
                "jenius", "dana", "ovo", "gopay", "linkaja", "shopeepay", "paypal", "bank",
                "wallet", "finance", "crypto", "binance", "invest", "saham", "stock",
            ),
        ),
        ElyraCategoryRule(
            ElyraAppCategory.Shopping,
            hints = listOf(
                "shopee", "tokopedia", "lazada", "bukalapak", "blibli", "amazon", "aliexpress",
                "alibaba", "marketplace", "shop", "shopping", ".store", "vending", "zalora",
            ),
        ),
        ElyraCategoryRule(
            ElyraAppCategory.Travel,
            hints = listOf(
                "gojek", "grab", "maxim", "traveloka", "tiket", "booking", "agoda", "waze",
                "kereta", "krl", ".kai.", "airasia", "airline", "travel", "transport", "uber",
                "lyft", "flight", "hotel", "maps",
            ),
        ),
        ElyraCategoryRule(
            ElyraAppCategory.Education,
            hints = listOf(
                "classroom", "duolingo", "coursera", "udemy", "ruangguru", "zenius", "brainly",
                "dictionary", "translate", "learning", "education", "school", "khan", "quizlet",
                "photomath",
            ),
        ),
        ElyraCategoryRule(
            ElyraAppCategory.Health,
            hints = listOf(
                "satusehat", "pedulilindungi", "halodoc", "alodokter", "fitness", "workout",
                "health", "medical", "medicine", "strava", "fitbit", "mifit",
            ),
        ),
        ElyraCategoryRule(
            ElyraAppCategory.Productivity,
            hints = listOf(
                "docs", "sheets", "slides", "office", "word", "excel", "powerpoint", "notion",
                "trello", "slack", "teams", "calendar", "notes", ".keep", "todo", "task",
                "evernote", "onenote", "wps", ".pdf",
            ),
        ),
        ElyraCategoryRule(
            ElyraAppCategory.Tools,
            hints = listOf(
                "calculator", "clock", "filemanager", ".files", ".file.", "weather", "compass",
                "scanner", "authenticator", "vpn", "browser", "download", "cleaner", "terminal",
                "termux", "flashlight", "recorder", "backup", "antivirus", "tool",
            ),
        ),
    )

    /**
     * The pure classification rule.
     *
     * @param packageName the app package name.
     * @param label the app's user-visible label (may be empty).
     * @param applicationCategory [ApplicationInfo.category], or
     *   [ApplicationInfo.CATEGORY_UNDEFINED] when unknown.
     */
    fun classify(
        packageName: String,
        label: CharSequence?,
        applicationCategory: Int = ApplicationInfo.CATEGORY_UNDEFINED,
    ): ElyraAppCategory {
        val pkg = packageName.lowercase()
        val haystack = "$pkg ${label?.toString().orEmpty()}".lowercase()

        rules.forEach { rule ->
            if (rule.matches(pkg, haystack)) return rule.category
        }
        return categoryFromApplicationCategory(applicationCategory) ?: ElyraAppCategory.Other
    }

    private fun categoryFromApplicationCategory(category: Int): ElyraAppCategory? = when (category) {
        ApplicationInfo.CATEGORY_GAME -> ElyraAppCategory.Games
        ApplicationInfo.CATEGORY_AUDIO -> ElyraAppCategory.MusicAudio
        ApplicationInfo.CATEGORY_VIDEO -> ElyraAppCategory.Media
        ApplicationInfo.CATEGORY_IMAGE -> ElyraAppCategory.PhotoVideo
        ApplicationInfo.CATEGORY_SOCIAL,
        ApplicationInfo.CATEGORY_NEWS,
        -> ElyraAppCategory.Social
        ApplicationInfo.CATEGORY_MAPS -> ElyraAppCategory.Travel
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> ElyraAppCategory.Productivity
        ApplicationInfo.CATEGORY_ACCESSIBILITY -> ElyraAppCategory.Tools
        else -> null
    }
}
