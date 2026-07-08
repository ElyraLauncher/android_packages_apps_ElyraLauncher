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
 * vs. bottom of the drawer). Gating every call site through this one helper keeps
 * the flag read consistent and makes the OFF path provably identical to upstream:
 * when [ElyraFlag.BottomSearch] is off, no bottom-search code path runs.
 *
 * The value is read once per All Apps container construction (a blocking read of
 * the persisted flag, matching how the container already reads its other
 * preferences); toggling the flag takes effect the next time the drawer is rebuilt.
 */
object ElyraBottomSearch {

    /** Whether the app-drawer search bar should be anchored to the bottom. */
    @JvmStatic
    fun isEnabled(context: Context): Boolean =
        ElyraFlagsRepository.getInstance(context).isEnabled(ElyraFlag.BottomSearch)

    /**
     * Whether optional web/provider results (web suggestions, the search-provider
     * row, the Play Store "more apps" row) may appear below local app results in the
     * drawer. Defaults OFF: the drawer is local-installed-apps first and runs no
     * network/provider search unless the user explicitly opts in. The provider
     * implementations are preserved for a future home/global search surface.
     */
    @JvmStatic
    fun webResultsEnabled(context: Context): Boolean =
        ElyraFlagsRepository.getInstance(context).isEnabled(ElyraFlag.DrawerWebResults)
}
