/* Copyright (C) 2026 The Elyra Project */
package com.android.launcher3.views;

import androidx.annotation.Nullable;

import com.android.launcher3.allapps.ElyraSectionIndexSnapshot;

import java.util.Objects;

/** Owns the single adapter-bound request allowed to reach the next display frame. */
public final class ElyraCompactSectionUpdateState {
    @Nullable private ElyraSectionIndexSnapshot.Resolution mPendingRequest;
    private boolean mFrameScheduled;

    /** Replaces older work and returns true only when a new frame callback is needed. */
    public boolean replace(@Nullable ElyraSectionIndexSnapshot.Resolution request) {
        mPendingRequest = request;
        if (request == null || mFrameScheduled) return false;
        mFrameScheduled = true;
        return true;
    }

    /** Atomically consumes the request owned by the scheduled callback. */
    @Nullable
    public ElyraSectionIndexSnapshot.Resolution takeForFrame() {
        mFrameScheduled = false;
        ElyraSectionIndexSnapshot.Resolution request = mPendingRequest;
        mPendingRequest = null;
        return request;
    }

    public void clear() {
        mPendingRequest = null;
        mFrameScheduled = false;
    }

    public boolean isFrameScheduled() {
        return mFrameScheduled;
    }

    public boolean hasPendingRequest() {
        return mPendingRequest != null;
    }

    /** Content comparison for optional popup labels; null represents no section. */
    public static boolean labelsEqual(@Nullable CharSequence first,
            @Nullable CharSequence second) {
        return Objects.equals(first == null ? null : first.toString(),
                second == null ? null : second.toString());
    }
}
