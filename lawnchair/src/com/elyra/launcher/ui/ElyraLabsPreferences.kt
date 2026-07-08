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

package com.elyra.launcher.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.lawnchair.preferences.getAdapter
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.components.controls.SwitchPreference
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLayout
import com.android.launcher3.R
import com.elyra.launcher.flags.ElyraFlag
import com.elyra.launcher.flags.ElyraFlagStore

/**
 * "Elyra Labs" — the experimental feature-flag screen.
 *
 * Every Elyra feature flag is exposed here as a labelled toggle so features can be
 * enabled for development and testing. Flags default OFF and the features behind
 * them are not implemented yet in this stage, so the toggles do not claim finished
 * behavior; ROM-only flags are annotated as such and cannot take effect in the
 * universal APK.
 */
@Composable
fun ElyraLabsPreferences(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val store = remember { ElyraFlagStore.getInstance(context) }
    PreferenceLayout(
        label = stringResource(id = R.string.elyra_labs_label),
        backArrowVisible = !LocalIsExpandedScreen.current,
        modifier = modifier,
    ) {
        PreferenceGroup(
            heading = stringResource(id = R.string.elyra_experimental_features),
            description = stringResource(id = R.string.elyra_restart_may_be_required),
        ) {
            ElyraFlag.entries.forEach { flag ->
                val descriptionRes = flag.descriptionRes()
                SwitchPreference(
                    adapter = store.preferenceFor(flag).getAdapter(),
                    label = stringResource(id = flag.labelRes()),
                    description = when {
                        flag.romOnly -> stringResource(id = R.string.elyra_rom_only_note)
                        descriptionRes != null -> stringResource(id = descriptionRes)
                        else -> null
                    },
                )
            }
        }
    }
}

/**
 * Optional one-line description shown under a flag toggle. Only flags whose
 * behavior benefits from a short explanation return a value; the rest return null
 * and render as a plain toggle (ROM-only flags are annotated separately).
 */
@StringRes
private fun ElyraFlag.descriptionRes(): Int? = when (this) {
    ElyraFlag.BottomSearch -> R.string.elyra_flag_bottom_search_description
    ElyraFlag.DrawerWebResults -> R.string.elyra_flag_drawer_web_results_description
    else -> null
}

@StringRes
private fun ElyraFlag.labelRes(): Int = when (this) {
    ElyraFlag.BottomSearch -> R.string.elyra_flag_bottom_search
    ElyraFlag.DrawerWebResults -> R.string.elyra_flag_drawer_web_results
    ElyraFlag.DrawerCategories -> R.string.elyra_flag_drawer_categories
    ElyraFlag.DrawerSuggestions -> R.string.elyra_flag_drawer_suggestions
    ElyraFlag.AzRail -> R.string.elyra_flag_az_rail
    ElyraFlag.WallpaperThemedIcons -> R.string.elyra_flag_wallpaper_themed_icons
    ElyraFlag.IconResize -> R.string.elyra_flag_icon_resize
    ElyraFlag.LargeFolders -> R.string.elyra_flag_large_folders
    ElyraFlag.FolderDirectLaunch -> R.string.elyra_flag_folder_direct_launch
    ElyraFlag.FlowMotion -> R.string.elyra_flag_flow_motion
    ElyraFlag.ProgressiveBlur -> R.string.elyra_flag_progressive_blur
    ElyraFlag.ContourGlow -> R.string.elyra_flag_contour_glow
    ElyraFlag.EditMode -> R.string.elyra_flag_edit_mode
    ElyraFlag.LayoutLock -> R.string.elyra_flag_layout_lock
    ElyraFlag.LayoutHistory -> R.string.elyra_flag_layout_history
    ElyraFlag.SmartArrange -> R.string.elyra_flag_smart_arrange
    ElyraFlag.HiddenApps -> R.string.elyra_flag_hidden_apps
    ElyraFlag.TaskbarUniversal -> R.string.elyra_flag_taskbar_universal
    ElyraFlag.QuickstepRom -> R.string.elyra_flag_quickstep_rom
}
