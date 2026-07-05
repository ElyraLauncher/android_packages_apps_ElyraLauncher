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

package com.elyra.launcher

/**
 * Central product identity for Elyra Launcher.
 *
 * New Elyra-specific code should live under the `com.elyra.launcher` namespace.
 * The upstream Launcher3 (`com.android.launcher3`) and Lawnchair (`app.lawnchair`)
 * packages are intentionally preserved so future rebases onto upstream stay
 * practical; only genuinely new Elyra code introduces this namespace.
 */
object ElyraBranding {

    /** Full user-facing product name. */
    const val PRODUCT_NAME = "Elyra Launcher"

    /** Short user-facing product name. */
    const val SHORT_NAME = "Elyra"

    /** Human-readable prefix used for user-visible backup file names. */
    const val BACKUP_FILE_PREFIX = "Elyra_Backup"
}
