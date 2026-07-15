/*
 * Copyright 2026, The Elyra Launcher Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.android.launcher3.allapps;

import static com.android.app.animation.Interpolators.EMPHASIZED;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Parcelable;
import android.os.Trace;
import android.text.TextPaint;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.BuildConfig;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.elyra.launcher.drawer.ElyraAppCategory;
import com.elyra.launcher.drawer.ElyraCategoryCardModel;
import com.elyra.launcher.drawer.ElyraCategoryMotionStateMachine;

/**
 * Owns one shared-container category transition while the real All Apps model remains
 * authoritative. The stabilization baseline intentionally animates no individual app icons.
 */
final class ElyraCategoryMotionController {
    private static final long OPEN_MS = 290;
    private static final long CLOSE_MS = 265;
    private static final float OPEN_CONTENT_SCALE = 0.97f;
    private static final float ROOT_CONTENT_SCALE = 0.985f;

    private final ActivityAllAppsContainerView<?> mContainer;
    private final ElyraCategoryMotionStateMachine mState =
            new ElyraCategoryMotionStateMachine();
    private final int[] mContainerLocation = new int[2];
    private final int[] mViewLocation = new int[2];

    @Nullable private ValueAnimator mAnimator;
    @Nullable private MorphView mMorph;
    @Nullable private ElyraCategoryCardModel.CategoryCard mCard;
    @Nullable private Parcelable mRootScrollState;
    @Nullable private Parcelable mAllScrollState;
    @Nullable private Parcelable mCategoriesScrollState;
    @Nullable private RectF mSourceBounds;
    @Nullable private View mSourceView;
    @Nullable private View mClosingSource;
    private int mTransitionGeneration;
    private int mNextTraceCookie;
    private int mOpenTraceCookie;
    private int mCloseTraceCookie;
    private int mTabTraceCookie;
    private boolean mTabSwitching;

    ElyraCategoryMotionController(ActivityAllAppsContainerView<?> container) {
        mContainer = container;
    }

    boolean openCategory(View source, ElyraCategoryCardModel.CategoryCard card) {
        if (mTabSwitching || !source.isAttachedToWindow() || source.getWidth() <= 0
                || source.getHeight() <= 0 || !mState.requestOpen()) {
            return false;
        }
        AllAppsRecyclerView rv = mContainer.getActiveRecyclerView();
        if (rv == null || rv.getLayoutManager() == null) {
            mState.forceRoot();
            return false;
        }

        mCard = card;
        mSourceView = source;
        mRootScrollState = saveScrollState(rv);
        mSourceBounds = bounds(source);
        float[] sourceTitle = titlePosition(source, mSourceBounds);
        float sourceRadius = mContainer.getResources().getDimension(R.dimen.elyra_radius_large);
        mMorph = new MorphView(source, mSourceBounds, card.getLabel(), sourceTitle,
                mContainer.getContext().getColor(R.color.elyra_drawer_card_surface),
                mContainer.getContext().getColor(R.color.elyra_drawer_card_outline),
                sourceRadius);
        addMorph();

        source.setScaleX(0.975f);
        source.setScaleY(0.975f);
        mOpenTraceCookie = beginTrace("ElyraCategoryOpen");
        int generation = ++mTransitionGeneration;

        // Commit and measure the real destination before the first animation frame.
        beginSyncTrace("ElyraCategoryOpenPrepare");
        mContainer.selectElyraCategoryForMotion(card.getCategory());
        endSyncTrace();
        prepareRecyclerForTransition(rv, OPEN_CONTENT_SCALE);
        rv.scrollToPosition(0);
        rv.postOnAnimation(() -> rv.postOnAnimation(() -> prepareOpen(generation, rv)));
        return true;
    }

    boolean canHandleBack() {
        return mState.getState() != ElyraCategoryMotionStateMachine.State.ROOT;
    }

