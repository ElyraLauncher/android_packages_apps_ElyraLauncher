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
import com.elyra.launcher.flags.ElyraFlag
import com.elyra.launcher.flags.ElyraFlagsRepository

/**
 * Single decision point for the Stage 6 app-drawer organization features:
 * categories, local suggestions, and the A-Z fast rail. Each is gated by its own
 * Stage 3 flag and defaults OFF, so when a flag is off the drawer behaves exactly
 * like upstream. Reads are blocking flag reads, consistent with how the drawer
 * already reads its other preferences; a toggle takes effect the next time the
 * drawer is rebuilt.
 */
object ElyraDrawer {

    /** Group the drawer app list into local categories. */
    @JvmStatic
    fun categoriesEnabled(context: Context): Boolean =
        ElyraFlagsRepository.getInstance(context).isEnabled(ElyraFlag.DrawerCategories)

    /** Surface a local suggestions section at the top of the drawer. */
    @JvmStatic
    fun suggestionsEnabled(context: Context): Boolean =
        ElyraFlagsRepository.getInstance(context).isEnabled(ElyraFlag.DrawerSuggestions)

    /** Reveal the A-Z fast-scroll rail on the side of the drawer. */
    @JvmStatic
    fun azRailEnabled(context: Context): Boolean =
        ElyraFlagsRepository.getInstance(context).isEnabled(ElyraFlag.AzRail)
}
