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
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.os.Trace;
import android.text.TextPaint;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.BuildConfig;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.model.data.AppInfo;
import com.elyra.launcher.drawer.ElyraAppCategory;
import com.elyra.launcher.drawer.ElyraCategoryCardModel;
import com.elyra.launcher.drawer.ElyraCategoryMotionStateMachine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Owns source-card category motion while the real AllApps RecyclerView remains authoritative. */
final class ElyraCategoryMotionController {
    private static final long OPEN_MS = 320;
    private static final long CLOSE_MS = 280;
    private static final float CONTENT_SCALE = 0.98f;
    private static final int SHARED_ICON_LIMIT = 4;

    private final ActivityAllAppsContainerView<?> mContainer;
    private final ElyraCategoryMotionStateMachine mState =
            new ElyraCategoryMotionStateMachine();
    private final int[] mContainerLocation = new int[2];
    private final int[] mViewLocation = new int[2];
    private ValueAnimator mAnimator;
    private MorphView mMorph;
    private ElyraCategoryCardModel.CategoryCard mCard;
    private Parcelable mRootScrollState;
    private Parcelable mAllScrollState;
    private Parcelable mCategoriesScrollState;
    private RectF mSourceBounds;
    private List<MorphIcon> mSourceIcons = Collections.emptyList();
    private View mClosingSource;
    private int mGeneration;
    private int mNextTraceCookie;
    private int mOpenTraceCookie;
    private int mCloseTraceCookie;
    private int mTabTraceCookie;
    private boolean mTabSwitching;

    ElyraCategoryMotionController(ActivityAllAppsContainerView<?> container) {
        mContainer = container;
    }

    boolean openCategory(View source, ElyraCategoryCardModel.CategoryCard card) {
        if (mTabSwitching || !(source instanceof LinearLayout)) return false;
        if (!mState.requestOpen()) return false;
        AllAppsRecyclerView rv = mContainer.getActiveRecyclerView();
        if (rv == null) {
            mState.forceRoot();
            return false;
        }
        mCard = card;
        mRootScrollState = saveScrollState(rv);
        mSourceBounds = bounds(source);
        mSourceIcons = captureCardIcons((LinearLayout) source, card);
        TextView sourceTitle = (TextView) ((ViewGroup) source).getChildAt(0);
        float radius = mContainer.getResources().getDimension(R.dimen.elyra_radius_large);
        mMorph = new MorphView(
                source, mSourceBounds, card.getLabel(), baseline(sourceTitle), mSourceIcons,
                mContainer.getContext().getColor(R.color.elyra_drawer_card_surface),
                mContainer.getContext().getColor(R.color.elyra_drawer_card_outline), radius);
        addMorph();
        source.animate().cancel();
        source.animate().scaleX(0.97f).scaleY(0.97f).setDuration(64).withEndAction(() ->
                source.animate().scaleX(1f).scaleY(1f).setDuration(72).start()).start();
        mOpenTraceCookie = beginTrace("ElyraCategoryOpen");
        mContainer.selectElyraCategoryForMotion(card.getCategory());
        hideContent(rv);
        rv.scrollToPosition(0);
        rv.post(() -> rv.post(this::animateOpen));
        return true;
    }

    boolean canHandleBack() {
        return mState.getState() != ElyraCategoryMotionStateMachine.State.CATEGORIES_ROOT;
    }

