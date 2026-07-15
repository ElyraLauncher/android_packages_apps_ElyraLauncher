/* Copyright (C) 2026 The Elyra Project */
package com.android.launcher3.views;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.android.launcher3.allapps.ElyraSectionIndexSnapshot;

import org.junit.Test;

public class ElyraCompactSectionUpdateStateTest {
    private final Object adapter = new Object();

    @Test
    public void nullAndClearedPendingRequestAreIgnored() {
        ElyraCompactSectionUpdateState state = new ElyraCompactSectionUpdateState();
        assertFalse(state.replace(null));
        assertFalse(state.isFrameScheduled());
        assertNull(state.takeForFrame());
        state.clear();
        assertNull(state.takeForFrame());
    }

    @Test
    public void latestRequestWinsSingleScheduledFrame() {
        ElyraCompactSectionUpdateState state = new ElyraCompactSectionUpdateState();
        ElyraSectionIndexSnapshot snapshot = snapshot();
        ElyraSectionIndexSnapshot.Resolution first = resolve(snapshot, 1);
        ElyraSectionIndexSnapshot.Resolution latest = resolve(snapshot, 26);

        assertTrue(state.replace(first));
        assertFalse(state.replace(latest));
        assertTrue(state.isFrameScheduled());
        assertSame(latest, state.takeForFrame());
        assertFalse(state.isFrameScheduled());
        assertNull(state.takeForFrame());
    }

    @Test
    public void optionalLabelsCompareWithoutNullCrash() {
        assertTrue(ElyraCompactSectionUpdateState.labelsEqual(null, null));
        assertFalse(ElyraCompactSectionUpdateState.labelsEqual("A", null));
        assertFalse(ElyraCompactSectionUpdateState.labelsEqual(null, "A"));
        assertTrue(ElyraCompactSectionUpdateState.labelsEqual("A", new StringBuilder("A")));
    }

    private ElyraSectionIndexSnapshot snapshot() {
        return ElyraSectionIndexSnapshot.create(4, 2,
                ElyraSectionIndexSnapshot.Mode.ALL_APPS, 12, adapter,
                new CharSequence[] {"A", "Z"}, new int[] {2, 11});
    }

    private ElyraSectionIndexSnapshot.Resolution resolve(
            ElyraSectionIndexSnapshot snapshot, int index) {
        return snapshot.resolve(index, 4, 2, ElyraSectionIndexSnapshot.Mode.ALL_APPS,
                12, adapter);
    }
}
