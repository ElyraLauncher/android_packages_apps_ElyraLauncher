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

package com.elyra.launcher.allapps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.android.launcher3.model.data.AppInfo
import java.util.concurrent.ConcurrentHashMap

object ElyraAppIconColorExtractor {

    private const val ICON_SAMPLE_SIZE = 48
    private val cache = ConcurrentHashMap<String, ElyraAppColorBucket>()

    fun bucketFor(context: Context, app: AppInfo): ElyraAppColorBucket {
        val key = app.toComponentKey().toString()
        return cache.getOrPut(key) {
            runCatching {
                val drawable = app.newIcon(context, false)
                ElyraAppColorClassifier.classify(drawable.toSampleBitmap())
            }.getOrDefault(ElyraAppColorBucket.Gray)
        }
    }

    fun clear() {
        cache.clear()
    }

    fun invalidate(componentKeys: Set<String>) {
        componentKeys.forEach(cache::remove)
    }

    private fun Drawable.toSampleBitmap(): Bitmap {
        if (this is BitmapDrawable && bitmap != null && !bitmap.isRecycled) {
            return Bitmap.createScaledBitmap(bitmap, ICON_SAMPLE_SIZE, ICON_SAMPLE_SIZE, true)
        }
        val bitmap = Bitmap.createBitmap(ICON_SAMPLE_SIZE, ICON_SAMPLE_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val oldBounds = bounds
        setBounds(0, 0, ICON_SAMPLE_SIZE, ICON_SAMPLE_SIZE)
        draw(canvas)
        setBounds(oldBounds)
        return bitmap
    }
}
