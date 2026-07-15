/* Copyright (C) 2026 The Elyra Project */
package com.android.launcher3.allapps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ElyraSectionIndexSnapshotTest {
    private static final long ADAPTER_GENERATION = 7;
    private static final long FILTER_GENERATION = 3;
    private final Object adapter = new Object();
    private final CharSequence[] names = {"A", "M", "Z"};
    private final int[] positions = {2, 14, 27};

    @Test
    public void resolvesHashLettersAndLeadingRows() {
        ElyraSectionIndexSnapshot snapshot = snapshot(28);
        assertEquals(2, snapshot.getLeadingNonAppRows());
        assertEquals(2, resolve(snapshot, 0, 28).position);
        assertEquals(2, resolve(snapshot, 1, 28).position);
        assertEquals(27, resolve(snapshot, 26, 28).position);
        assertEquals(14, resolve(snapshot, 2, 28).position);
    }

    @Test
    public void rejectsStaleInvalidAndWrongModeRequests() {
        ElyraSectionIndexSnapshot snapshot = snapshot(28);
        assertNull(snapshot.resolve(1, 8, FILTER_GENERATION,
                ElyraSectionIndexSnapshot.Mode.ALL_APPS, 28, adapter));
        assertNull(snapshot.resolve(1, ADAPTER_GENERATION, FILTER_GENERATION + 1,
                ElyraSectionIndexSnapshot.Mode.ALL_APPS, 28, adapter));
        assertNull(snapshot.resolve(-1, ADAPTER_GENERATION, FILTER_GENERATION,
                ElyraSectionIndexSnapshot.Mode.ALL_APPS, 28, adapter));
        assertNull(snapshot.resolve(27, ADAPTER_GENERATION, FILTER_GENERATION,
                ElyraSectionIndexSnapshot.Mode.ALL_APPS, 28, adapter));
        assertNull(snapshot.resolve(1, ADAPTER_GENERATION, FILTER_GENERATION,
                ElyraSectionIndexSnapshot.Mode.FILTERED, 28, adapter));
        assertNull(snapshot.resolve(1, ADAPTER_GENERATION, FILTER_GENERATION,
                ElyraSectionIndexSnapshot.Mode.ALL_APPS, 27, adapter));
        assertNull(snapshot.resolve(1, ADAPTER_GENERATION, FILTER_GENERATION,
                ElyraSectionIndexSnapshot.Mode.ALL_APPS, 28, new Object()));
    }

    @Test
    public void invalidPositionsAndEmptyAdaptersAreSafe() {
        ElyraSectionIndexSnapshot invalid = ElyraSectionIndexSnapshot.create(
                ADAPTER_GENERATION, FILTER_GENERATION,
                ElyraSectionIndexSnapshot.Mode.ALL_APPS, 28, adapter,
                new CharSequence[] {"A", "Z"}, new int[] {-1, 28});
        assertNull(invalid.resolve(0, ADAPTER_GENERATION, FILTER_GENERATION,
                ElyraSectionIndexSnapshot.Mode.ALL_APPS, 28, adapter));
        ElyraSectionIndexSnapshot empty = ElyraSectionIndexSnapshot.create(
                ADAPTER_GENERATION, FILTER_GENERATION,
                ElyraSectionIndexSnapshot.Mode.ALL_APPS, 0, adapter,
                new CharSequence[0], new int[0]);
        assertNull(empty.resolve(0, ADAPTER_GENERATION, FILTER_GENERATION,
                ElyraSectionIndexSnapshot.Mode.ALL_APPS, 0, adapter));
    }

    @Test
    public void filteredAndCategorySnapshotsCannotDriveAllApps() {
        for (ElyraSectionIndexSnapshot.Mode mode : new ElyraSectionIndexSnapshot.Mode[] {
                ElyraSectionIndexSnapshot.Mode.FILTERED,
                ElyraSectionIndexSnapshot.Mode.CATEGORIES}) {
            ElyraSectionIndexSnapshot snapshot = ElyraSectionIndexSnapshot.create(
                    ADAPTER_GENERATION, FILTER_GENERATION, mode, 28, adapter, names, positions);
            assertNull(snapshot.resolve(1, ADAPTER_GENERATION, FILTER_GENERATION,
                    ElyraSectionIndexSnapshot.Mode.ALL_APPS, 28, adapter));
        }
    }

    @Test
    public void missingSymbolsNeverProduceInvalidAdapterPositions() {
        ElyraSectionIndexSnapshot snapshot = ElyraSectionIndexSnapshot.create(
                ADAPTER_GENERATION, FILTER_GENERATION,
                ElyraSectionIndexSnapshot.Mode.ALL_APPS, 9, adapter,
                new CharSequence[] {"M"}, new int[] {4});
        for (int index : new int[] {0, 1, 26}) {
            ElyraSectionIndexSnapshot.Resolution result = snapshot.resolve(index,
                    ADAPTER_GENERATION, FILTER_GENERATION,
                    ElyraSectionIndexSnapshot.Mode.ALL_APPS, 9, adapter);
            assertNotNull(result);
            assertEquals(4, result.position);
        }
    }

    private ElyraSectionIndexSnapshot snapshot(int itemCount) {
        return ElyraSectionIndexSnapshot.create(ADAPTER_GENERATION, FILTER_GENERATION,
                ElyraSectionIndexSnapshot.Mode.ALL_APPS, itemCount, adapter, names, positions);
    }

    private ElyraSectionIndexSnapshot.Resolution resolve(
            ElyraSectionIndexSnapshot snapshot, int index, int itemCount) {
        ElyraSectionIndexSnapshot.Resolution result = snapshot.resolve(index,
                ADAPTER_GENERATION, FILTER_GENERATION,
                ElyraSectionIndexSnapshot.Mode.ALL_APPS, itemCount, adapter);
        assertNotNull(result);
        return result;
    }
}
