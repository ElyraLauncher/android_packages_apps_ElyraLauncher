/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.views;

import static android.view.HapticFeedbackConstants.CLOCK_TICK;

import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Property;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowInsets;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.FastScrollRecyclerView;
import com.android.launcher3.allapps.AllAppsRecyclerView;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.graphics.FastScrollThumbDrawable;
import com.android.launcher3.util.Themes;
import com.elyra.launcher.drawer.ElyraDrawerLayoutPolicy;

import java.util.Collections;
import java.util.List;

/**
 * The track and scrollbar that shows when you scroll the list.
 */
public class RecyclerViewFastScroller extends View {
    private static final String TAG = "RecyclerViewFastScroller";
    private static final boolean DEBUG = false;
    private static final int FASTSCROLL_THRESHOLD_MILLIS = 10;
    private static final int SCROLL_DELTA_THRESHOLD_DP = 4;

    // Track is very narrow to target and correctly. This is especially the case if a user is
    // using a hardware case. Even if x is offset by following amount, we consider it to be valid.
    private static final int SCROLLBAR_LEFT_OFFSET_TOUCH_DELEGATE_DP = 5;
    private static final Rect sTempRect = new Rect();

    private static final Property<RecyclerViewFastScroller, Integer> TRACK_WIDTH =
            new Property<RecyclerViewFastScroller, Integer>(Integer.class, "width") {

                @Override
                public Integer get(RecyclerViewFastScroller scrollBar) {
                    return scrollBar.mWidth;
                }

                @Override
                public void set(RecyclerViewFastScroller scrollBar, Integer value) {
                    scrollBar.setTrackWidth(value);
                }
            };

    private final static int MAX_TRACK_ALPHA = 30;
    private final static int SCROLL_BAR_VIS_DURATION = 150;

    private static final List<Rect> SYSTEM_GESTURE_EXCLUSION_RECT =
            Collections.singletonList(new Rect());

    private final int mMinWidth;
    private final int mMaxWidth;
    private final int mThumbPadding;

    /** Keeps the last known scrolling delta/velocity along y-axis. */
    private int mDy = 0;
    private final float mDeltaThreshold;
    private final float mScrollbarLeftOffsetTouchDelegate;

    private final ViewConfiguration mConfig;

    // Current width of the track
    private int mWidth;
    private ObjectAnimator mWidthAnimator;

    private final Paint mThumbPaint;
    protected final int mThumbHeight;
    private final RectF mThumbBounds = new RectF();
    private final Point mThumbDrawOffset = new Point();

    private final Paint mTrackPaint;
    private final Paint mIndexPaint;

    private float mLastTouchY;
    private boolean mIsDragging;
    /**
     * Tracks whether a keyboard hide request has been sent due to downward scrolling.
     * <p>
     * Set to true when scrolling down and reset when scrolling up to prevents redundant hide
     * requests during continuous downward scrolls.
     */
    private boolean mRequestedHideKeyboard;
    private boolean mIsThumbDetached;
    private final boolean mCanThumbDetach;
    private boolean mIgnoreDragGesture;
    private long mDownTimeStampMillis;

    // This is the offset from the top of the scrollbar when the user first starts touching.  To
    // prevent jumping, this offset is applied as the user scrolls.
    protected int mTouchOffsetY;
    protected int mThumbOffsetY;

    // Fast scroller popup
    private TextView mPopupView;
    private boolean mPopupVisible;
    private boolean mCompactPopup;
    private CharSequence mPopupSectionName;
    private Insets mSystemGestureInsets;

    protected FastScrollRecyclerView mRv;
    private RecyclerView.OnScrollListener mOnScrollListener;
    private final ActivityContext mActivityContext;

    private int mDownX;
    private int mDownY;
    private int mLastY;

    public RecyclerViewFastScroller(Context context) {
        this(context, null);
    }

