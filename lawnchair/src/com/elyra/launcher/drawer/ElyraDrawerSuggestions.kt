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

/**
 * Local, deterministic app-drawer suggestions.
 *
 * Suggestions are ranked from on-device signals only — there is no web/provider
 * suggestion, no network call, and no telemetry. The default local signal is
 * package recency (most-recently-installed first), a permission-free signal read
 * from [android.content.pm.PackageManager]; the label is the deterministic
 * tiebreak. A caller may pass an explicit usage score map (e.g. once the launcher
 * tracks launches locally) to override the signal without changing this API.
 *
 * The ranking rule [rank] is pure and generic so it can be unit tested without a
 * device.
 */
object ElyraDrawerSuggestions {

    /** Default number of suggestions surfaced at the top of the drawer. */
    const val DEFAULT_COUNT = 5

    /** A rankable candidate: higher [score] ranks first, [tiebreak] breaks ties. */
    data class Candidate<T>(val item: T, val score: Long, val tiebreak: String)

    /**
     * Deterministic ranking: by [Candidate.score] descending, then [Candidate.tiebreak]
     * ascending. Returns at most [limit] items. Pure and side-effect free.
     */
    fun <T> rank(candidates: List<Candidate<T>>, limit: Int): List<T> {
        if (limit <= 0) return emptyList()
        return candidates
            .sortedWith(compareByDescending<Candidate<T>> { it.score }.thenBy { it.tiebreak })
            .take(limit)
            .map { it.item }
    }

    // Cache install time per package; the value is stable for the life of an install.
    private val installTimeCache = HashMap<String, Long>()

    private fun installTime(context: Context, app: AppInfo): Long {
        val pkg = app.targetPackage ?: return 0L
        installTimeCache[pkg]?.let { return it }
        val time = try {
            context.packageManager.getPackageInfo(pkg, 0).firstInstallTime
        } catch (_: Exception) {
            0L
        }
        return time.also { installTimeCache[pkg] = it }
    }

    /**
     * Ranks [apps] into at most [limit] local suggestions using package recency as
     * the default signal and the lowercased label as the deterministic tiebreak.
     */
    fun suggest(context: Context, apps: List<AppInfo>, limit: Int = DEFAULT_COUNT): List<AppInfo> {
        val candidates = apps.map { app ->
            Candidate(
                item = app,
                score = installTime(context, app),
                tiebreak = app.title?.toString()?.lowercase().orEmpty(),
            )
        }
        return rank(candidates, limit)
    }
}
