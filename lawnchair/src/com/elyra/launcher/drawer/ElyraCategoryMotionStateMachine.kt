/*
 * Copyright 2026, The Elyra Launcher Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package com.elyra.launcher.drawer

/** Cancellation-safe ownership for category root/detail navigation. */
class ElyraCategoryMotionStateMachine {
    enum class State {
        ROOT,
        CATEGORY_OPENING,
        CATEGORY_DETAIL,
        CATEGORY_CLOSING,
    }

    var state: State = State.ROOT
        private set

    fun requestOpen(): Boolean {
        if (state != State.ROOT) return false
        state = State.CATEGORY_OPENING
        return true
    }

    fun markOpened(): Boolean {
        if (state != State.CATEGORY_OPENING) return false
        state = State.CATEGORY_DETAIL
        return true
    }

    fun requestClose(): Boolean {
        if (state != State.CATEGORY_OPENING && state != State.CATEGORY_DETAIL) return false
        state = State.CATEGORY_CLOSING
        return true
    }

    fun markClosed(): Boolean {
        if (state != State.CATEGORY_CLOSING) return false
        state = State.ROOT
        return true
    }

    /** Restores a valid non-transitional state after teardown or model replacement. */
    fun forceRoot() {
        state = State.ROOT
    }
}
