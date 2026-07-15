/*
 * Copyright 2026, The Elyra Launcher Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.android.launcher3.allapps;

import static com.android.app.animation.Interpolators.EMPHASIZED;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.os.Parcelable;
import android.os.Trace;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.BuildConfig;
import com.android.launcher3.Utilities;
import com.android.launcher3.model.data.AppInfo;
import com.elyra.launcher.drawer.ElyraAppCategory;
import com.elyra.launcher.drawer.ElyraCategoryCardModel;
import com.elyra.launcher.drawer.ElyraCategoryMotionStateMachine;
import com.elyra.launcher.drawer.ElyraDrawerModelCoordinator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns the stable category root/detail transition. The real adapter is committed and laid out
 * before detail content becomes visible; no overlay, bounds morph, or per-icon animation exists.
 */
final class ElyraCategoryMotionController {
    private static final long ROOT_EXIT_MS = 80;
    private static final long DETAIL_ENTER_MS = 170;
    private static final long DETAIL_EXIT_MS = 80;
    private static final long ROOT_ENTER_MS = 150;
    private static final long TAB_EXIT_MS = 70;
    private static final long TAB_ENTER_MS = 120;
    private static final float ROOT_EXIT_SCALE = 0.985f;
    private static final float DETAIL_ENTER_SCALE = 0.975f;

    private final ActivityAllAppsContainerView<?> mContainer;
    private final ElyraCategoryMotionStateMachine mState =
            new ElyraCategoryMotionStateMachine();

    @Nullable private ValueAnimator mAnimator;
    @Nullable private ElyraCategoryCardModel.CategoryCard mCard;
    @Nullable private View mSourceView;
    @Nullable private Parcelable mRootScrollState;
    @Nullable private Parcelable mAllScrollState;
    @Nullable private Parcelable mCategoriesScrollState;
    private List<AppInfo> mDetailSnapshot = Collections.emptyList();
    private long mPreparationModelGeneration;
    private int mTransitionGeneration;
    private boolean mTabSwitching;

    @Nullable private RecyclerView mPendingRecyclerView;
    @Nullable private RecyclerView.Adapter<?> mPendingAdapter;
    @Nullable private RecyclerView.AdapterDataObserver mPendingAdapterObserver;
    @Nullable private Runnable mPendingCommitRunnable;
    @Nullable private Runnable mPendingBeforePreDrawAction;
    @Nullable private Runnable mPendingReadyAction;
    @Nullable private ViewTreeObserver.OnPreDrawListener mPendingPreDrawListener;

    private int mNextTraceCookie;
    private int mOpenTraceCookie;
    private int mCloseTraceCookie;
    private int mTabTraceCookie;

    ElyraCategoryMotionController(ActivityAllAppsContainerView<?> container) {
        mContainer = container;
    }

    boolean openCategory(View source, ElyraCategoryCardModel.CategoryCard card) {
        AllAppsRecyclerView rv = mContainer.getActiveRecyclerView();
        if (mTabSwitching || rv == null || rv.getAdapter() == null
                || rv.getLayoutManager() == null || !rv.isAttachedToWindow()
                || !source.isAttachedToWindow() || source.getWidth() <= 0
                || source.getHeight() <= 0 || card.getApps().isEmpty()
                || !mState.requestOpen()) {
            return false;
        }

        mCard = card;
        mSourceView = source;
        mRootScrollState = saveScrollState(rv);
        mDetailSnapshot = Collections.unmodifiableList(new ArrayList<>(card.getApps()));
        mPreparationModelGeneration = ElyraDrawerModelCoordinator.currentGeneration();
        int generation = beginTransition();
        mOpenTraceCookie = beginTrace("ElyraCategoryOpen");

        rv.stopScroll();
        rv.setEnabled(false);
        restoreViewProperties(rv);
        source.animate().cancel();
        source.setScaleX(0.975f);
        source.setScaleY(0.975f);
        source.animate().scaleX(1f).scaleY(1f).setDuration(90)
                .setInterpolator(EMPHASIZED).start();

        if (!ValueAnimator.areAnimatorsEnabled()) {
            rv.setAlpha(0f);
            beginDetailCommit(generation, rv);
            return true;
        }
        startAnimator(ROOT_EXIT_MS, generation, progress -> {
            rv.setAlpha(1f - progress);
            setScale(rv, lerp(1f, ROOT_EXIT_SCALE, progress));
            rv.setTranslationY(lerp(0f, -dp(6), progress));
        }, () -> beginDetailCommit(generation, rv));
        return true;
    }

