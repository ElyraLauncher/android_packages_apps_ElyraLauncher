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

import android.view.Menu
import android.view.View
import android.widget.PopupMenu
import com.android.launcher3.R
import com.elyra.launcher.flags.ElyraFlag
import com.elyra.launcher.flags.ElyraFlagsRepository

/**
 * Drawer-scoped overflow menu for the app-drawer header three-dot button.
 *
 * This is intentionally *not* an entry point into Elyra Labs or the full Elyra
 * settings: it only exposes the local drawer feature toggles that shape the
 * drawer itself. Every option maps to an existing Elyra drawer flag; toggling
 * one persists the flag and asks the caller to rebuild the drawer via
 * [onChanged]. Categories/suggestions re-read on the next list rebuild and so
 * update immediately; the A-Z index and color-search chrome are read when the
 * drawer is constructed and take effect the next time it is opened.
 */
object ElyraDrawerOptions {

    private val OPTIONS = listOf(
        ElyraFlag.DrawerSuggestions to R.string.elyra_drawer_option_suggestions,
    )

    @JvmStatic
    fun show(anchor: View, onChanged: Runnable) {
        val context = anchor.context
        val repo = ElyraFlagsRepository.getInstance(context)
        val popup = PopupMenu(context, anchor)
        val menu = popup.menu

        // Non-clickable title row so the popup reads as "App drawer options".
        menu.add(Menu.NONE, 0, 0, R.string.elyra_drawer_options_title).isEnabled = false

        OPTIONS.forEachIndexed { index, (flag, labelRes) ->
            menu.add(Menu.NONE, index + 1, index + 1, labelRes).apply {
                isCheckable = true
                isChecked = repo.isEnabled(flag)
            }
        }

        popup.setOnMenuItemClickListener { item ->
            val option = OPTIONS.getOrNull(item.itemId - 1)
                ?: return@setOnMenuItemClickListener false
            val flag = option.first
            repo.setEnabled(flag, !repo.isEnabled(flag))
            onChanged.run()
            true
        }
        popup.show()
    }
}