    boolean handleBack() {
        ElyraCategoryMotionStateMachine.State state = mState.getState();
        if (state == ElyraCategoryMotionStateMachine.State.CATEGORY_CLOSING) return true;
        if (!mState.requestClose()) return false;

        RectF startBounds = mMorph == null
                ? detailBounds(mContainer.getActiveRecyclerView())
                : mMorph.currentBounds();
        float startRadius = mMorph == null ? dp(14) : mMorph.currentRadius();
        float[] startTitle = mMorph == null
                ? detailTitlePosition(mContainer.getActiveRecyclerView(), startBounds)
                : mMorph.currentTitle();
        cancelAnimator();
        endTrace("ElyraCategoryOpen", mOpenTraceCookie);
        mOpenTraceCookie = 0;
        startClose(startBounds, startRadius, startTitle);
        return true;
    }

    void switchMode(boolean categories) {
        if (mTabSwitching || canHandleBack()
                || categories == mContainer.isElyraCategoryUiMode()) return;
        AllAppsRecyclerView rv = mContainer.getActiveRecyclerView();
        if (rv == null) return;
        mTabSwitching = true;
        if (categories) mAllScrollState = saveScrollState(rv);
        else mCategoriesScrollState = saveScrollState(rv);
        Runnable swap = () -> {
            if (categories) mContainer.showElyraCategoryCardsForMotion();
            else mContainer.showElyraAllAppsForMotion();
            Parcelable restore = categories ? mCategoriesScrollState : mAllScrollState;
            rv.postOnAnimation(() -> {
                restoreScrollState(rv, restore);
                if (ValueAnimator.areAnimatorsEnabled()) {
                    rv.setTranslationX(categories ? dp(8) : -dp(8));
                    rv.animate().alpha(1f).translationX(0f).setDuration(110)
                            .setInterpolator(EMPHASIZED)
                            .withEndAction(this::finishTabSwitch).start();
                } else {
                    rv.setAlpha(1f);
                    rv.setTranslationX(0f);
                    finishTabSwitch();
                }
                mContainer.updateElyraDrawerVisualState();
            });
        };
        if (!ValueAnimator.areAnimatorsEnabled()) {
            rv.setAlpha(0f);
            swap.run();
            return;
        }
        mTabTraceCookie = beginTrace("ElyraDrawerTabSwitch");
        rv.animate().cancel();
        rv.animate().alpha(0f).translationX(categories ? -dp(8) : dp(8)).setDuration(100)
                .setInterpolator(EMPHASIZED).withEndAction(swap).start();
    }

    /** Package/model changes settle transitions before replacing transition-owned state. */
    void onModelUpdated() {
        ElyraCategoryMotionStateMachine.State state = mState.getState();
        if (state == ElyraCategoryMotionStateMachine.State.CATEGORY_OPENING) {
            cancelAnimator();
            if (mContainer.getPersonalAppList().canHandleElyraCategoryBack()) {
                finishOpenImmediately();
            } else {
                settleRoot();
            }
        } else if (state == ElyraCategoryMotionStateMachine.State.CATEGORY_CLOSING
                || (state == ElyraCategoryMotionStateMachine.State.CATEGORY_DETAIL
                && !mContainer.getPersonalAppList().canHandleElyraCategoryBack())) {
            settleRoot();
        }
    }

    void dispose() {
        ElyraCategoryMotionStateMachine.State state = mState.getState();
        cancelAnimator();
        if (state == ElyraCategoryMotionStateMachine.State.CATEGORY_OPENING
                || state == ElyraCategoryMotionStateMachine.State.CATEGORY_CLOSING) {
            mContainer.showElyraCategoryCardsForMotion();
        }
        cleanupTransitionViews();
        mState.forceRoot();
        clearCategorySession();
        endTrace("ElyraCategoryOpen", mOpenTraceCookie);
        endTrace("ElyraCategoryClose", mCloseTraceCookie);
        endTrace("ElyraDrawerTabSwitch", mTabTraceCookie);
        mOpenTraceCookie = mCloseTraceCookie = mTabTraceCookie = 0;
        mTabSwitching = false;
    }