    boolean canHandleBack() {
        return mState.getState() != ElyraCategoryMotionStateMachine.State.ROOT;
    }

    boolean handleBack() {
        ElyraCategoryMotionStateMachine.State state = mState.getState();
        if (state == ElyraCategoryMotionStateMachine.State.CLOSING) return true;
        if (state == ElyraCategoryMotionStateMachine.State.PREPARING
                || state == ElyraCategoryMotionStateMachine.State.OPENING) {
            mState.requestClose();
            restoreCategoryRoot(/* focusSource= */ true);
            return true;
        }
        if (state != ElyraCategoryMotionStateMachine.State.DETAIL
                || !mState.requestClose()) {
            return false;
        }
        startClose();
        return true;
    }

    void switchMode(boolean categories) {
        if (mTabSwitching || canHandleBack()
                || categories == mContainer.isElyraCategoryUiMode()) {
            return;
        }
        AllAppsRecyclerView rv = mContainer.getActiveRecyclerView();
        if (rv == null || rv.getAdapter() == null || !rv.isAttachedToWindow()) return;

        mTabSwitching = true;
        if (categories) mAllScrollState = saveScrollState(rv);
        else mCategoriesScrollState = saveScrollState(rv);
        int generation = beginTransition();
        mTabTraceCookie = beginTrace("ElyraDrawerTabSwitch");
        rv.stopScroll();
        rv.setEnabled(false);

        Runnable commit = () -> beginAdapterCommit(rv, generation, () -> {
            if (categories) mContainer.showElyraCategoryCardsForMotion();
            else mContainer.showElyraAllAppsForMotion();
        }, () -> restoreScrollState(
                rv, categories ? mCategoriesScrollState : mAllScrollState), () -> {
            rv.setAlpha(0f);
            rv.setTranslationY(categories ? dp(4) : -dp(4));
            if (!ValueAnimator.areAnimatorsEnabled()) {
                finishTabSwitch(rv);
                return;
            }
            startAnimator(TAB_ENTER_MS, generation, progress -> {
                rv.setAlpha(progress);
                rv.setTranslationY(lerp(categories ? dp(4) : -dp(4), 0f, progress));
            }, () -> finishTabSwitch(rv));
        });

        if (!ValueAnimator.areAnimatorsEnabled()) {
            rv.setAlpha(0f);
            commit.run();
            return;
        }
        startAnimator(TAB_EXIT_MS, generation, progress -> rv.setAlpha(1f - progress), commit);
    }

    /** Package/model changes invalidate preparation rather than mutating transition-owned data. */
    void onModelUpdated() {
        ElyraCategoryMotionStateMachine.State state = mState.getState();
        if (mTabSwitching) {
            cancelTabSwitch();
            return;
        }
        if (state == ElyraCategoryMotionStateMachine.State.PREPARING
                || state == ElyraCategoryMotionStateMachine.State.OPENING
                || state == ElyraCategoryMotionStateMachine.State.CLOSING) {
            restoreCategoryRoot(/* focusSource= */ false);
        } else if (state == ElyraCategoryMotionStateMachine.State.DETAIL
                && !mContainer.getPersonalAppList().canHandleElyraCategoryBack()) {
            restoreCategoryRoot(/* focusSource= */ false);
        }
    }

    void dispose() {
        invalidateTransition();
        if (mContainer.getPersonalAppList().canHandleElyraCategoryBack()) {
            mContainer.showElyraCategoryCardsForMotion();
        }
        cleanupTransitionViews();
        mState.forceRoot();
        clearCategorySession();
        mTabSwitching = false;
        endAllTraces();
    }

