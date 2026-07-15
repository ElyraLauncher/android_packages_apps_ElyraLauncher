/*
 * Copyright 2026, The Elyra Launcher Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.android.launcher3.allapps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.elyra.launcher.drawer.ElyraCategoryMotionStateMachine;

import org.junit.Test;

public class ElyraCategoryMotionStateMachineTest {

    @Test
    public void openAndClose_followValidStates() {
        ElyraCategoryMotionStateMachine state = new ElyraCategoryMotionStateMachine();

        assertTrue(state.requestOpen());
        assertEquals(ElyraCategoryMotionStateMachine.State.CATEGORY_OPENING, state.getState());
        assertTrue(state.markOpened());
        assertEquals(ElyraCategoryMotionStateMachine.State.CATEGORY_DETAIL, state.getState());
        assertTrue(state.requestClose());
        assertEquals(ElyraCategoryMotionStateMachine.State.CATEGORY_CLOSING, state.getState());
        assertTrue(state.markClosed());
        assertEquals(ElyraCategoryMotionStateMachine.State.CATEGORIES_ROOT, state.getState());
    }

    @Test
    public void repeatedOpen_doesNotCreateSecondTransition() {
        ElyraCategoryMotionStateMachine state = new ElyraCategoryMotionStateMachine();

        assertTrue(state.requestOpen());
        assertFalse(state.requestOpen());
        assertEquals(ElyraCategoryMotionStateMachine.State.CATEGORY_OPENING, state.getState());
    }

    @Test
    public void backDuringOpen_entersClosingAndCanRecover() {
        ElyraCategoryMotionStateMachine state = new ElyraCategoryMotionStateMachine();

        assertTrue(state.requestOpen());
        assertTrue(state.requestClose());
        assertFalse(state.markOpened());
        assertTrue(state.markClosed());
        assertEquals(ElyraCategoryMotionStateMachine.State.CATEGORIES_ROOT, state.getState());
    }

    @Test
    public void teardown_alwaysRestoresStableRoot() {
        ElyraCategoryMotionStateMachine state = new ElyraCategoryMotionStateMachine();

        state.requestOpen();
        state.forceRoot();
        assertEquals(ElyraCategoryMotionStateMachine.State.CATEGORIES_ROOT, state.getState());
        assertFalse(state.requestClose());
    }
}
