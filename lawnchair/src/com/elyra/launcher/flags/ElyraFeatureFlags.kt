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

/**
 * Central registry of Elyra feature flags.
 *
 * This is the single source of truth for which experimental Elyra features exist.
 * It is pure Kotlin so it can be validated in unit tests without an Android
 * runtime. Persistence lives in [ElyraFlagStore]; higher-level access lives in
 * [ElyraFlagsRepository].
 */
object ElyraFeatureFlags {

    /** All flags, in declaration order. */
    val all: List<ElyraFlag> = ElyraFlag.entries.toList()

    /** Stable DataStore keys for all flags. */
    val keys: List<String> = all.map { it.key }

    init {
        // Stable-key contract: no two flags may share a key, or persistence would
        // collide. This is a cheap invariant guard, not a substitute for tests.
        require(keys.toSet().size == keys.size) {
            "Duplicate Elyra feature flag key detected in $keys"
        }
    }

    /** Look up a flag by its stable key, or null if unknown. */
    fun fromKey(key: String): ElyraFlag? = all.firstOrNull { it.key == key }

    /** The default (shipped) value for a flag. */
    fun defaultFor(flag: ElyraFlag): Boolean = flag.default
}