    private void prepareOpen(int generation, AllAppsRecyclerView rv) {
        if (generation != mTransitionGeneration
                || mState.getState()
                != ElyraCategoryMotionStateMachine.State.CATEGORY_OPENING
                || mMorph == null || !rv.isAttachedToWindow()) {
            return;
        }
        RectF destination = detailBounds(rv);
        if (destination.width() <= 0 || destination.height() <= 0) {
            finishOpenImmediately();
            return;
        }
        TextView title = findHeader(rv);
        mMorph.setTarget(destination, title == null
                        ? detailTitlePosition(rv, destination) : baseline(title),
                dp(14));
        if (!ValueAnimator.areAnimatorsEnabled()) {
            finishOpenImmediately();
            return;
        }
        rv.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mMorph.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mAnimator = createAnimator(OPEN_MS, generation, progress -> {
            if (mMorph == null) return;
            mMorph.setProgress(progress);
            rv.setAlpha(clamp((progress - 0.12f) / 0.88f));
            setScale(rv, OPEN_CONTENT_SCALE + (1f - OPEN_CONTENT_SCALE) * progress);
        }, () -> finishOpen(title));
        mAnimator.start();
    }

    private void startClose(RectF startBounds, float startRadius, float[] startTitle) {
        AllAppsRecyclerView rv = mContainer.getActiveRecyclerView();
        if (rv == null || mCard == null) {
            settleRoot();
            return;
        }
        mCloseTraceCookie = beginTrace("ElyraCategoryClose");
        removeMorph();
        mMorph = new MorphView(rv, startBounds, mCard.getLabel(), startTitle,
                mContainer.getContext().getColor(R.color.elyra_drawer_card_surface),
                mContainer.getContext().getColor(R.color.elyra_drawer_card_outline),
                startRadius);
        addMorph();

        beginSyncTrace("ElyraCategoryClosePrepare");
        mContainer.showElyraCategoryCardsForMotion();
        endSyncTrace();
        prepareRecyclerForTransition(rv, ROOT_CONTENT_SCALE);
        int generation = ++mTransitionGeneration;
        rv.postOnAnimation(() -> {
            restoreScrollState(rv, mRootScrollState);
            rv.postOnAnimation(() -> prepareClose(generation, rv, startBounds));
        });
    }

    private void prepareClose(int generation, AllAppsRecyclerView rv, RectF expanded) {
        if (generation != mTransitionGeneration
                || mState.getState()
                != ElyraCategoryMotionStateMachine.State.CATEGORY_CLOSING
                || mMorph == null || !rv.isAttachedToWindow()) {
            return;
        }
        View source = findCategoryCard(rv, mCard == null ? null : mCard.getCategory());
        RectF target = source == null ? fallbackSource(expanded) : bounds(source);
        mMorph.setTarget(target, titlePosition(source, target),
                mContainer.getResources().getDimension(R.dimen.elyra_radius_large));
        if (source != null) {
            source.setAlpha(0f);
            mClosingSource = source;
        }
        if (!ValueAnimator.areAnimatorsEnabled()) {
            finishClose(source);
            return;
        }
        rv.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mMorph.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mAnimator = createAnimator(CLOSE_MS, generation, progress -> {
            if (mMorph == null) return;
            mMorph.setProgress(progress);
            rv.setAlpha(clamp((progress - 0.10f) / 0.90f));
            setScale(rv, ROOT_CONTENT_SCALE + (1f - ROOT_CONTENT_SCALE) * progress);
        }, () -> finishClose(source));
        mAnimator.start();
    }