    public RecyclerViewFastScroller(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecyclerViewFastScroller(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mTrackPaint = new Paint();
        mTrackPaint.setColor(context.getColor(R.color.elyra_drawer_text_primary));
        mTrackPaint.setAlpha(MAX_TRACK_ALPHA);

        mIndexPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mIndexPaint.setColor(context.getColor(R.color.elyra_drawer_text_unselected));
        mIndexPaint.setAlpha(Utilities.isDarkTheme(context) ? 112 : 170);
        mIndexPaint.setTextAlign(Paint.Align.CENTER);
        mIndexPaint.setTextSize(getResources().getDimension(
                R.dimen.elyra_fastscroll_index_text_size));

        mThumbPaint = new Paint();
        mThumbPaint.setAntiAlias(true);
        mThumbPaint.setColor(Themes.getColorAccent(context));
        mThumbPaint.setStyle(Paint.Style.FILL);

        Resources res = getResources();
        mWidth = mMinWidth = res.getDimensionPixelSize(R.dimen.fastscroll_track_min_width);
        mMaxWidth = res.getDimensionPixelSize(R.dimen.fastscroll_track_max_width);

        mThumbPadding = res.getDimensionPixelSize(R.dimen.fastscroll_thumb_padding);
        mThumbHeight = res.getDimensionPixelSize(R.dimen.fastscroll_thumb_height);

        mConfig = ViewConfiguration.get(context);
        mDeltaThreshold = res.getDisplayMetrics().density * SCROLL_DELTA_THRESHOLD_DP;
        mScrollbarLeftOffsetTouchDelegate = res.getDisplayMetrics().density
                * SCROLLBAR_LEFT_OFFSET_TOUCH_DELEGATE_DP;
        mActivityContext = ActivityContext.lookupContext(context);
        TypedArray ta =
                context.obtainStyledAttributes(attrs, R.styleable.RecyclerViewFastScroller, defStyleAttr, 0);
        mCanThumbDetach = ta.getBoolean(R.styleable.RecyclerViewFastScroller_canThumbDetach, false);
        ta.recycle();
    }

    /** Sets the popup view to show while the scroller is being dragged. */
    public void setPopupView(TextView popupView) {
        setPopupView(popupView, false);
    }

    /** Uses a compact rounded popup for the app drawer while preserving other scrollers. */
    public void setPopupView(TextView popupView, boolean compact) {
        mPopupView = popupView;
        mCompactPopup = compact;
        if (compact) {
            GradientDrawable background = new GradientDrawable();
            background.setShape(GradientDrawable.RECTANGLE);
            background.setColor(
                    getContext().getColor(R.color.elyra_drawer_index_popup_surface));
            background.setCornerRadius(
                    getResources().getDimensionPixelSize(R.dimen.elyra_fastscroll_popup_size) / 2f);
            background.setStroke(
                    getResources().getDimensionPixelSize(R.dimen.search_decoration_padding),
                    getContext().getColor(R.color.elyra_drawer_outline_level_2));
            mPopupView.setBackground(background);
            mPopupView.setTextColor(
                    getContext().getColor(R.color.elyra_drawer_text_primary));
            mPopupView.setPadding(0, 0, 0, 0);
        } else {
            mPopupView.setBackground(
                    new FastScrollThumbDrawable(mThumbPaint, Utilities.isRtl(getResources())));
        }
    }

    public void setRecyclerView(FastScrollRecyclerView rv) {
        if (mRv != null && mOnScrollListener != null) {
            mRv.removeOnScrollListener(mOnScrollListener);
        }
        mRv = rv;

        mRv.addOnScrollListener(mOnScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                mDy = dy;

                // TODO(winsonc): If we want to animate the section heads while scrolling, we can
                //                initiate that here if the recycler view scroll state is not
                //                RecyclerView.SCROLL_STATE_IDLE.

                mRv.onUpdateScrollbar(dy);
            }
        });
    }

    public void reattachThumbToScroll() {
        mIsThumbDetached = false;
    }

    public void setThumbOffsetY(int y) {
        if (mThumbOffsetY == y) {
            return;
        }
        updatePopupY(y);
        mThumbOffsetY = y;
        invalidate();
    }

    public int getThumbOffsetY() {
        return mThumbOffsetY;
    }

    private void setTrackWidth(int width) {
        if (mWidth == width) {
            return;
        }
        mWidth = width;
        invalidate();
    }

    public int getThumbHeight() {
        return mThumbHeight;
    }

    public boolean isDraggingThumb() {
        return mIsDragging;
    }

    public boolean isThumbDetached() {
        return mIsThumbDetached;
    }

    /**
     * Handles the touch event and determines whether to show the fast scroller (or updates it if
     * it is already showing).
     */
    public boolean handleTouchEvent(MotionEvent ev, Point offset) {
        int x = (int) ev.getX() - offset.x;
        int y = (int) ev.getY() - offset.y;

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Keep track of the down positions
                mDownX = x;
                mDownY = mLastY = y;
                mDownTimeStampMillis = ev.getDownTime();
                mRequestedHideKeyboard = false;

                if (isNearScrollBar(x) && mRv.supportsFastScrolling()) {
                    mTouchOffsetY = isNearThumb(x, y)
                            ? mDownY - mThumbOffsetY
                            : mThumbHeight / 2;
                    calcTouchOffsetAndPrepToFastScroll(mDownY, mDownY);
                    updateFastScrollSectionNameAndThumbOffset(y);
                    break;
                }

                if ((Math.abs(mDy) < mDeltaThreshold &&
                        mRv.getScrollState() != SCROLL_STATE_IDLE)) {
                    // now the touch events are being passed to the {@link WidgetCell} until the
                    // touch sequence goes over the touch slop.
                    mRv.stopScroll();
                }
                if (isNearThumb(x, y)) {
                    mTouchOffsetY = mDownY - mThumbOffsetY;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                boolean isScrollingDown = y > mLastY;
                mLastY = y;
                int absDeltaY = Math.abs(y - mDownY);
                int absDeltaX = Math.abs(x - mDownX);

                // Check if we should start scrolling, but ignore this fastscroll gesture if we have
                // exceeded some fixed movement
                mIgnoreDragGesture |= absDeltaY > mConfig.getScaledPagingTouchSlop();

                if (!mIsDragging && !mIgnoreDragGesture && mRv.supportsFastScrolling()) {
                    if ((isNearThumb(mDownX, mLastY) && ev.getEventTime() - mDownTimeStampMillis
                                    > FASTSCROLL_THRESHOLD_MILLIS)) {
                        calcTouchOffsetAndPrepToFastScroll(mDownY, mLastY);
                    }
                }
                if (mIsDragging) {
                    if (isScrollingDown) {
                        if (!mRequestedHideKeyboard) {
                            mActivityContext.hideKeyboard();
                        }
                        mRequestedHideKeyboard = true;
                    } else {
                        mRequestedHideKeyboard = false;
                    }
                    updateFastScrollSectionNameAndThumbOffset(y);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                endFastScrolling();
                break;
        }
        if (DEBUG) {
            Log.d(TAG, (ev.getAction() == MotionEvent.ACTION_DOWN ? "\n" : "")
                    + "handleTouchEvent " + MotionEvent.actionToString(ev.getAction())
                    + " (" + x + "," + y + ")" + " isDragging=" + mIsDragging
                    + " mIgnoreDragGesture=" + mIgnoreDragGesture);

        }
        return mIsDragging;
    }

    private void calcTouchOffsetAndPrepToFastScroll(int downY, int lastY) {
        mIsDragging = true;
        if (mCanThumbDetach) {
            mIsThumbDetached = true;
        }
        mTouchOffsetY += (lastY - downY);
        animatePopupVisibility(true);
        showActiveScrollbar(true);
    }

    private void updateFastScrollSectionNameAndThumbOffset(int y) {
        // Update the fastscroller section name at this touch position
        int bottom = mRv.getScrollbarTrackHeight() - mThumbHeight;
        float boundedY = (float) Math.max(0, Math.min(bottom, y - mTouchOffsetY));
        float progress = bottom == 0 ? 0f : boundedY / bottom;
        CharSequence sectionName;
        if (mCompactPopup && mRv instanceof AllAppsRecyclerView) {
            int railY = y - mRv.getScrollBarTop();
            int index = ElyraDrawerLayoutPolicy.railIndex(
                    railY, mRv.getScrollbarTrackHeight());
            String target = String.valueOf(ElyraDrawerLayoutPolicy.INDEX_LABELS.charAt(index));
            ((AllAppsRecyclerView) mRv).scrollToSectionName(target);
            sectionName = target;
        } else {
            sectionName = mRv.scrollToPositionAtProgress(progress);
        }
        if (!sectionName.equals(mPopupSectionName)) {
            mPopupSectionName = sectionName;
            mPopupView.setText(sectionName);
            performHapticFeedback(CLOCK_TICK);
        }
        animatePopupVisibility(!TextUtils.isEmpty(sectionName));
        mLastTouchY = boundedY;
        setThumbOffsetY((int) mLastTouchY);
        if (mCompactPopup) {
            int touchY = Utilities.boundToRange(
                    y - mRv.getScrollBarTop(), 0, mRv.getScrollbarTrackHeight());
            updatePopupY(touchY);
        }
    }

    /** End any active fast scrolling touch handling, if applicable. */
    public void endFastScrolling() {
        mRv.onFastScrollCompleted();
        mTouchOffsetY = 0;
        mLastTouchY = 0;
        mIgnoreDragGesture = false;
        if (mIsDragging) {
            mIsDragging = false;
            animatePopupVisibility(false);
            showActiveScrollbar(false);
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mRv == null || (mThumbOffsetY < 0 && !mCompactPopup)) {
            return;
        }
        int saveCount = canvas.save();
        canvas.translate(getWidth() / 2, mRv.getScrollBarTop());
        mThumbDrawOffset.set(getWidth() / 2, mRv.getScrollBarTop());
        // The drawer uses a real A-Z index at rest; other fast scrollers keep the
        // original subtle track. The same RecyclerView progress mapping drives both.
        float halfW = mWidth / 2;
        if (mCompactPopup) {
            drawIndexLetters(canvas, mRv.getScrollbarTrackHeight());
        } else {
            canvas.drawRoundRect(-halfW, 0, halfW, mRv.getScrollbarTrackHeight(),
                    mWidth, mWidth, mTrackPaint);
        }

        if (mThumbOffsetY >= 0) {
            canvas.translate(0, mThumbOffsetY);
        mThumbDrawOffset.y += mThumbOffsetY;
        halfW += mThumbPadding;
        float r = getScrollThumbRadius();
        mThumbBounds.set(-halfW, 0, halfW, mThumbHeight);
        canvas.drawRoundRect(mThumbBounds, r, r, mThumbPaint);
        mThumbBounds.roundOut(SYSTEM_GESTURE_EXCLUSION_RECT.get(0));
        // swiping very close to the thumb area (not just within it's bound)
        // will also prevent back gesture
        SYSTEM_GESTURE_EXCLUSION_RECT.get(0).offset(mThumbDrawOffset.x, mThumbDrawOffset.y);
        if (Utilities.ATLEAST_Q) {
            if (mSystemGestureInsets != null) {
                SYSTEM_GESTURE_EXCLUSION_RECT.get(0).left =
                    SYSTEM_GESTURE_EXCLUSION_RECT.get(0).right - mSystemGestureInsets.right;
            }
            setSystemGestureExclusionRects(SYSTEM_GESTURE_EXCLUSION_RECT);
        }
        }
        canvas.restoreToCount(saveCount);
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        if (Utilities.ATLEAST_Q) {
            mSystemGestureInsets = insets.getSystemGestureInsets();
        } else {
            mSystemGestureInsets = null;
        }
        return super.onApplyWindowInsets(insets);
    }

    private void drawIndexLetters(Canvas canvas, int trackHeight) {
        if (trackHeight <= 0) {
            return;
        }
        float step = trackHeight / (float) ElyraDrawerLayoutPolicy.INDEX_LABELS.length();
        Paint.FontMetrics metrics = mIndexPaint.getFontMetrics();
        float baselineOffset = -(metrics.ascent + metrics.descent) / 2f;
        for (int index = 0; index < ElyraDrawerLayoutPolicy.INDEX_LABELS.length(); index++) {
            float centerY = step * (index + 0.5f);
            canvas.drawText(String.valueOf(ElyraDrawerLayoutPolicy.INDEX_LABELS.charAt(index)),
                    0, centerY + baselineOffset, mIndexPaint);
        }
    }

    private float getScrollThumbRadius() {
        return mWidth + mThumbPadding + mThumbPadding;
    }

    /**
     * Animates the width of the scrollbar.
     */
    private void showActiveScrollbar(boolean isScrolling) {
        if (mWidthAnimator != null) {
            mWidthAnimator.cancel();
        }

        mWidthAnimator = ObjectAnimator.ofInt(this, TRACK_WIDTH,
                isScrolling ? mMaxWidth : mMinWidth);
        mWidthAnimator.setDuration(SCROLL_BAR_VIS_DURATION);
        mWidthAnimator.start();
    }

    /**
     * Returns whether the specified point is inside the thumb bounds.
     */
    private boolean isNearThumb(int x, int y) {
        int offset = y - mThumbOffsetY;

        return x >= 0 && x < getWidth() && offset >= 0 && offset <= mThumbHeight;
    }

    /**
     * Returns true if AllAppsTransitionController can handle vertical motion
     * beginning at this point.
     */
    public boolean shouldBlockIntercept(int x, int y) {
        return isNearScrollBar(x);
    }

    /**
     * Returns whether the specified x position is near the scroll bar.
     */
    public boolean isNearScrollBar(int x) {
        if (mCompactPopup) {
            return x >= 0 && x <= getWidth();
        }
        return x >= (getWidth() - mMaxWidth) / 2 - mScrollbarLeftOffsetTouchDelegate
                && x <= (getWidth() + mMaxWidth) / 2;
    }

    private void animatePopupVisibility(boolean visible) {
        if (mPopupVisible == visible) {
            return;
        }
        mPopupVisible = visible;
        mPopupView.animate().cancel();
        if (mCompactPopup) {
            if (visible) {
                mPopupView.setScaleX(0.86f);
                mPopupView.setScaleY(0.86f);
            }
            mPopupView.animate()
                    .alpha(visible ? 1f : 0f)
                    .scaleX(visible ? 1f : 0.9f)
                    .scaleY(visible ? 1f : 0.9f)
                    .setDuration(visible ? 140 : 100)
                    .start();
        } else {
            mPopupView.animate()
                    .alpha(visible ? 1f : 0f)
                    .setDuration(visible ? 200 : 150)
                    .start();
        }
    }

    private void updatePopupY(int lastTouchY) {
        int height = mPopupView.getHeight();
        View parent = (View) getParent();
        float centerInParent = getTop() + mRv.getScrollBarTop() + lastTouchY
                + (getScrollThumbRadius() / 2f);
        float popupTop = centerInParent - (height / 2f);
        float minTop = parent.getPaddingTop();
        float maxTop = parent.getHeight() - parent.getPaddingBottom() - height;
        popupTop = Utilities.boundToRange(popupTop, minTop, Math.max(minTop, maxTop));
        mPopupView.setTranslationY(popupTop - mPopupView.getTop());
    }

    public boolean isHitInParent(float x, float y, Point outOffset) {
        if (mThumbOffsetY < 0 && !mCompactPopup) {
            return false;
        }
        getHitRect(sTempRect);
        sTempRect.top += mRv.getScrollBarTop();
        if (outOffset != null) {
            outOffset.set(sTempRect.left, sTempRect.top);
        }
        return sTempRect.contains((int) x, (int) y);
    }

    @Override
    public boolean hasOverlappingRendering() {
        // There is actually some overlap between the track and the thumb. But since the track
        // alpha is so low, it does not matter.
        return false;
    }
}
