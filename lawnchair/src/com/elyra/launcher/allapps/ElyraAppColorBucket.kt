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

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import com.android.launcher3.R

enum class ElyraAppColorBucket(
    @StringRes val labelRes: Int,
    @ColorInt val swatchColor: Int,
) {
    Red(R.string.elyra_color_red, Color.rgb(220, 38, 38)),
    Orange(R.string.elyra_color_orange, Color.rgb(234, 88, 12)),
    Yellow(R.string.elyra_color_yellow, Color.rgb(234, 179, 8)),
    Green(R.string.elyra_color_green, Color.rgb(22, 163, 74)),
    Teal(R.string.elyra_color_teal, Color.rgb(13, 148, 136)),
    Blue(R.string.elyra_color_blue, Color.rgb(37, 99, 235)),
    Purple(R.string.elyra_color_purple, Color.rgb(126, 34, 206)),
    Pink(R.string.elyra_color_pink, Color.rgb(219, 39, 119)),
    Brown(R.string.elyra_color_brown, Color.rgb(120, 72, 40)),
    Gray(R.string.elyra_color_gray, Color.rgb(107, 114, 128)),
    Black(R.string.elyra_color_black, Color.rgb(24, 24, 27)),
    White(R.string.elyra_color_white, Color.rgb(245, 245, 245)),
    Multicolor(R.string.elyra_color_multicolor, Color.rgb(99, 102, 241)),
}
