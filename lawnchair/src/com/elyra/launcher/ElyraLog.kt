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

import android.util.Log
import com.android.launcher3.BuildConfig

/**
 * Debug-only logging for Elyra startup, settings, and feature-flag events.
 *
 * This is a thin wrapper over [android.util.Log] under a single `Elyra` tag so
 * baseline behavior can be traced with `adb logcat -s Elyra` during smoke tests.
 * It is intentionally minimal:
 *
 * - Debug output ([d]/[w]) is gated on [BuildConfig.DEBUG], so release builds emit
 *   nothing and pay no cost.
 * - There is no telemetry, no network, and no file/external upload — it only writes
 *   to the local device log buffer.
 *
 * Genuine error conditions ([e]) are always logged so crashes remain diagnosable in
 * any build, but still only to the local log buffer.
 */
object ElyraLog {

    /** Shared logcat tag for all Elyra baseline diagnostics. */
    const val TAG = "Elyra"

    /** True when verbose Elyra debug logging is compiled in (debug builds only). */
    val isLoggable: Boolean get() = BuildConfig.DEBUG

    /** Debug trace; no-op in release builds. */
    fun d(message: String) {
        if (isLoggable) Log.d(TAG, message)
    }

    /** Warning trace; no-op in release builds. */
    fun w(message: String, throwable: Throwable? = null) {
        if (isLoggable) Log.w(TAG, message, throwable)
    }

    /** Error trace; always recorded to the local log buffer so crashes stay diagnosable. */
    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
}
