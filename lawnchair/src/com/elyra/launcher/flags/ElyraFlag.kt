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

package com.elyra.launcher.flags

/**
 * A single Elyra feature flag.
 *
 * Every future Elyra feature is gated behind one of these flags. Flags default
 * OFF so that merely adding a flag never changes current launcher behavior; a
 * feature becomes visible only once its implementation stage explicitly turns it
 * on. This is pure Kotlin (no Android dependencies) so it can be unit tested.
 *
 * @property key stable DataStore key; it is the migration contract and must never
 *   change once shipped.
 * @property default the default value; risky/unfinished behavior defaults OFF.
 * @property romOnly true when the feature genuinely requires a privileged /
 *   platform-integrated (ROM) build and can never take effect in the universal
 *   APK. The universal build must show a graceful fallback rather than fake it.
 */
enum class ElyraFlag(
    val key: String,
    val default: Boolean,
    val romOnly: Boolean = false,
) {
    BottomSearch(key = "elyra_bottom_search", default = false),
    DrawerCategories(key = "elyra_drawer_categories", default = false),
    DrawerSuggestions(key = "elyra_drawer_suggestions", default = false),
    AzRail(key = "elyra_az_rail", default = false),
    WallpaperThemedIcons(key = "elyra_wallpaper_themed_icons", default = false),
    IconResize(key = "elyra_icon_resize", default = false),
    LargeFolders(key = "elyra_large_folders", default = false),
    FolderDirectLaunch(key = "elyra_folder_direct_launch", default = false),
    FlowMotion(key = "elyra_flow_motion", default = false),
    ProgressiveBlur(key = "elyra_progressive_blur", default = false),
    ContourGlow(key = "elyra_contour_glow", default = false),
    EditMode(key = "elyra_edit_mode", default = false),
    LayoutLock(key = "elyra_layout_lock", default = false),
    LayoutHistory(key = "elyra_layout_history", default = false),
    SmartArrange(key = "elyra_smart_arrange", default = false),
    HiddenApps(key = "elyra_hidden_apps", default = false),
    TaskbarUniversal(key = "elyra_taskbar_universal", default = false),
    QuickstepRom(key = "elyra_quickstep_rom", default = false, romOnly = true),
}