    private void beginDetailCommit(int generation, AllAppsRecyclerView rv) {
        if (!isCurrent(generation, ElyraCategoryMotionStateMachine.State.PREPARING)
                || mDetailSnapshot.isEmpty()
                || mPreparationModelGeneration
                != ElyraDrawerModelCoordinator.currentGeneration()) {
            restoreCategoryRoot(/* focusSource= */ false);
            return;
        }
        rv.setAlpha(0f);
        setScale(rv, DETAIL_ENTER_SCALE);
        rv.setTranslationY(dp(10));
        beginAdapterCommit(rv, generation,
                () -> {
                    beginSyncTrace("ElyraCategoryDetailCommit");
                    try {
                        mContainer.selectElyraCategoryForMotion(mCard.getCategory());
                    } finally {
                        endSyncTrace();
                    }
                },
                () -> rv.scrollToPosition(0),
                () -> onDetailPrepared(generation, rv));
    }

    private void onDetailPrepared(int generation, AllAppsRecyclerView rv) {
        if (!isCurrent(generation, ElyraCategoryMotionStateMachine.State.PREPARING)
                || mPreparationModelGeneration
                != ElyraDrawerModelCoordinator.currentGeneration()
                || !mContainer.getPersonalAppList().canHandleElyraCategoryBack()
                || rv.getAdapter() == null || rv.getAdapter().getItemCount() == 0
                || rv.getWidth() <= 0 || rv.getHeight() <= 0
                || !mState.markPrepared()) {
            restoreCategoryRoot(/* focusSource= */ false);
            return;
        }
        if (!ValueAnimator.areAnimatorsEnabled()) {
            finishOpen(rv);
            return;
        }
        startAnimator(DETAIL_ENTER_MS, generation, progress -> {
            rv.setAlpha(progress);
            setScale(rv, lerp(DETAIL_ENTER_SCALE, 1f, progress));
            rv.setTranslationY(lerp(dp(10), 0f, progress));
        }, () -> finishOpen(rv));
    }

    private void finishOpen(AllAppsRecyclerView rv) {
        if (!mState.markOpened()) return;
        cleanupTransitionViews();
        TextView title = findHeader(rv);
        if (title != null) {
            title.setFocusable(true);
            title.requestFocus();
            title.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        } else if (rv.getChildCount() > 0) {
            rv.getChildAt(0).requestFocus();
        }
        endTrace("ElyraCategoryOpen", mOpenTraceCookie);
        mOpenTraceCookie = 0;
        mContainer.updateElyraDrawerVisualState();
    }

    private void startClose() {
        AllAppsRecyclerView rv = mContainer.getActiveRecyclerView();
        if (rv == null || rv.getAdapter() == null) {
            restoreCategoryRoot(/* focusSource= */ true);
            return;
        }
        int generation = beginTransition();
        mCloseTraceCookie = beginTrace("ElyraCategoryClose");
        rv.stopScroll();
        rv.setEnabled(false);
        if (!ValueAnimator.areAnimatorsEnabled()) {
            rv.setAlpha(0f);
            beginRootCommit(generation, rv);
            return;
        }
        startAnimator(DETAIL_EXIT_MS, generation, progress -> {
            rv.setAlpha(1f - progress);
            setScale(rv, lerp(1f, ROOT_EXIT_SCALE, progress));
            rv.setTranslationY(lerp(0f, dp(6), progress));
        }, () -> beginRootCommit(generation, rv));
    }

    private void beginRootCommit(int generation, AllAppsRecyclerView rv) {
        if (!isCurrent(generation, ElyraCategoryMotionStateMachine.State.CLOSING)) return;
        rv.setAlpha(0f);
        setScale(rv, ROOT_EXIT_SCALE);
        rv.setTranslationY(-dp(6));
        beginAdapterCommit(rv, generation, mContainer::showElyraCategoryCardsForMotion,
                () -> restoreScrollState(rv, mRootScrollState), () -> {
            if (!ValueAnimator.areAnimatorsEnabled()) {
                finishClose(rv);
                return;
            }
            startAnimator(ROOT_ENTER_MS, generation, progress -> {
                rv.setAlpha(progress);
                setScale(rv, lerp(ROOT_EXIT_SCALE, 1f, progress));
                rv.setTranslationY(lerp(-dp(6), 0f, progress));
            }, () -> finishClose(rv));
        });
    }

