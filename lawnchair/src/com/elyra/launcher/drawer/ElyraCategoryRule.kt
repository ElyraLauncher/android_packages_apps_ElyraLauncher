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

/**
 * A single deterministic classification rule for [ElyraAppCategoryClassifier].
 *
 * A rule matches an app when its package name starts with any of [packagePrefixes]
 * or when the app's "haystack" (lowercased `package + " " + label`) contains any of
 * [hints]. Keeping the rules as plain data in one place — rather than scattering
 * `if (pkg.contains(...))` checks across UI classes — makes the category system easy
 * to read, test, and extend.
 *
 * All matching is lowercase substring / prefix matching; callers pass already
 * lowercased text. Rules are evaluated in list order, so more specific rules must be
 * ordered before more generic ones.
 */
data class ElyraCategoryRule(
    val category: ElyraAppCategory,
    val hints: List<String> = emptyList(),
    val packagePrefixes: List<String> = emptyList(),
) {
    /**
     * @param packageName lowercased package name.
     * @param haystack lowercased `package + " " + label`.
     */
    fun matches(packageName: String, haystack: String): Boolean =
        packagePrefixes.any { packageName.startsWith(it) } || hints.any { haystack.contains(it) }
}
