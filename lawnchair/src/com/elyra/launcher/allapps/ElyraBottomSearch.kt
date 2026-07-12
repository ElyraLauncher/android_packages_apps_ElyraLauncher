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

package com.elyra.launcher.allapps

import android.content.Context
import com.elyra.launcher.flags.ElyraFlag
import com.elyra.launcher.flags.ElyraFlagsRepository

/**
 * Single decision point for the Elyra "bottom search" app-drawer feature.
 *
 * The feature reuses the real Launcher3/Lawnchair All Apps model and search
 * primitives; it only changes where the existing search surface is anchored (top
 * vs. bottom of the drawer). The bottom placement is the stable drawer baseline. Compatibility flag keys remain
 * in the schema so existing preference data stays readable, but presentation no
 * longer depends on a Labs choice.
 */
object ElyraBottomSearch {

    /** Whether the app-drawer search bar should be anchored to the bottom. */
    @JvmStatic
    fun isEnabled(context: Context): Boolean = true

    /**
     * Compatibility preference retained for future global-search surfaces. Normal
     * bottom drawer search intentionally ignores this and remains local-only.
     */
    @JvmStatic
    fun webResultsEnabled(context: Context): Boolean =
        ElyraFlagsRepository.getInstance(context).isEnabled(ElyraFlag.DrawerWebResults)

    @JvmStatic
    fun localOnlyDrawerSearchEnabled(): Boolean = true

    @JvmStatic
    fun shouldAcceptDrawerResult(
        resultQuery: String,
        currentQuery: String,
        hasColorFilter: Boolean,
    ): Boolean = resultQuery == currentQuery && (currentQuery.isNotEmpty() || hasColorFilter)

    @JvmStatic
    fun colorSearchEnabled(context: Context): Boolean = true
}