    private void finishClose(AllAppsRecyclerView rv) {
        if (!mState.markClosed()) mState.forceRoot();
        ElyraAppCategory sourceCategory = mCard == null ? null : mCard.getCategory();
        cleanupTransitionViews();
        View source = findCategoryCard(rv, sourceCategory);
        if (source != null && source.isAttachedToWindow()) source.requestFocus();
        clearCategorySession();
        endTrace("ElyraCategoryClose", mCloseTraceCookie);
        mCloseTraceCookie = 0;
        mContainer.updateElyraDrawerVisualState();
    }

    private void restoreCategoryRoot(boolean focusSource) {
        ElyraAppCategory sourceCategory = mCard == null ? null : mCard.getCategory();
        int generation = invalidateTransition();
        mState.forceRoot();
        AllAppsRecyclerView rv = mContainer.getActiveRecyclerView();
        if (rv == null) {
            cleanupTransitionViews();
            clearCategorySession();
            endCategoryTraces();
            return;
        }
        rv.setAlpha(0f);
        rv.setEnabled(false);
        Runnable finish = () -> {
            cleanupTransitionViews();
            if (focusSource) {
                View source = findCategoryCard(rv, sourceCategory);
                if (source != null) source.requestFocus();
            }
            clearCategorySession();
            endCategoryTraces();
            mContainer.updateElyraDrawerVisualState();
        };
        if (mContainer.getPersonalAppList().canHandleElyraCategoryBack()) {
            beginAdapterCommit(rv, generation,
                    mContainer::showElyraCategoryCardsForMotion,
                    () -> restoreScrollState(rv, mRootScrollState), finish);
        } else {
            finish.run();
        }
    }

    private void beginAdapterCommit(AllAppsRecyclerView rv, int generation,
            Runnable mutation, Runnable beforePreDraw, Runnable ready) {
        clearPendingPreparation();
        RecyclerView.Adapter<?> adapter = rv.getAdapter();
        if (adapter == null || !rv.isAttachedToWindow() || generation != mTransitionGeneration) {
            abortInvalidPreparation();
            return;
        }
        mPendingRecyclerView = rv;
        mPendingAdapter = adapter;
        mPendingBeforePreDrawAction = beforePreDraw;
        mPendingReadyAction = ready;
        mPendingAdapterObserver = new RecyclerView.AdapterDataObserver() {
            @Override public void onChanged() { scheduleAdapterCommit(generation, rv); }
            @Override public void onItemRangeChanged(int start, int count) {
                scheduleAdapterCommit(generation, rv);
            }
            @Override public void onItemRangeInserted(int start, int count) {
                scheduleAdapterCommit(generation, rv);
            }
            @Override public void onItemRangeRemoved(int start, int count) {
                scheduleAdapterCommit(generation, rv);
            }
            @Override public void onItemRangeMoved(int from, int to, int count) {
                scheduleAdapterCommit(generation, rv);
            }
        };
        adapter.registerAdapterDataObserver(mPendingAdapterObserver);
        mutation.run();
        // A committed list can legitimately produce an empty DiffUtil dispatch. The next frame is
        // still the adapter commit boundary and is coalesced with observer callbacks below.
        scheduleAdapterCommit(generation, rv);
    }

    private void scheduleAdapterCommit(int generation, AllAppsRecyclerView rv) {
        if (mPendingCommitRunnable != null) return;
        mPendingCommitRunnable = () -> {
            Runnable callback = mPendingCommitRunnable;
            mPendingCommitRunnable = null;
            if (callback == null || generation != mTransitionGeneration
                    || rv != mPendingRecyclerView || !rv.isAttachedToWindow()
                    || rv.getAdapter() != mPendingAdapter) {
                clearPendingPreparation();
                return;
            }
            Runnable beforePreDraw = mPendingBeforePreDrawAction;
            Runnable ready = mPendingReadyAction;
            unregisterPendingAdapterObserver();
            mPendingBeforePreDrawAction = null;
            mPendingReadyAction = null;
            if (beforePreDraw != null) beforePreDraw.run();
            waitForValidPreDraw(rv, generation, ready);
        };
        rv.postOnAnimation(mPendingCommitRunnable);
    }

