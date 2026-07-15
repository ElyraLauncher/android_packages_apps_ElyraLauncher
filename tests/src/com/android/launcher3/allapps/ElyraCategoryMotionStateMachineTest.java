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
        assertEquals(ElyraCategoryMotionStateMachine.State.PREPARING, state.getState());
        assertTrue(state.markPrepared());
        assertEquals(ElyraCategoryMotionStateMachine.State.OPENING, state.getState());
        assertTrue(state.markOpened());
        assertEquals(ElyraCategoryMotionStateMachine.State.DETAIL, state.getState());
        assertTrue(state.requestClose());
        assertEquals(ElyraCategoryMotionStateMachine.State.CLOSING, state.getState());
        assertTrue(state.markClosed());
        assertEquals(ElyraCategoryMotionStateMachine.State.ROOT, state.getState());
    }

    @Test
    public void repeatedOpen_doesNotCreateSecondTransition() {
        ElyraCategoryMotionStateMachine state = new ElyraCategoryMotionStateMachine();

        assertTrue(state.requestOpen());
        assertFalse(state.requestOpen());
        assertEquals(ElyraCategoryMotionStateMachine.State.PREPARING, state.getState());
    }

    @Test
    public void detailCannotOpenBeforePreparationCommit() {
        ElyraCategoryMotionStateMachine state = new ElyraCategoryMotionStateMachine();

        assertTrue(state.requestOpen());
        assertFalse(state.markOpened());
        assertEquals(ElyraCategoryMotionStateMachine.State.PREPARING, state.getState());
        assertTrue(state.markPrepared());
        assertTrue(state.markOpened());
        assertEquals(ElyraCategoryMotionStateMachine.State.DETAIL, state.getState());
    }

    @Test
    public void backDuringPreparation_entersClosingAndCanRecover() {
        ElyraCategoryMotionStateMachine state = new ElyraCategoryMotionStateMachine();

        assertTrue(state.requestOpen());
        assertTrue(state.requestClose());
        assertEquals(ElyraCategoryMotionStateMachine.State.CLOSING, state.getState());
        assertFalse(state.markPrepared());
        assertFalse(state.markOpened());
        assertTrue(state.markClosed());
        assertEquals(ElyraCategoryMotionStateMachine.State.ROOT, state.getState());
    }

    @Test
    public void backDuringOpening_entersClosingAndCanRecover() {
        ElyraCategoryMotionStateMachine state = new ElyraCategoryMotionStateMachine();

        assertTrue(state.requestOpen());
        assertTrue(state.markPrepared());
        assertTrue(state.requestClose());
        assertFalse(state.markOpened());
        assertTrue(state.markClosed());
        assertEquals(ElyraCategoryMotionStateMachine.State.ROOT, state.getState());
    }

    @Test
    public void teardown_alwaysRestoresStableRoot() {
        ElyraCategoryMotionStateMachine state = new ElyraCategoryMotionStateMachine();

        state.requestOpen();
        state.forceRoot();
        assertEquals(ElyraCategoryMotionStateMachine.State.ROOT, state.getState());
        assertFalse(state.requestClose());
    }
}