    boolean handleBack() {
        if (mState.getState() == ElyraCategoryMotionStateMachine.State.CATEGORY_CLOSING) return true;
        boolean wasOpening = mState.getState()
                == ElyraCategoryMotionStateMachine.State.CATEGORY_OPENING;
        if (!mState.requestClose()) return false;
        cancelAnimator();
        if (wasOpening) {
            endTrace("ElyraCategoryOpen", mOpenTraceCookie);
            mOpenTraceCookie = 0;
        }
        startClose();
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
            rv.post(() -> {
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
            swap.run();
            return;
        }
        mTabTraceCookie = beginTrace("ElyraDrawerTabSwitch");
        rv.animate().cancel();
        rv.animate().alpha(0f).translationX(categories ? -dp(8) : dp(8)).setDuration(100)
                .setInterpolator(EMPHASIZED).withEndAction(() -> {
                    swap.run();
                }).start();
    }

    void onModelUpdated() {
        if (mState.getState() != ElyraCategoryMotionStateMachine.State.CATEGORIES_ROOT
                && !mContainer.getPersonalAppList().canHandleElyraCategoryBack()) {
            cancelAnimator();
            restoreClosingSource();
            removeMorph();
            resetContent();
            mState.forceRoot();
            mContainer.updateElyraDrawerVisualState();
        }
    }

    void dispose() {
        cancelAnimator();
        restoreClosingSource();
        removeMorph();
        resetContent();
        mState.forceRoot();
        endTrace("ElyraCategoryOpen", mOpenTraceCookie);
        endTrace("ElyraCategoryClose", mCloseTraceCookie);
        endTrace("ElyraDrawerTabSwitch", mTabTraceCookie);
        mOpenTraceCookie = mCloseTraceCookie = mTabTraceCookie = 0;
        mTabSwitching = false;
    }

    private void animateOpen() {
        if (mState.getState() != ElyraCategoryMotionStateMachine.State.CATEGORY_OPENING) return;
        AllAppsRecyclerView rv = mContainer.getActiveRecyclerView();
        if (rv == null || mMorph == null) {
            finishOpen(Collections.emptyList(), null);
            return;
        }
        RectF detail = detailBounds(rv);
        TextView title = findHeader(rv);
        List<BubbleTextView> icons = visibleIcons(rv);
        Map<String, RectF> destinations = iconBounds(icons);
        List<MorphIcon> shared = new ArrayList<>();
        for (MorphIcon icon : mSourceIcons) {
            RectF target = destinations.get(icon.key);
            shared.add(icon.withEnd(target == null ? fallbackIcon(detail) : target));
        }
        mMorph.setTarget(detail,
                title == null ? new float[] {detail.left + dp(16), detail.top + dp(28)}
                        : baseline(title),
                shared,
                dp(14));
        prepareChildren(icons, title);
        if (!ValueAnimator.areAnimatorsEnabled()) {
            finishOpen(icons, title);
            return;
        }
        int generation = ++mGeneration;
        mAnimator = animator(OPEN_MS, progress -> {
            if (generation != mGeneration || mMorph == null) return;
            mMorph.setProgress(progress);
            rv.setAlpha(clamp((progress - 0.18f) / 0.82f));
            setContentScale(rv, CONTENT_SCALE + (1f - CONTENT_SCALE) * progress);
            updateChildren(icons, title, progress);
        }, () -> finishOpen(icons, title));
        mAnimator.start();
    }

    private void startClose() {
        AllAppsRecyclerView rv = mContainer.getActiveRecyclerView();
        if (rv == null || mCard == null) {
            finishClose(null);
            return;
        }
        mCloseTraceCookie = beginTrace("ElyraCategoryClose");
        RectF expanded = mMorph == null ? detailBounds(rv) : mMorph.currentBounds();
        TextView title = findHeader(rv);
        List<MorphIcon> starts = mMorph == null
                ? captureDetailIcons(visibleIcons(rv), mCard) : mMorph.currentIcons();
        float[] titleStart = mMorph == null
                ? (title == null ? new float[] {expanded.left + dp(16), expanded.top + dp(28)}
                        : baseline(title))
                : mMorph.currentTitle();
        float radiusStart = mMorph == null ? dp(14) : mMorph.currentRadius();
        removeMorph();
        mMorph = new MorphView(
                rv, expanded, mCard.getLabel(), titleStart,
                starts,
                mContainer.getContext().getColor(R.color.elyra_drawer_card_surface),
                mContainer.getContext().getColor(R.color.elyra_drawer_card_outline), radiusStart);
        addMorph();
        mContainer.showElyraCategoryCardsForMotion();
        hideContent(rv);
        rv.post(() -> {
            restoreScrollState(rv, mRootScrollState);
            rv.post(() -> animateClose(rv, expanded));
        });
    }

    private void animateClose(AllAppsRecyclerView rv, RectF expanded) {
        if (mState.getState() != ElyraCategoryMotionStateMachine.State.CATEGORY_CLOSING
                || mMorph == null) return;
        View source = findCategoryCard(rv, mCard == null ? null : mCard.getCategory());
        RectF target = source == null ? fallbackSource(expanded) : bounds(source);
        List<MorphIcon> endIcons = source instanceof LinearLayout
                ? captureCardIcons((LinearLayout) source, mCard)
                : fallbackIcons(target, mMorph.icons());
        TextView title = source instanceof ViewGroup
                ? (TextView) ((ViewGroup) source).getChildAt(0) : null;
        mMorph.setTarget(target,
                title == null ? new float[] {target.left + dp(14), target.top + dp(24)}
                        : baseline(title),
                mapTargets(mMorph.icons(), endIcons, target),
                mContainer.getResources().getDimension(R.dimen.elyra_radius_large));
        if (source != null) source.setAlpha(0f);
        mClosingSource = source;
        if (!ValueAnimator.areAnimatorsEnabled()) {
            finishClose(source);
            return;
        }
        int generation = ++mGeneration;
        mAnimator = animator(CLOSE_MS, progress -> {
            if (generation != mGeneration || mMorph == null) return;
            mMorph.setProgress(progress);
            rv.setAlpha(clamp((progress - 0.12f) / 0.88f));
            setContentScale(rv, CONTENT_SCALE + (1f - CONTENT_SCALE) * progress);
        }, () -> finishClose(source));
        mAnimator.start();
    }

    private ValueAnimator animator(long duration, ProgressConsumer update, Runnable end) {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(duration);
        animator.setInterpolator(EMPHASIZED);
        animator.addUpdateListener(value -> update.accept((float) value.getAnimatedValue()));
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled;
            @Override public void onAnimationCancel(Animator animation) { cancelled = true; }
            @Override public void onAnimationEnd(Animator animation) {
                if (!cancelled) end.run();
            }
        });
        return animator;
    }

    private void finishOpen(List<BubbleTextView> icons, @Nullable TextView title) {
        resetChildren(icons, title);
        resetContent();
        removeMorph();
        mState.markOpened();
        if (title != null) {
            title.setFocusable(true);
            title.requestFocus();
            title.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        }
        else if (!icons.isEmpty()) icons.get(0).requestFocus();
        endTrace("ElyraCategoryOpen", mOpenTraceCookie);
        mOpenTraceCookie = 0;
        mContainer.updateElyraDrawerVisualState();
    }

    private void finishClose(@Nullable View source) {
        if (mState.getState() != ElyraCategoryMotionStateMachine.State.CATEGORY_CLOSING) {
            mContainer.showElyraCategoryCardsForMotion();
            mState.forceRoot();
        } else {
            mState.markClosed();
        }
        if (source != null) {
            source.setAlpha(1f);
            source.requestFocus();
        }
        mClosingSource = null;
        resetContent();
        removeMorph();
        endTrace("ElyraCategoryClose", mCloseTraceCookie);
        mCloseTraceCookie = 0;
        mContainer.updateElyraDrawerVisualState();
    }

    private void prepareChildren(List<BubbleTextView> icons, @Nullable TextView title) {
        for (BubbleTextView icon : icons) {
            icon.setAlpha(0f);
            icon.setScaleX(0.92f);
            icon.setScaleY(0.92f);
        }
        if (title != null) {
            title.setAlpha(0f);
            title.setTranslationY(dp(12));
        }
    }

    private void updateChildren(List<BubbleTextView> icons, @Nullable TextView title, float p) {
        for (int i = 0; i < icons.size(); i++) {
            float stagger = Math.min(i, 10) * (8f / OPEN_MS);
            float child = clamp((p - 0.32f - stagger) / 0.5f);
            BubbleTextView icon = icons.get(i);
            icon.setAlpha(child);
            icon.setScaleX(0.92f + 0.08f * child);
            icon.setScaleY(0.92f + 0.08f * child);
        }
        if (title != null) {
            float child = clamp((p - 0.2f) / 0.55f);
            title.setAlpha(child);
            title.setTranslationY(dp(12) * (1f - child));
        }
    }

    private void resetChildren(List<BubbleTextView> icons, @Nullable TextView title) {
        for (BubbleTextView icon : icons) {
            icon.setAlpha(1f);
            icon.setScaleX(1f);
            icon.setScaleY(1f);
        }
        if (title != null) {
            title.setAlpha(1f);
            title.setTranslationY(0f);
        }
    }

    private List<MorphIcon> captureCardIcons(
            LinearLayout card, ElyraCategoryCardModel.CategoryCard model) {
        List<MorphIcon> result = new ArrayList<>();
        if (!(card.getChildAt(2) instanceof GridLayout)) return result;
        GridLayout grid = (GridLayout) card.getChildAt(2);
        int count = Math.min(Math.min(grid.getChildCount(), model.getPreview().size()),
                SHARED_ICON_LIMIT);
        for (int i = 0; i < count; i++) {
            if (!(grid.getChildAt(i) instanceof ImageView)) continue;
            ImageView icon = (ImageView) grid.getChildAt(i);
            if (icon.getDrawable() == null) continue;
            RectF source = bounds(icon);
            result.add(new MorphIcon(
                    model.getPreview().get(i).toComponentKey().toString(), icon.getDrawable(),
                    source, source));
        }
        return result;
    }

    private List<MorphIcon> captureDetailIcons(
            List<BubbleTextView> icons, ElyraCategoryCardModel.CategoryCard model) {
        Map<String, BubbleTextView> byKey = new HashMap<>();
        for (BubbleTextView icon : icons) {
            AppInfo app = (AppInfo) icon.getTag();
            byKey.put(app.toComponentKey().toString(), icon);
        }
        List<MorphIcon> result = new ArrayList<>();
        for (AppInfo app : model.getPreview()) {
            if (result.size() == SHARED_ICON_LIMIT) break;
            String key = app.toComponentKey().toString();
            BubbleTextView icon = byKey.get(key);
            if (icon == null || icon.getIcon() == null) continue;
            RectF source = bounds(icon);
            result.add(new MorphIcon(key, icon.getIcon(), source, source));
        }
        return result;
    }

    private Map<String, RectF> iconBounds(List<BubbleTextView> icons) {
        Map<String, RectF> result = new HashMap<>();
        for (BubbleTextView icon : icons) {
            AppInfo app = (AppInfo) icon.getTag();
            result.put(app.toComponentKey().toString(), bounds(icon));
        }
        return result;
    }

    private List<BubbleTextView> visibleIcons(ViewGroup parent) {
        List<BubbleTextView> result = new ArrayList<>();
        collectIcons(parent, result);
        return result;
    }

    private void collectIcons(ViewGroup parent, List<BubbleTextView> output) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof BubbleTextView && child.getTag() instanceof AppInfo) {
                output.add((BubbleTextView) child);
            } else if (child instanceof ViewGroup) {
                collectIcons((ViewGroup) child, output);
            }
        }
    }

    @Nullable
    private TextView findHeader(RecyclerView rv) {
        for (int i = 0; i < rv.getChildCount(); i++) {
            View child = rv.getChildAt(i);
            if (rv.getChildViewHolder(child).getItemViewType()
                    == BaseAllAppsAdapter.VIEW_TYPE_ELYRA_CATEGORY_HEADER
                    && child instanceof TextView) return (TextView) child;
        }
        return null;
    }

    @Nullable
    private View findCategoryCard(RecyclerView rv, @Nullable ElyraAppCategory category) {
        if (category == null) return null;
        for (int i = 0; i < rv.getChildCount(); i++) {
            View child = rv.getChildAt(i);
            if (child.getTag() == category) return child;
        }
        return null;
    }

    private RectF detailBounds(AllAppsRecyclerView rv) {
        RectF bounds = bounds(rv);
        TextView header = findHeader(rv);
        float top = header == null ? bounds.top + rv.getPaddingTop() : bounds(header).top;
        return new RectF(bounds.left + rv.getPaddingLeft() + dp(8), top,
                bounds.right - rv.getPaddingRight() - dp(8),
                bounds.bottom - rv.getPaddingBottom());
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

    private RectF fallbackIcon(RectF surface) {
        float size = dp(32);
        return new RectF(surface.centerX() - size / 2f, surface.top + dp(72),
                surface.centerX() + size / 2f, surface.top + dp(72) + size);
    }

    private List<MorphIcon> fallbackIcons(RectF target, List<MorphIcon> starts) {
        List<MorphIcon> result = new ArrayList<>();
        for (int i = 0; i < starts.size(); i++) {
            float left = target.centerX() - dp(38) + (i % 2) * dp(38);
            float top = target.top + dp(70) + (i / 2) * dp(38);
            result.add(starts.get(i).withEnd(new RectF(left, top, left + dp(32), top + dp(32))));
        }
        return result;
    }

    private List<MorphIcon> mapTargets(
            List<MorphIcon> starts, List<MorphIcon> targets, RectF fallback) {
        Map<String, RectF> byKey = new HashMap<>();
        for (MorphIcon target : targets) byKey.put(target.key, target.start);
        List<MorphIcon> result = new ArrayList<>();
        for (MorphIcon start : starts) {
            RectF target = byKey.get(start.key);
            result.add(start.withEnd(target == null ? fallbackIcon(fallback) : target));
        }
        return result;
    }

    private Parcelable saveScrollState(RecyclerView rv) {
        return rv.getLayoutManager() == null ? null : rv.getLayoutManager().onSaveInstanceState();
    }

    private void restoreScrollState(RecyclerView rv, @Nullable Parcelable state) {
        if (state != null && rv.getLayoutManager() != null) {
            rv.getLayoutManager().onRestoreInstanceState(state);
        }
    }

    private void hideContent(AllAppsRecyclerView rv) {
        rv.animate().cancel();
        rv.setAlpha(0f);
        setContentScale(rv, CONTENT_SCALE);
    }

    private void setContentScale(View view, float scale) {
        view.setScaleX(scale);
        view.setScaleY(scale);
    }

    private void resetContent() {
        AllAppsRecyclerView rv = mContainer.getActiveRecyclerView();
        if (rv != null) {
            rv.animate().cancel();
            rv.setAlpha(1f);
            rv.setScaleX(1f);
            rv.setScaleY(1f);
            rv.setTranslationX(0f);
            rv.setLayerType(View.LAYER_TYPE_NONE, null);
        }
    }

    private void addMorph() {
        if (mMorph == null) return;
        mContainer.getOverlay().add(mMorph);
        mMorph.layout(0, 0, mContainer.getWidth(), mContainer.getHeight());
    }

    private void removeMorph() {
        if (mMorph != null) {
            mContainer.getOverlay().remove(mMorph);
            mMorph = null;
        }
    }

    private void cancelAnimator() {
        mGeneration++;
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }
    }

    private void restoreClosingSource() {
        if (mClosingSource != null) {
            mClosingSource.setAlpha(1f);
            mClosingSource = null;
        }
    }

    private void finishTabSwitch() {
        mTabSwitching = false;
        endTrace("ElyraDrawerTabSwitch", mTabTraceCookie);
        mTabTraceCookie = 0;
    }

    private int beginTrace(String name) {
        int cookie = ++mNextTraceCookie;
        if (BuildConfig.DEBUG && Utilities.ATLEAST_Q) {
            Trace.beginAsyncSection(name, cookie);
        }
        return cookie;
    }

    private void endTrace(String name, int cookie) {
        if (cookie != 0 && BuildConfig.DEBUG && Utilities.ATLEAST_Q) {
            Trace.endAsyncSection(name, cookie);
        }
    }

    private int dp(int value) {
        return Math.round(value * mContainer.getResources().getDisplayMetrics().density);
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private interface ProgressConsumer { void accept(float progress); }

    private static final class MorphIcon {
        final String key;
        final Drawable drawable;
        final RectF start;
        final RectF end;

        MorphIcon(String key, Drawable drawable, RectF start, RectF end) {
            this.key = key;
            this.drawable = drawable;
            this.start = new RectF(start);
            this.end = new RectF(end);
        }

        MorphIcon withEnd(RectF target) {
            return new MorphIcon(key, drawable, start, target);
        }
    }

    /** Temporary drawable overlay; it never captures a card or drawer bitmap. */
    private static final class MorphView extends View {
        private final Paint surfacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final RectF current = new RectF();
        private final RectF iconBounds = new RectF();
        private RectF start;
        private RectF end;
        private float[] startTitle;
        private float[] endTitle;
        private List<MorphIcon> icons;
        private float startRadius;
        private float endRadius;
        private float progress;
        private final String title;

        MorphView(
                View source, RectF bounds, String title, float[] titlePosition,
                List<MorphIcon> icons, int surfaceColor, int strokeColor, float radius) {
            super(source.getContext());
            start = new RectF(bounds);
            end = new RectF(bounds);
            this.title = title;
            startTitle = titlePosition.clone();
            endTitle = titlePosition.clone();
            this.icons = new ArrayList<>(icons);
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

        void setTarget(RectF bounds, float[] titlePosition, List<MorphIcon> icons, float radius) {
            end = new RectF(bounds);
            endTitle = titlePosition.clone();
            this.icons = new ArrayList<>(icons);
            endRadius = radius;
        }

        void setProgress(float progress) {
            this.progress = progress;
            invalidate();
        }

        List<MorphIcon> icons() { return new ArrayList<>(icons); }

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

        float currentRadius() { return lerp(startRadius, endRadius, progress); }

        List<MorphIcon> currentIcons() {
            List<MorphIcon> result = new ArrayList<>();
            RectF currentBounds = new RectF();
            for (MorphIcon icon : icons) {
                lerp(icon.start, icon.end, progress, currentBounds);
                result.add(new MorphIcon(icon.key, icon.drawable, currentBounds, currentBounds));
            }
            return result;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            lerp(start, end, progress, current);
            float radius = lerp(startRadius, endRadius, progress);
            canvas.drawRoundRect(current, radius, radius, surfacePaint);
            canvas.drawRoundRect(current, radius, radius, strokePaint);
            titlePaint.setAlpha(Math.round(255 * (1f - 0.3f * progress)));
            canvas.drawText(title,
                    lerp(startTitle[0], endTitle[0], progress),
                    lerp(startTitle[1], endTitle[1], progress), titlePaint);
            for (MorphIcon icon : icons) {
                lerp(icon.start, icon.end, progress, iconBounds);
                icon.drawable.setBounds(Math.round(iconBounds.left), Math.round(iconBounds.top),
                        Math.round(iconBounds.right), Math.round(iconBounds.bottom));
                icon.drawable.draw(canvas);
            }
        }

        private static void lerp(RectF start, RectF end, float progress, RectF out) {
            out.set(lerp(start.left, end.left, progress), lerp(start.top, end.top, progress),
                    lerp(start.right, end.right, progress), lerp(start.bottom, end.bottom, progress));
        }

        private static float lerp(float start, float end, float progress) {
            return start + (end - start) * progress;
        }
    }
}