    private void waitForValidPreDraw(AllAppsRecyclerView rv, int generation,
            @Nullable Runnable ready) {
        if (ready == null) return;
        ViewTreeObserver observer = rv.getViewTreeObserver();
        if (!observer.isAlive()) {
            clearPendingPreparation();
            return;
        }
        mPendingRecyclerView = rv;
        mPendingReadyAction = ready;
        mPendingPreDrawListener = () -> {
            if (generation != mTransitionGeneration || !rv.isAttachedToWindow()) {
                clearPendingPreparation();
                return true;
            }
            RecyclerView.Adapter<?> adapter = rv.getAdapter();
            if (rv.getWidth() <= 0 || rv.getHeight() <= 0 || adapter == null
                    || adapter.getItemCount() <= 0) {
                return true;
            }
            Runnable action = mPendingReadyAction;
            removePendingPreDrawListener();
            mPendingReadyAction = null;
            mPendingRecyclerView = null;
            if (action != null) action.run();
            return true;
        };
        observer.addOnPreDrawListener(mPendingPreDrawListener);
        rv.invalidate();
    }

    private void startAnimator(long duration, int generation, ProgressConsumer update,
            Runnable end) {
        cancelAnimatorOnly();
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        mAnimator = animator;
        animator.setDuration(duration);
        animator.setInterpolator(EMPHASIZED);
        animator.addUpdateListener(value -> {
            if (generation == mTransitionGeneration) {
                update.accept((float) value.getAnimatedValue());
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean mCancelled;
            @Override public void onAnimationCancel(Animator animation) { mCancelled = true; }
            @Override public void onAnimationEnd(Animator animation) {
                if (mAnimator == animation) mAnimator = null;
                if (!mCancelled && generation == mTransitionGeneration) end.run();
            }
        });
        animator.start();
    }

    private int beginTransition() {
        clearPendingPreparation();
        cancelAnimatorOnly();
        return ++mTransitionGeneration;
    }

    private int invalidateTransition() {
        int generation = ++mTransitionGeneration;
        clearPendingPreparation();
        cancelAnimatorOnly();
        return generation;
    }

    private void cancelAnimatorOnly() {
        if (mAnimator != null) {
            ValueAnimator animator = mAnimator;
            mAnimator = null;
            animator.cancel();
        }
    }

    /** One deterministic visual/listener cleanup path for completion, cancellation, and detach. */
    private void cleanupTransitionViews() {
        clearPendingPreparation();
        cancelAnimatorOnly();
        restoreSource(mSourceView);
        AllAppsRecyclerView rv = mContainer.getActiveRecyclerView();
        if (rv != null) {
            rv.animate().cancel();
            restoreViewProperties(rv);
            rv.setEnabled(true);
            rv.setVisibility(View.VISIBLE);
        }
    }

    private void clearPendingPreparation() {
        if (mPendingRecyclerView != null && mPendingCommitRunnable != null) {
            mPendingRecyclerView.removeCallbacks(mPendingCommitRunnable);
        }
        mPendingCommitRunnable = null;
        unregisterPendingAdapterObserver();
        removePendingPreDrawListener();
        mPendingBeforePreDrawAction = null;
        mPendingReadyAction = null;
        mPendingRecyclerView = null;
    }

    private void unregisterPendingAdapterObserver() {
        if (mPendingAdapter != null && mPendingAdapterObserver != null) {
            mPendingAdapter.unregisterAdapterDataObserver(mPendingAdapterObserver);
        }
        mPendingAdapterObserver = null;
        mPendingAdapter = null;
    }

    private void removePendingPreDrawListener() {
        if (mPendingRecyclerView != null && mPendingPreDrawListener != null) {
            ViewTreeObserver observer = mPendingRecyclerView.getViewTreeObserver();
            if (observer.isAlive()) observer.removeOnPreDrawListener(mPendingPreDrawListener);
        }
        mPendingPreDrawListener = null;
    }

    private void cancelTabSwitch() {
        invalidateTransition();
        cleanupTransitionViews();
        mTabSwitching = false;
        endTrace("ElyraDrawerTabSwitch", mTabTraceCookie);
        mTabTraceCookie = 0;
    }

    private void abortInvalidPreparation() {
        if (mTabSwitching) {
            cancelTabSwitch();
            return;
        }
        mState.forceRoot();
        cleanupTransitionViews();
        clearCategorySession();
        endCategoryTraces();
        mContainer.updateElyraDrawerVisualState();
    }

    private void finishTabSwitch(AllAppsRecyclerView rv) {
        restoreViewProperties(rv);
        rv.setEnabled(true);
        mTabSwitching = false;
        endTrace("ElyraDrawerTabSwitch", mTabTraceCookie);
        mTabTraceCookie = 0;
        mContainer.updateElyraDrawerVisualState();
    }

    @Nullable
    private TextView findHeader(@Nullable RecyclerView rv) {
        if (rv == null) return null;
        for (int i = 0; i < rv.getChildCount(); i++) {
            View child = rv.getChildAt(i);
            RecyclerView.ViewHolder holder = rv.findContainingViewHolder(child);
            if (holder != null
                    && holder.getItemViewType()
                    == BaseAllAppsAdapter.VIEW_TYPE_ELYRA_CATEGORY_HEADER
                    && child instanceof TextView) {
                return (TextView) child;
            }
        }
        return null;
    }

    @Nullable
    private View findCategoryCard(@Nullable RecyclerView rv, @Nullable ElyraAppCategory category) {
        if (rv == null || category == null) return null;
        for (int i = 0; i < rv.getChildCount(); i++) {
            View child = rv.getChildAt(i);
            if (child.getTag() == category) return child;
        }
        return null;
    }

    @Nullable
    private Parcelable saveScrollState(RecyclerView rv) {
        return rv.getLayoutManager() == null ? null : rv.getLayoutManager().onSaveInstanceState();
    }

    private void restoreScrollState(RecyclerView rv, @Nullable Parcelable state) {
        if (state != null && rv.getLayoutManager() != null) {
            rv.getLayoutManager().onRestoreInstanceState(state);
        }
    }

    private void restoreSource(@Nullable View source) {
        if (source == null) return;
        source.animate().cancel();
        restoreViewProperties(source);
    }

    private static void restoreViewProperties(View view) {
        view.setAlpha(1f);
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.setTranslationX(0f);
        view.setTranslationY(0f);
    }

    private void clearCategorySession() {
        mCard = null;
        mSourceView = null;
        mRootScrollState = null;
        mDetailSnapshot = Collections.emptyList();
        mPreparationModelGeneration = 0;
    }

    private boolean isCurrent(int generation, ElyraCategoryMotionStateMachine.State state) {
        return generation == mTransitionGeneration && mState.getState() == state;
    }

    private int beginTrace(String name) {
        int cookie = ++mNextTraceCookie;
        if (BuildConfig.DEBUG && Utilities.ATLEAST_Q) Trace.beginAsyncSection(name, cookie);
        return cookie;
    }

    private void endTrace(String name, int cookie) {
        if (cookie != 0 && BuildConfig.DEBUG && Utilities.ATLEAST_Q) {
            Trace.endAsyncSection(name, cookie);
        }
    }

    private void endCategoryTraces() {
        endTrace("ElyraCategoryOpen", mOpenTraceCookie);
        endTrace("ElyraCategoryClose", mCloseTraceCookie);
        mOpenTraceCookie = mCloseTraceCookie = 0;
    }

    private void endAllTraces() {
        endCategoryTraces();
        endTrace("ElyraDrawerTabSwitch", mTabTraceCookie);
        mTabTraceCookie = 0;
    }

    private void beginSyncTrace(String name) {
        if (BuildConfig.DEBUG) Trace.beginSection(name);
    }

    private void endSyncTrace() {
        if (BuildConfig.DEBUG) Trace.endSection();
    }

    private int dp(int value) {
        return Math.round(value * mContainer.getResources().getDisplayMetrics().density);
    }

    private static void setScale(View view, float scale) {
        view.setScaleX(scale);
        view.setScaleY(scale);
    }

    private static float lerp(float from, float to, float amount) {
        return from + (to - from) * amount;
    }

    private interface ProgressConsumer { void accept(float progress); }
}
