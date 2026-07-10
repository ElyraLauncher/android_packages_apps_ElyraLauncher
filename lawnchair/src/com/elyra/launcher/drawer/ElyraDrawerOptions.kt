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

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Switch
import android.widget.TextView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.android.launcher3.R
import com.android.launcher3.util.Themes
import com.elyra.launcher.flags.ElyraFlag
import com.elyra.launcher.flags.ElyraFlagsRepository

/**
 * Drawer-scoped overflow menu for the app-drawer header three-dot button.
 *
 * This compact anchored panel contains only drawer-scoped actions: the persisted
 * local suggestion toggle and a shortcut back to the real All Apps mode. It does
 * not route through Labs or expose unrelated launcher settings.
 */
object ElyraDrawerOptions {

    @JvmStatic
    fun show(anchor: View, onChanged: Runnable, onShowAll: Runnable) {
        val context = anchor.context
        val repo = ElyraFlagsRepository.getInstance(context)
        val width = dp(anchor, 248)
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(anchor, 10), dp(anchor, 8), dp(anchor, 10), dp(anchor, 8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(anchor, 20).toFloat()
                val base = Themes.getColorBackgroundFloating(context)
                setColor(Color.argb(244, Color.red(base), Color.green(base), Color.blue(base)))
                setStroke(
                    dp(anchor, 1),
                    Themes.getAttrColor(context, android.R.attr.textColorTertiary),
                )
            }
        }

        panel.addView(TextView(context).apply {
            setText(R.string.elyra_drawer_options_title)
            setTextColor(Themes.getAttrColor(context, android.R.attr.textColorPrimary))
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(anchor, 12), dp(anchor, 6), dp(anchor, 12), dp(anchor, 8))
        })

        lateinit var popup: PopupWindow
        val suggestionRow = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            background = context.getDrawable(R.drawable.pill_ripple)
            setPadding(dp(anchor, 12), 0, dp(anchor, 8), 0)
            val label = TextView(context).apply {
                setText(R.string.elyra_drawer_option_suggestions)
                setTextColor(Themes.getAttrColor(context, android.R.attr.textColorPrimary))
                textSize = 14f
                gravity = Gravity.CENTER_VERTICAL
            }
            val toggle = Switch(context).apply {
                isChecked = repo.isEnabled(ElyraFlag.DrawerSuggestions)
                isClickable = false
            }
            addView(label, LinearLayout.LayoutParams(0, dp(anchor, 48), 1f))
            addView(toggle, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(anchor, 48),
            ))
            setOnClickListener {
                repo.setEnabled(ElyraFlag.DrawerSuggestions, !toggle.isChecked)
                popup.dismiss()
                onChanged.run()
            }
        }
        panel.addView(suggestionRow, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(anchor, 48),
        ))

        val showAll = TextView(context).apply {
            setText(R.string.elyra_drawer_show_all_apps)
            setTextColor(Themes.getAttrColor(context, android.R.attr.textColorPrimary))
            textSize = 14f
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            background = context.getDrawable(R.drawable.pill_ripple)
            setPadding(dp(anchor, 12), 0, dp(anchor, 12), 0)
            setOnClickListener {
                popup.dismiss()
                onShowAll.run()
            }
        }
        panel.addView(showAll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(anchor, 48),
        ))

        popup = PopupWindow(
            panel,
            width,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true,
        ).apply {
            isOutsideTouchable = true
            elevation = dp(anchor, 10).toFloat()
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        popup.showAsDropDown(anchor, anchor.width - width, -dp(anchor, 4))
        panel.alpha = 0f
        panel.scaleX = 0.96f
        panel.scaleY = 0.96f
        panel.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(150)
            .setInterpolator(FastOutSlowInInterpolator())
            .start()
    }

    private fun dp(view: View, value: Int): Int =
        (value * view.resources.displayMetrics.density).toInt()
}
