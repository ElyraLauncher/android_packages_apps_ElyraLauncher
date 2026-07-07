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

package com.elyra.launcher.flags

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.android.launcher3.util.MainThreadInitializedObject
import com.android.launcher3.util.SafeCloseable
import com.patrykmichalik.opto.core.PreferenceManager
import com.patrykmichalik.opto.domain.Preference

/**
 * Persistent storage for Elyra feature flags.
 *
 * Reuses the launcher's existing preference architecture (the opto
 * [PreferenceManager] over Jetpack DataStore) instead of introducing a new
 * storage mechanism. Flags are kept in a dedicated `elyra_preferences` DataStore
 * file so Elyra state stays isolated from upstream Lawnchair preferences, which
 * keeps rebases clean. There is no DB mutation, no network, and no telemetry.
 */
class ElyraFlagStore private constructor(context: Context) :
    PreferenceManager,
    SafeCloseable {

    override val preferencesDataStore = context.elyraPreferencesDataStore

    private val flags: Map<ElyraFlag, Preference<Boolean, Boolean, *>> =
        ElyraFeatureFlags.all.associateWith { flag ->
            preference(
                key = booleanPreferencesKey(name = flag.key),
                defaultValue = flag.default,
            )
        }

    /** The persistent preference backing a given flag. */
    fun preferenceFor(flag: ElyraFlag): Preference<Boolean, Boolean, *> = flags.getValue(flag)

    override fun close() {}

    companion object {
        private val Context.elyraPreferencesDataStore by preferencesDataStore(
            name = "elyra_preferences",
        )

        @JvmField
        val INSTANCE = MainThreadInitializedObject(::ElyraFlagStore)

        @JvmStatic
        fun getInstance(context: Context): ElyraFlagStore = INSTANCE.get(context)!!
    }
}
