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
import android.content.pm.ApplicationInfo
import com.android.launcher3.model.data.AppInfo
import java.util.concurrent.ConcurrentHashMap

/**
 * Builds the local category card models that back the drawer Categories view.
 *
 * Each [CategoryCard] is derived only from installed apps via
 * [ElyraAppCategoryClassifier]; there is no cloud, network, or Play Store category
 * API at runtime. Empty categories are dropped, cards are returned in the stable
 * [ElyraAppCategory] order, and apps within a card are ordered by the same local
 * signal used for suggestions (package recency, then label) so the preview cluster
 * is stable and uses real installed apps.
 *
 * The Android-declared [ApplicationInfo.category] lookup is cached per component so
 * repeated drawer rebuilds do not re-query PackageManager.
 */
object ElyraCategoryCardModel {

    /** Default number of app icons previewed on a category card. */
    const val DEFAULT_PREVIEW_COUNT = 6

    /**
     * A single category card: its [category], localized [label], the full ordered
     * [apps] in the category, and the leading [preview] used for the card cluster.
     */
    data class CategoryCard(
        val category: ElyraAppCategory,
        val label: String,
        val apps: List<AppInfo>,
        val preview: List<AppInfo>,
    )

    private val categoryCache = ConcurrentHashMap<String, ElyraAppCategory>()

    private fun resolveCategory(app: AppInfo): ElyraAppCategory {
        val pkg = app.targetPackage ?: return ElyraAppCategory.Other
        val key = app.toComponentKey().toString()
        return categoryCache[key]
            ?: ElyraAppCategoryClassifier.classify(
                pkg,
                app.title,
                ApplicationInfo.CATEGORY_UNDEFINED,
            )
    }

    /** Resolves PackageManager metadata on Launcher3's model executor. */
    fun warm(context: Context, apps: List<AppInfo>) {
        apps.forEach { app ->
            val pkg = app.targetPackage ?: return@forEach
            val key = app.toComponentKey().toString()
            val applicationCategory = try {
                context.packageManager.getApplicationInfo(pkg, 0).category
            } catch (_: Exception) {
                ApplicationInfo.CATEGORY_UNDEFINED
            }
            categoryCache[key] = ElyraAppCategoryClassifier.classify(
                pkg,
                app.title,
                applicationCategory,
            )
        }
    }

    fun invalidate(componentKeys: Set<String>) {
        componentKeys.forEach(categoryCache::remove)
    }

    /**
     * Groups [apps] into non-empty category cards in stable [ElyraAppCategory] order.
     * [previewCount] bounds the preview cluster size. Apps in each card are ordered
     * by the local recency signal (via [ElyraDrawerSuggestions]) with a label
     * tiebreak, so both the card preview and the opened category list are stable.
     */
    fun build(
        apps: List<AppInfo>,
        context: Context,
        previewCount: Int = DEFAULT_PREVIEW_COUNT,
    ): List<CategoryCard> {
        val byCategory = LinkedHashMap<ElyraAppCategory, MutableList<AppInfo>>()
        apps.forEach { app ->
            byCategory.getOrPut(resolveCategory(app)) { mutableListOf() }.add(app)
        }

        return ElyraAppCategory.entries.mapNotNull { category ->
            val bucket = byCategory[category]
            if (bucket.isNullOrEmpty()) {
                null
            } else {
                val ordered = ElyraDrawerSuggestions.suggest(context, bucket, bucket.size)
                CategoryCard(
                    category = category,
                    label = context.getString(category.labelRes),
                    apps = ordered,
                    preview = ordered.take(previewCount.coerceAtLeast(0)),
                )
            }
        }
    }
}
