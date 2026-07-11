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
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.util.ComponentKey
import java.util.concurrent.TimeUnit

/**
 * Local app-drawer suggestions backed by the platform prediction order plus a
 * permission-free launch-history fallback. No signal leaves the device.
 */
object ElyraDrawerSuggestions {

    const val DEFAULT_COUNT = 5

    private const val PREFERENCES = "elyra_drawer_suggestion_history"
    private const val LAST_PREFIX = "last:"
    private const val COUNT_PREFIX = "count:"
    private val HOUR_MILLIS = TimeUnit.HOURS.toMillis(1)
    private val RECENCY_WINDOW_MILLIS = TimeUnit.DAYS.toMillis(14)
    private val NEW_INSTALL_WINDOW_MILLIS = TimeUnit.HOURS.toMillis(72)

    data class Candidate<T>(val item: T, val score: Long, val tiebreak: String)

    @Volatile
    private var platformPredictionKeys: List<String> = emptyList()
    private val installTimeCache = HashMap<String, Long>()

    /** Deterministic, unique, bounded ranking used by production and tests. */
    fun <T> rank(candidates: List<Candidate<T>>, limit: Int): List<T> {
        if (limit <= 0) return emptyList()
        return candidates
            .sortedWith(compareByDescending<Candidate<T>> { it.score }.thenBy { it.tiebreak })
            .distinctBy { it.item }
            .take(limit.coerceAtMost(DEFAULT_COUNT))
            .map { it.item }
    }

    /**
     * Combines platform prediction, recent launch, frequency, and a temporary
     * 72-hour install boost. Old installs receive no residual install score.
     */
    fun score(
        nowMillis: Long,
        lastLaunchMillis: Long,
        launchCount: Long,
        installMillis: Long,
        platformRank: Int?,
    ): Long {
        val launchAge = (nowMillis - lastLaunchMillis).coerceAtLeast(0)
        val recency = if (lastLaunchMillis > 0 && launchAge < RECENCY_WINDOW_MILLIS) {
            ((RECENCY_WINDOW_MILLIS - launchAge) / HOUR_MILLIS) * 50_000L
        } else {
            0L
        }
        val frequency = launchCount.coerceIn(0, 100) * 100_000L
        val installAge = (nowMillis - installMillis).coerceAtLeast(0)
        val newInstall = if (installMillis > 0 && installAge < NEW_INSTALL_WINDOW_MILLIS) {
            ((NEW_INSTALL_WINDOW_MILLIS - installAge) / HOUR_MILLIS + 1L) * 50_000L
        } else {
            0L
        }
        val platform = platformRank?.let { (DEFAULT_COUNT - it).coerceAtLeast(1) * 1_000_000L }
            ?: 0L
        return recency + frequency + newInstall + platform
    }

    /** Receives the existing Launcher3 on-device All Apps prediction order. */
    @JvmStatic
    fun updatePlatformPredictions(items: List<ItemInfo>) {
        platformPredictionKeys = items.mapNotNull { item ->
            val component = item.targetComponent ?: return@mapNotNull null
            ComponentKey(component, item.user).toString()
        }.distinct()
    }

    /** Records a successful launcher app launch for the local fallback. */
    @JvmStatic
    fun recordLaunch(context: Context, item: ItemInfo) {
        val component = item.targetComponent ?: return
        val key = ComponentKey(component, item.user).toString()
        val preferences = context.applicationContext.getSharedPreferences(
            PREFERENCES,
            Context.MODE_PRIVATE,
        )
        val countKey = COUNT_PREFIX + key
        preferences.edit()
            .putLong(LAST_PREFIX + key, System.currentTimeMillis())
            .putLong(countKey, preferences.getLong(countKey, 0L) + 1L)
            .apply()
    }

    /** Package/model changes invalidate install timestamps, including reinstalls. */
    @JvmStatic
    fun onPackagesChanged() {
        synchronized(installTimeCache) { installTimeCache.clear() }
    }

    private fun installTime(context: Context, app: AppInfo, key: String): Long {
        synchronized(installTimeCache) {
            installTimeCache[key]?.let { return it }
        }
        val pkg = app.targetPackage ?: return 0L
        val time = try {
            context.packageManager.getPackageInfo(pkg, 0).firstInstallTime
        } catch (_: Exception) {
            0L
        }
        synchronized(installTimeCache) { installTimeCache[key] = time }
        return time
    }

    /**
     * Returns up to five real, available, unique apps. The caller supplies the
     * already profile-filtered All Apps list and hidden component keys.
     */
    fun suggest(context: Context, apps: List<AppInfo>, limit: Int): List<AppInfo> =
        suggest(context, apps, emptySet(), limit)

    fun suggest(
        context: Context,
        apps: List<AppInfo>,
        hiddenApps: Set<String> = emptySet(),
        limit: Int = DEFAULT_COUNT,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<AppInfo> {
        val preferences = context.applicationContext.getSharedPreferences(
            PREFERENCES,
            Context.MODE_PRIVATE,
        )
        val predictionRanks = platformPredictionKeys.withIndex().associate { it.value to it.index }
        val candidates = apps.asSequence()
            .distinctBy { it.toComponentKey() }
            .filterNot { it.targetPackage == context.packageName }
            .mapNotNull { app ->
                val key = app.toComponentKey().toString()
                if (key in hiddenApps) return@mapNotNull null
                Candidate(
                    item = app,
                    score = score(
                        nowMillis = nowMillis,
                        lastLaunchMillis = preferences.getLong(LAST_PREFIX + key, 0L),
                        launchCount = preferences.getLong(COUNT_PREFIX + key, 0L),
                        installMillis = installTime(context, app, key),
                        platformRank = predictionRanks[key],
                    ),
                    tiebreak = app.title?.toString()?.lowercase().orEmpty(),
                )
            }
            .toList()
        return rank(candidates, limit)
    }
}
