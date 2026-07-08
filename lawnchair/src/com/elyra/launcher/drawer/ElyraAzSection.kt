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
 * A-Z fast-rail section mapping.
 *
 * The drawer's fast-scroll rail groups apps by a leading section letter. The real
 * sectioning is done by the Launcher3 `AlphabeticalAppsList` (which the A-Z rail
 * feature simply reveals via the existing fast scroller); this helper captures the
 * label-to-section fallback in a pure, unit-testable form so non-Latin, empty, and
 * numeric labels degrade to a stable `#` bucket instead of a raw glyph.
 */
object ElyraAzSection {

    /** The bucket used for labels that do not start with a Latin letter. */
    const val NON_LATIN_SECTION = "#"

    /**
     * Returns the A-Z section for [label]: the uppercased leading letter for a
     * Latin-alphabet label, or [NON_LATIN_SECTION] for empty, numeric, symbol, or
     * non-Latin labels. Pure and locale-independent for A-Z.
     */
    fun sectionFor(label: CharSequence?): String {
        val first = label?.trim()?.firstOrNull() ?: return NON_LATIN_SECTION
        return if (first in 'A'..'Z' || first in 'a'..'z') {
            first.uppercaseChar().toString()
        } else {
            NON_LATIN_SECTION
        }
    }
}
