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

package app.lawnchair.search.algorithms

import android.content.Context
import app.lawnchair.preferences2.PreferenceManager2
import com.android.launcher3.LauncherAppState
import com.android.launcher3.allapps.BaseAllAppsAdapter
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.search.SearchCallback
import com.android.launcher3.search.StringMatcherUtility
import com.android.launcher3.util.Executors
import com.elyra.launcher.allapps.ElyraAppColorBucket
import com.elyra.launcher.allapps.ElyraAppIconColorExtractor
import com.patrykmichalik.opto.core.firstBlocking
import android.os.Handler

class ElyraAppColorSearchAlgorithm(
    context: Context,
    private val delegate: LawnchairSearchAlgorithm,
    private val selectedBucket: () -> ElyraAppColorBucket?,
) : LawnchairSearchAlgorithm(context) {

    private val appState = LauncherAppState.getInstance(context)
    private val resultHandler = Handler(Executors.MAIN_EXECUTOR.looper)
    private val prefs2 = PreferenceManager2.getInstance(context)

    override fun doSearch(query: String, callback: SearchCallback<BaseAllAppsAdapter.AdapterItem>) {
        val bucket = selectedBucket()
        if (bucket == null) {
            delegate.doSearch(query, callback)
            return
        }
        doColorSearch(query, bucket, callback)
    }

    override fun doZeroStateSearch(callback: SearchCallback<BaseAllAppsAdapter.AdapterItem>) {
        val bucket = selectedBucket()
        if (bucket == null) {
            delegate.doZeroStateSearch(callback)
            return
        }
        doColorSearch("", bucket, callback)
    }

    override fun cancel(interruptActiveRequests: Boolean) {
        delegate.cancel(interruptActiveRequests)
        if (interruptActiveRequests) {
            resultHandler.removeCallbacksAndMessages(null)
        }
    }

    private fun doColorSearch(
        query: String,
        bucket: ElyraAppColorBucket,
        callback: SearchCallback<BaseAllAppsAdapter.AdapterItem>,
    ) {
        appState.model.enqueueModelUpdateTask { _, _, apps ->
            val result = getColorMatchResult(apps.data, query, bucket)
            resultHandler.post { callback.onSearchResult(query, result) }
        }
    }

    private fun getColorMatchResult(
        apps: List<AppInfo>,
        query: String,
        bucket: ElyraAppColorBucket,
    ): ArrayList<BaseAllAppsAdapter.AdapterItem> {
        val hiddenApps = prefs2.hiddenApps.firstBlocking()
        val matcher = StringMatcherUtility.StringMatcher.getInstance()
        val normalizedQuery = query.trim().lowercase()
        val result = ArrayList<BaseAllAppsAdapter.AdapterItem>()

        apps.asSequence()
            .filterNot { hiddenApps.contains(it.toComponentKey().toString()) }
            .filter { ElyraAppIconColorExtractor.bucketFor(context, it) == bucket }
            .filter {
                normalizedQuery.isEmpty() ||
                    StringMatcherUtility.matches(normalizedQuery, it.title.toString(), matcher)
            }
            .take(MAX_COLOR_RESULTS)
            .mapTo(result) { BaseAllAppsAdapter.AdapterItem.asApp(it) }

        if (result.isEmpty()) {
            result.add(emptyMessage(query.ifEmpty { context.getString(bucket.labelRes) }))
        }
        return result
    }

    private fun emptyMessage(query: String): BaseAllAppsAdapter.AdapterItem {
        val item = BaseAllAppsAdapter.AdapterItem(BaseAllAppsAdapter.VIEW_TYPE_EMPTY_SEARCH)
        val placeholder = AppInfo()
        placeholder.title = query
        item.itemInfo = placeholder
        return item
    }

    private companion object {
        const val MAX_COLOR_RESULTS = 60
    }
}