    private ValueAnimator createAnimator(long duration, int generation, ProgressConsumer update,
            Runnable end) {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(duration);
        animator.setInterpolator(EMPHASIZED);
        animator.addUpdateListener(value -> {
            if (generation == mTransitionGeneration) {
                update.accept((float) value.getAnimatedValue());
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled;
            @Override public void onAnimationCancel(Animator animation) { cancelled = true; }
            @Override public void onAnimationEnd(Animator animation) {
                if (!cancelled && generation == mTransitionGeneration) end.run();
            }
        });
        return animator;
    }

    private void finishOpen(@Nullable TextView title) {
        if (!mState.markOpened()) return;
        cleanupTransitionViews();
        if (title != null) {
            title.setFocusable(true);
            title.requestFocus();
            title.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        } else {
            AllAppsRecyclerView rv = mContainer.getActiveRecyclerView();
            if (rv != null && rv.getChildCount() > 0) rv.getChildAt(0).requestFocus();
        }
        endTrace("ElyraCategoryOpen", mOpenTraceCookie);
        mOpenTraceCookie = 0;
        mContainer.updateElyraDrawerVisualState();
    }

    private void finishOpenImmediately() {
        TextView title = findHeader(mContainer.getActiveRecyclerView());
        if (mState.getState() == ElyraCategoryMotionStateMachine.State.CATEGORY_OPENING) {
            finishOpen(title);
        } else {
            cleanupTransitionViews();
        }
    }

    private void finishClose(@Nullable View source) {
        if (!mState.markClosed()) {
            mState.forceRoot();
        }
        restoreSource(source);
        cleanupTransitionViews();
        if (source != null && source.isAttachedToWindow()) source.requestFocus();
        clearCategorySession();
        endTrace("ElyraCategoryClose", mCloseTraceCookie);
        mCloseTraceCookie = 0;
        mContainer.updateElyraDrawerVisualState();
    }

    private void settleRoot() {
        cancelAnimator();
        if (mContainer.getPersonalAppList().canHandleElyraCategoryBack()) {
            mContainer.showElyraCategoryCardsForMotion();
        }
        cleanupTransitionViews();
        mState.forceRoot();
        clearCategorySession();
        endTrace("ElyraCategoryOpen", mOpenTraceCookie);
        endTrace("ElyraCategoryClose", mCloseTraceCookie);
        mOpenTraceCookie = mCloseTraceCookie = 0;
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

    private RectF detailBounds(@Nullable AllAppsRecyclerView rv) {
        if (rv == null || rv.getWidth() <= 0 || rv.getHeight() <= 0) {
            return mSourceBounds == null ? new RectF() : new RectF(mSourceBounds);
        }
        RectF bounds = bounds(rv);
        TextView header = findHeader(rv);
        float top = header == null ? bounds.top + rv.getPaddingTop() : bounds(header).top;
        return new RectF(bounds.left + rv.getPaddingLeft() + dp(8), top,
                bounds.right - rv.getPaddingRight() - dp(8),
                bounds.bottom - rv.getPaddingBottom());
    }

    private float[] detailTitlePosition(@Nullable AllAppsRecyclerView rv, RectF fallback) {
        TextView title = findHeader(rv);
        return title == null
                ? new float[] {fallback.left + dp(16), fallback.top + dp(28)}
                : baseline(title);
    }

    private float[] titlePosition(@Nullable View source, RectF fallback) {
        if (source instanceof ViewGroup) {
            View title = ((ViewGroup) source).getChildAt(0);
            if (title instanceof TextView) return baseline((TextView) title);
        }
        return new float[] {fallback.left + dp(14), fallback.top + dp(24)};
    }

    private RectF bounds(View view) {
        mContainer.getLocationOnScreen(mContainerLocation);
        view.getLocationOnScreen(mViewLocation);
        float left = mViewLocation[0] - mContainerLocation[0];
        float top = mViewLocation[1] - mContainerLocation[1];
        return new RectF(left, top, left + view.getWidth(), top + view.getHeight());
    }

    private float[] baseline(TextView title) {
        RectF bounds = bounds(title);
        Paint.FontMetrics metrics = title.getPaint().getFontMetrics();
        return new float[] {bounds.left + title.getPaddingLeft(),
                bounds.top + (title.getHeight() - metrics.bottom - metrics.top) / 2f};
    }

    private RectF fallbackSource(RectF expanded) {
        float width = mSourceBounds == null ? dp(164) : mSourceBounds.width();
        float height = mSourceBounds == null ? dp(152) : mSourceBounds.height();
        float centerY = expanded.top + expanded.height() * 0.58f;
        return new RectF(expanded.centerX() - width / 2f, centerY - height / 2f,
                expanded.centerX() + width / 2f, centerY + height / 2f);
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

    private void prepareRecyclerForTransition(AllAppsRecyclerView rv, float scale) {
        rv.animate().cancel();
        rv.setAlpha(0f);
        setScale(rv, scale);
    }

    private void cleanupTransitionViews() {
        restoreSource(mClosingSource);
        restoreSource(mSourceView);
        mClosingSource = null;
        mSourceView = null;
        AllAppsRecyclerView rv = mContainer.getActiveRecyclerView();
        if (rv != null) {
            rv.animate().cancel();
            rv.setAlpha(1f);
            rv.setScaleX(1f);
            rv.setScaleY(1f);
            rv.setTranslationX(0f);
            rv.setLayerType(View.LAYER_TYPE_NONE, null);
        }
        removeMorph();
        mAnimator = null;
    }

    private void restoreSource(@Nullable View source) {
        if (source == null) return;
        source.animate().cancel();
        source.setAlpha(1f);
        source.setScaleX(1f);
        source.setScaleY(1f);
        source.setLayerType(View.LAYER_TYPE_NONE, null);
    }

    private void clearCategorySession() {
        mCard = null;
        mSourceBounds = null;
        mRootScrollState = null;
    }

    private void addMorph() {
        if (mMorph == null) return;
        mContainer.getOverlay().add(mMorph);
        mMorph.layout(0, 0, mContainer.getWidth(), mContainer.getHeight());
    }

    private void removeMorph() {
        if (mMorph == null) return;
        mMorph.setLayerType(View.LAYER_TYPE_NONE, null);
        mContainer.getOverlay().remove(mMorph);
        mMorph = null;
    }

    private void cancelAnimator() {
        mTransitionGeneration++;
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }
    }

    private void finishTabSwitch() {
        mTabSwitching = false;
        endTrace("ElyraDrawerTabSwitch", mTabTraceCookie);
        mTabTraceCookie = 0;
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

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private interface ProgressConsumer { void accept(float progress); }

    /** One preallocated shared surface; it never captures a card or drawer bitmap. */
    private static final class MorphView extends View {
        private final Paint surfacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final RectF start = new RectF();
        private final RectF end = new RectF();
        private final RectF current = new RectF();
        private final float[] startTitle = new float[2];
        private final float[] endTitle = new float[2];
        private final String title;
        private float startRadius;
        private float endRadius;
        private float progress;

        MorphView(View source, RectF bounds, String title, float[] titlePosition,
                int surfaceColor, int strokeColor, float radius) {
            super(source.getContext());
            start.set(bounds);
            end.set(bounds);
            this.title = title;
            startTitle[0] = endTitle[0] = titlePosition[0];
            startTitle[1] = endTitle[1] = titlePosition[1];
            startRadius = endRadius = radius;
            surfacePaint.setColor(surfaceColor);
            strokePaint.setColor(strokeColor);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(source.getResources().getDisplayMetrics().density);
            titlePaint.setColor(source.getContext().getColor(R.color.elyra_drawer_text_primary));
            titlePaint.setTextSize(14 * source.getResources().getDisplayMetrics().scaledDensity);
            titlePaint.setFakeBoldText(true);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        void setTarget(RectF bounds, float[] titlePosition, float radius) {
            end.set(bounds);
            endTitle[0] = titlePosition[0];
            endTitle[1] = titlePosition[1];
            endRadius = radius;
        }

        void setProgress(float value) {
            progress = value;
            invalidate();
        }

        RectF currentBounds() {
            RectF result = new RectF();
            lerp(start, end, progress, result);
            return result;
        }

        float[] currentTitle() {
            return new float[] {
                    lerp(startTitle[0], endTitle[0], progress),
                    lerp(startTitle[1], endTitle[1], progress),
            };
        }

        float currentRadius() {
            return lerp(startRadius, endRadius, progress);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            lerp(start, end, progress, current);
            float radius = lerp(startRadius, endRadius, progress);
            int alpha = Math.round(255f * (1f - clamp((progress - 0.78f) / 0.22f)));
            surfacePaint.setAlpha(alpha);
            strokePaint.setAlpha(alpha);
            titlePaint.setAlpha(alpha);
            canvas.drawRoundRect(current, radius, radius, surfacePaint);
            canvas.drawRoundRect(current, radius, radius, strokePaint);
            canvas.drawText(title,
                    lerp(startTitle[0], endTitle[0], progress),
                    lerp(startTitle[1], endTitle[1], progress), titlePaint);
        }

        private static void lerp(RectF from, RectF to, float amount, RectF out) {
            out.set(lerp(from.left, to.left, amount), lerp(from.top, to.top, amount),
                    lerp(from.right, to.right, amount), lerp(from.bottom, to.bottom, amount));
        }

        private static float lerp(float from, float to, float amount) {
            return from + (to - from) * amount;
        }
    }
}
