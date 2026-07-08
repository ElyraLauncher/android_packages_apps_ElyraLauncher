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

import androidx.annotation.StringRes
import com.android.launcher3.R

/**
 * The fixed set of local app-drawer categories.
 *
 * The order of the entries is the stable display order used when the drawer groups
 * apps into categories. Each category resolves its user-facing label from a string
 * resource, so English (`values`) and Indonesian (`values-in`) are both covered.
 * [Other] is the catch-all for apps that cannot be classified.
 *
 * This enum carries no Android state and is pure Kotlin, so the classifier that maps
 * apps onto it ([ElyraAppCategorizer]) can be unit tested without a running device.
 */
enum class ElyraAppCategory(@StringRes val labelRes: Int) {
    Communication(R.string.elyra_category_communication),
    Social(R.string.elyra_category_social),
    Media(R.string.elyra_category_media),
    Games(R.string.elyra_category_games),
    Tools(R.string.elyra_category_tools),
    Productivity(R.string.elyra_category_productivity),
    Finance(R.string.elyra_category_finance),
    Shopping(R.string.elyra_category_shopping),
    Travel(R.string.elyra_category_travel),
    System(R.string.elyra_category_system),
    Other(R.string.elyra_category_other),
}
