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

package com.elyra.launcher.theme

/**
 * Elyra design tokens expressed as code constants.
 *
 * Color, spacing, radius, icon-size and layout-surface tokens live in resources
 * (`lawnchair/res/values/elyra_tokens.xml` and its `-night` variant) so they
 * follow the platform light/dark and density system. The constants here are the
 * motion and effect tokens that later stages will consume.
 *
 * Nothing in Stage 3 reads these values, so they do not change any current
 * launcher behavior. They exist only as reusable, non-faked building blocks.
 */
object ElyraMotionTokens {
    // Base durations (milliseconds).
    const val DURATION_FAST_MS = 150
    const val DURATION_MEDIUM_MS = 300
    const val DURATION_SLOW_MS = 450

    // Spring stiffness presets (higher = snappier).
    const val SPRING_STIFFNESS_LOW = 200f
    const val SPRING_STIFFNESS_MEDIUM = 700f
    const val SPRING_STIFFNESS_HIGH = 1400f

    // Spring damping ratios (1.0 = critically damped, no overshoot).
    const val SPRING_DAMPING_LOW = 0.6f
    const val SPRING_DAMPING_MEDIUM = 0.8f
    const val SPRING_DAMPING_HIGH = 1.0f

    // Surface-specific transition durations (milliseconds).
    const val DRAWER_TRANSITION_MS = 300
    const val FOLDER_TRANSITION_MS = 260
    const val EDIT_MODE_TRANSITION_MS = 240

    // Press feedback.
    const val ICON_PRESS_SCALE = 0.92f
}

/**
 * Elyra blur / effect configuration tokens.
 *
 * These are configuration values only. No blur or glow is rendered in Stage 3;
 * the enabled mappings simply forward a flag's state so later stages have a
 * single, non-faked place to decide whether an effect should run.
 */
object ElyraEffectTokens {
    // Blur radii in dp; DISABLED is the safe no-op default.
    const val BLUR_DISABLED_DP = 0
    const val BLUR_RADIUS_LOW_DP = 8
    const val BLUR_RADIUS_MEDIUM_DP = 16
    const val BLUR_RADIUS_HIGH_DP = 24

    /**
     * Placeholder device "performance class" threshold. Devices reporting a class
     * below this fall back to disabled effects. Wired to a real capability check
     * in a later stage.
     */
    const val LOW_END_PERFORMANCE_CLASS_THRESHOLD = 29

    /** Developer-only frame/jank logging; off by default, wired later. */
    const val DEBUG_FRAME_LOGGING_DEFAULT = false

    /** Progressive blur renders only when its flag is enabled. */
    fun progressiveBlurEnabled(flagEnabled: Boolean): Boolean = flagEnabled

    /** Contour glow renders only when its flag is enabled. */
    fun contourGlowEnabled(flagEnabled: Boolean): Boolean = flagEnabled
}
