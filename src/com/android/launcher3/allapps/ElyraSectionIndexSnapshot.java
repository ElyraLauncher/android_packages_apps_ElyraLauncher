/*
 * Copyright (C) 2026 The Elyra Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */
package com.android.launcher3.allapps;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Locale;

/** Immutable adapter-bound index used by the compact All Apps rail. */
public final class ElyraSectionIndexSnapshot {
    public static final String INDEX_LABELS = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final int NO_POSITION = -1;

    public enum Mode { ALL_APPS, FILTERED, CATEGORIES }

    public static final class Resolution {
        public final int position;
        @NonNull public final String requestedSection;
        @NonNull public final String resolvedSection;

        private Resolution(int position, @NonNull String requestedSection,
                @NonNull String resolvedSection) {
            this.position = position;
            this.requestedSection = requestedSection;
            this.resolvedSection = resolvedSection;
        }
    }

    private final long mGeneration;
    @NonNull private final Mode mMode;
    private final int mItemCount;
    private final int mLeadingNonAppRows;
    @NonNull private final Object mAdapterToken;
    private final int[] mPositions;
    private final String[] mResolvedSections;

    private ElyraSectionIndexSnapshot(long generation, @NonNull Mode mode, int itemCount,
            int leadingNonAppRows, @NonNull Object adapterToken, int[] positions,
            String[] resolvedSections) {
        mGeneration = generation;
        mMode = mode;
        mItemCount = itemCount;
        mLeadingNonAppRows = leadingNonAppRows;
        mAdapterToken = adapterToken;
        mPositions = positions;
        mResolvedSections = resolvedSections;
    }

    /** Builds one detached snapshot from the displayed adapter model. */
    @NonNull
    public static ElyraSectionIndexSnapshot create(long generation, @NonNull Mode mode,
            int itemCount, @NonNull Object adapterToken, @NonNull CharSequence[] sectionNames,
            @NonNull int[] sectionPositions) {
        int[] positions = new int[INDEX_LABELS.length()];
        String[] resolvedSections = new String[INDEX_LABELS.length()];
        Arrays.fill(positions, NO_POSITION);

        int validCount = Math.min(sectionNames.length, sectionPositions.length);
        int firstValidIndex = NO_POSITION;
        int lastValidIndex = NO_POSITION;
        int leadingRows = 0;
        for (int i = 0; i < validCount; i++) {
            if (!isValidSection(sectionNames, sectionPositions, i, itemCount)) continue;
            if (firstValidIndex == NO_POSITION) {
                firstValidIndex = i;
                leadingRows = sectionPositions[i];
            }
            lastValidIndex = i;
        }

        if (mode == Mode.ALL_APPS && firstValidIndex != NO_POSITION) {
            positions[0] = sectionPositions[firstValidIndex];
            resolvedSections[0] = normalizedSection(sectionNames[firstValidIndex]);
            for (int targetIndex = 1; targetIndex < INDEX_LABELS.length(); targetIndex++) {
                char target = INDEX_LABELS.charAt(targetIndex);
                int selectedIndex = NO_POSITION;
                for (int i = firstValidIndex; i <= lastValidIndex; i++) {
                    if (!isValidSection(sectionNames, sectionPositions, i, itemCount)) continue;
                    String normalized = normalizedSection(sectionNames[i]);
                    char initial = normalized.charAt(0);
                    if (initial >= 'A' && initial <= 'Z' && initial >= target) {
                        selectedIndex = i;
                        break;
                    }
                }
                if (selectedIndex == NO_POSITION) selectedIndex = lastValidIndex;
                if (isValidSection(sectionNames, sectionPositions, selectedIndex, itemCount)) {
                    positions[targetIndex] = sectionPositions[selectedIndex];
                    resolvedSections[targetIndex] = normalizedSection(sectionNames[selectedIndex]);
                }
            }
        }
        return new ElyraSectionIndexSnapshot(generation, mode, Math.max(0, itemCount),
                leadingRows, adapterToken, positions, resolvedSections);
    }

    @Nullable
    public Resolution resolve(int index, long currentGeneration, @NonNull Mode currentMode,
            int currentItemCount, @NonNull Object currentAdapterToken) {
        if (index < 0 || index >= INDEX_LABELS.length()
                || mMode != Mode.ALL_APPS || currentMode != Mode.ALL_APPS
                || mGeneration != currentGeneration || mItemCount != currentItemCount
                || mAdapterToken != currentAdapterToken) {
            return null;
        }
        int position = mPositions[index];
        if (position < 0 || position >= currentItemCount) return null;
        String resolved = mResolvedSections[index];
        return resolved == null ? null : new Resolution(position,
                String.valueOf(INDEX_LABELS.charAt(index)), resolved);
    }

    public long getGeneration() { return mGeneration; }
    public int getLeadingNonAppRows() { return mLeadingNonAppRows; }

    private static boolean isValidSection(CharSequence[] names, int[] positions, int index,
            int itemCount) {
        return index >= 0 && index < names.length && index < positions.length
                && names[index] != null && !names[index].toString().trim().isEmpty()
                && positions[index] >= 0 && positions[index] < itemCount;
    }

    @NonNull
    private static String normalizedSection(@NonNull CharSequence section) {
        return section.toString().trim().toUpperCase(Locale.ROOT);
    }
}
