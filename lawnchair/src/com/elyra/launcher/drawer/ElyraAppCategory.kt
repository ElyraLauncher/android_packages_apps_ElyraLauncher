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
 * The set of local app-drawer categories.
 *
 * The declaration order is the stable display order used for category grouping and
 * the (future) category card grid. Classification precedence is defined separately
 * by [ElyraAppCategoryClassifier] (e.g. System and Google are matched first), so the
 * two concerns do not get tangled. Each category resolves its user-facing label from
 * a string resource, so English (`values`) and Indonesian (`values-in`) are covered.
 * [Other] is the catch-all for apps that no rule matches.
 *
 * This enum is pure Kotlin, so the classifier can be unit tested without a device.
 */
enum class ElyraAppCategory(@StringRes val labelRes: Int) {
    Games(R.string.elyra_category_games),
    Social(R.string.elyra_category_social),
    Communication(R.string.elyra_category_communication),
    Google(R.string.elyra_category_google),
    Media(R.string.elyra_category_media),
    PhotoVideo(R.string.elyra_category_photo_video),
    MusicAudio(R.string.elyra_category_music_audio),
    Productivity(R.string.elyra_category_productivity),
    Tools(R.string.elyra_category_tools),
    Finance(R.string.elyra_category_finance),
    Shopping(R.string.elyra_category_shopping),
    Travel(R.string.elyra_category_travel),
    Education(R.string.elyra_category_education),
    Health(R.string.elyra_category_health),
    System(R.string.elyra_category_system),
    Other(R.string.elyra_category_other),
}
