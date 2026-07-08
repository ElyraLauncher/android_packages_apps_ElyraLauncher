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

import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.ColorInt
import kotlin.math.max

object ElyraAppColorClassifier {

    private const val MIN_ALPHA = 72
    private const val MIN_COLORFUL_SATURATION = 0.24f
    private const val MIN_COLORFUL_VALUE = 0.20f

    fun classify(@ColorInt color: Int): ElyraAppColorBucket {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        return classifyPixel(hsv[0], hsv[1], hsv[2])
    }

    fun classify(bitmap: Bitmap): ElyraAppColorBucket {
        val counts = linkedMapOf<ElyraAppColorBucket, Int>().apply {
            ElyraAppColorBucket.entries.forEach { put(it, 0) }
        }
        var total = 0
        val stepX = max(1, bitmap.width / 24)
        val stepY = max(1, bitmap.height / 24)
        val hsv = FloatArray(3)

        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                if (Color.alpha(pixel) >= MIN_ALPHA) {
                    Color.colorToHSV(pixel, hsv)
                    val bucket = classifyPixel(hsv[0], hsv[1], hsv[2])
                    counts[bucket] = counts.getValue(bucket) + 1
                    total++
                }
                x += stepX
            }
            y += stepY
        }

        if (total == 0) return ElyraAppColorBucket.Gray

        val ranked = counts.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
        val top = ranked.first()
        val second = ranked.getOrNull(1)
        val colorfulBuckets = ranked.count { (bucket, count) ->
            bucket !in neutralBuckets && count >= total * 0.12f
        }

        if (colorfulBuckets >= 3) return ElyraAppColorBucket.Multicolor
        if (top.key !in neutralBuckets && second != null && second.key !in neutralBuckets) {
            val topShare = top.value.toFloat() / total
            if (topShare < 0.45f && second.value >= top.value * 0.62f) {
                return ElyraAppColorBucket.Multicolor
            }
        }
        return top.key
    }

    private fun classifyPixel(hue: Float, saturation: Float, value: Float): ElyraAppColorBucket {
        if (saturation < 0.16f) {
            return when {
                value < 0.18f -> ElyraAppColorBucket.Black
                value > 0.86f -> ElyraAppColorBucket.White
                else -> ElyraAppColorBucket.Gray
            }
        }
        if (saturation < MIN_COLORFUL_SATURATION || value < MIN_COLORFUL_VALUE) {
            return if (value < 0.24f) ElyraAppColorBucket.Black else ElyraAppColorBucket.Gray
        }
        if (hue in 18f..44f && saturation > 0.32f && value < 0.68f) {
            return ElyraAppColorBucket.Brown
        }
        return when {
            hue < 12f || hue >= 345f -> ElyraAppColorBucket.Red
            hue < 28f -> ElyraAppColorBucket.Orange
            hue < 58f -> ElyraAppColorBucket.Yellow
            hue < 150f -> ElyraAppColorBucket.Green
            hue < 190f -> ElyraAppColorBucket.Teal
            hue < 245f -> ElyraAppColorBucket.Blue
            hue < 292f -> ElyraAppColorBucket.Purple
            hue < 345f -> ElyraAppColorBucket.Pink
            else -> ElyraAppColorBucket.Multicolor
        }
    }

    private val neutralBuckets = setOf(
        ElyraAppColorBucket.Gray,
        ElyraAppColorBucket.Black,
        ElyraAppColorBucket.White,
    )
}
