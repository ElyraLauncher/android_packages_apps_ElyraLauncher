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
import com.android.launcher3.model.data.AppInfo

/**
 * Facade that groups installed apps into localized category buckets for the drawer.
 *
 * The classification and grouping logic lives in [ElyraAppCategoryClassifier] and
 * [ElyraCategoryCardModel]; this facade preserves the simple title-to-apps map used
 * by the drawer's category-folder path so callers do not depend on the richer card
 * model. It is fully local — no network, cloud, or Play Store category API.
 */
object ElyraAppCategorizer {

    /**
     * Groups [apps] into localized category buckets in the stable
     * [ElyraAppCategory] order, skipping empty categories. The key is the localized
     * category label (usable directly as a drawer folder/section title); apps within
     * a bucket are in the card model's stable order.
     */
    fun categorize(apps: List<AppInfo>, context: Context): LinkedHashMap<String, List<AppInfo>> {
        val result = LinkedHashMap<String, List<AppInfo>>()
        ElyraCategoryCardModel.build(apps, context).forEach { card ->
            result[card.label] = card.apps
        }
        return result
    }
}
