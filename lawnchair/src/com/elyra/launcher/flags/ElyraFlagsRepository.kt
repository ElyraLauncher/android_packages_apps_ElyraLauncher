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
import com.elyra.launcher.ElyraLog
import com.patrykmichalik.opto.core.firstBlocking
import com.patrykmichalik.opto.core.setBlocking
import kotlinx.coroutines.flow.Flow

/**
 * Read/write access to Elyra feature flags for non-Compose call sites.
 *
 * Feature code should gate behavior with [isEnabled] (or [observe] for reactive
 * updates). Shipped defaults live in [ElyraFlag]; persisted user choices override
 * those defaults. Compose settings screens can bind directly to
 * [ElyraFlagStore.preferenceFor] via `getAdapter()`.
 */
class ElyraFlagsRepository(private val store: ElyraFlagStore) {

    /** Current value of a flag (blocking read of the persisted value). */
    fun isEnabled(flag: ElyraFlag): Boolean = store.preferenceFor(flag).firstBlocking()

    /** Reactive stream of a flag's value. */
    fun observe(flag: ElyraFlag): Flow<Boolean> = store.preferenceFor(flag).get()

    /** Persist a new value for a flag. */
    fun setEnabled(flag: ElyraFlag, enabled: Boolean) {
        store.preferenceFor(flag).setBlocking(enabled)
        ElyraLog.d("flag ${flag.key} set to $enabled")
    }

    companion object {
        fun getInstance(context: Context): ElyraFlagsRepository =
            ElyraFlagsRepository(ElyraFlagStore.getInstance(context))
    }
}
