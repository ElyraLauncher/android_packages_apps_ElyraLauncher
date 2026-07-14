/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.launcher3.allapps;

import static android.view.View.GONE;

import static com.android.launcher3.allapps.SectionDecorationInfo.ROUND_BOTTOM_LEFT;
import static com.android.launcher3.allapps.SectionDecorationInfo.ROUND_BOTTOM_RIGHT;
import static com.android.launcher3.allapps.SectionDecorationInfo.ROUND_NOTHING;
import static com.android.launcher3.allapps.SectionDecorationInfo.ROUND_TOP_LEFT;
import static com.android.launcher3.allapps.SectionDecorationInfo.ROUND_TOP_RIGHT;
import static com.android.launcher3.allapps.UserProfileManager.STATE_DISABLED;
import static com.android.launcher3.allapps.UserProfileManager.STATE_ENABLED;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.Flags;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.search.SearchAdapterProvider;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.util.Themes;
import com.android.launcher3.views.ActivityContext;
import com.elyra.launcher.drawer.ElyraCategoryCardModel;
import com.elyra.launcher.drawer.ElyraDrawerLayoutPolicy;
import com.elyra.launcher.drawer.ElyraDrawerOptions;

import java.util.List;

/**
 * Adapter for all the apps.
 *
 * @param <T> Type of context inflating all apps.
 */
public abstract class BaseAllAppsAdapter<T extends Context & ActivityContext> extends
        RecyclerView.Adapter<BaseAllAppsAdapter.ViewHolder> {

    public static final String TAG = "BaseAllAppsAdapter";

    // A normal icon
    public static final int VIEW_TYPE_ICON = 1 << 1;
    // The message shown when there are no filtered results
    public static final int VIEW_TYPE_EMPTY_SEARCH = 1 << 2;

    // The message to continue to a market search when there are no filtered results
    public static final int VIEW_TYPE_SEARCH_MARKET = 1 << 3;

    // A divider that separates the apps list and the search market button
    public static final int VIEW_TYPE_ALL_APPS_DIVIDER = 1 << 4;

    public static final int VIEW_TYPE_WORK_DISABLED_CARD = 1 << 5;
    public static final int VIEW_TYPE_PRIVATE_SPACE_HEADER = 1 << 6;
    public static final int VIEW_TYPE_PRIVATE_SPACE_SYS_APPS_DIVIDER = 1 << 7;
    public static final int VIEW_TYPE_WORK_EDU_CARD = 1 << 8;

    public static final int VIEW_TYPE_FOLDER = 1 << 9;

    public static final int VIEW_TYPE_ELYRA_CATEGORY_TABS = 1 << 23;
    public static final int VIEW_TYPE_ELYRA_CATEGORY_CARD = 1 << 24;
    public static final int VIEW_TYPE_ELYRA_CATEGORY_HEADER = 1 << 25;
    public static final int VIEW_TYPE_ELYRA_SUGGESTIONS = 1 << 26;

    public static final int NEXT_ID = 27;

    // Common view type masks
    public static final int VIEW_TYPE_MASK_DIVIDER = VIEW_TYPE_ALL_APPS_DIVIDER;
    public static final int VIEW_TYPE_MASK_ICON = VIEW_TYPE_ICON | VIEW_TYPE_FOLDER;

    public static final int VIEW_TYPE_MASK_PRIVATE_SPACE_HEADER = VIEW_TYPE_PRIVATE_SPACE_HEADER;
    public static final int VIEW_TYPE_MASK_PRIVATE_SPACE_SYS_APPS_DIVIDER = VIEW_TYPE_PRIVATE_SPACE_SYS_APPS_DIVIDER;

    protected final SearchAdapterProvider<?> mAdapterProvider;

    /**
     * ViewHolder for each icon.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View v) {
            super(v);
        }
    }

    /**
     * Sets the number of apps to be displayed in one row of the all apps screen.
     */
    public abstract void setAppsPerRow(int appsPerRow);

    /**
     * Info about a particular adapter item (can be either section or app)
     */
    public static class AdapterItem {

        /** Common properties */
        // The type of this item
        public int viewType;

        // The row that this item shows up on
        public int rowIndex;
        // The index of this app in the row
        public int rowAppIndex;
        // The associated ItemInfoWithIcon for the item
        public AppInfo itemInfo = new AppInfo();
        
        public FolderInfo folderInfo = new FolderInfo();

        public ElyraCategoryCardModel.CategoryCard elyraCategoryCard = null;
        public boolean elyraCategoryCardsSelected = false;
        public CharSequence elyraCategoryLabel = null;
        public List<AppInfo> elyraSuggestions = null;

        // Private App Decorator
        public SectionDecorationInfo decorationInfo = null;

        public AdapterItem(int viewType) {
            this.viewType = viewType;
        }

        /**
         * Factory method for AppIcon AdapterItem
         */
        public static AdapterItem asApp(AppInfo appInfo) {
            AdapterItem item = new AdapterItem(VIEW_TYPE_ICON);
            item.itemInfo = appInfo;
            return item;
        }
        
        public static AdapterItem asFolder(FolderInfo folderInfo) {
            AdapterItem item = new AdapterItem(VIEW_TYPE_FOLDER);
            item.folderInfo = folderInfo;
            return item;
        }

        public static AdapterItem asElyraCategoryTabs(boolean categoryCardsSelected) {
            AdapterItem item = new AdapterItem(VIEW_TYPE_ELYRA_CATEGORY_TABS);
            item.elyraCategoryCardsSelected = categoryCardsSelected;
            return item;
        }

        public static AdapterItem asElyraSuggestions(List<AppInfo> suggestions) {
            AdapterItem item = new AdapterItem(VIEW_TYPE_ELYRA_SUGGESTIONS);
            item.elyraSuggestions = suggestions;
            return item;
        }

        public static AdapterItem asElyraCategoryCard(
                ElyraCategoryCardModel.CategoryCard categoryCard) {
            AdapterItem item = new AdapterItem(VIEW_TYPE_ELYRA_CATEGORY_CARD);
            item.elyraCategoryCard = categoryCard;
            return item;
        }

        public static AdapterItem asElyraCategoryHeader(CharSequence categoryLabel) {
            AdapterItem item = new AdapterItem(VIEW_TYPE_ELYRA_CATEGORY_HEADER);
            item.elyraCategoryLabel = categoryLabel;
            return item;
        }

        public static AdapterItem asAppWithDecorationInfo(AppInfo appInfo,
                SectionDecorationInfo decorationInfo) {
            AdapterItem item = asApp(appInfo);
            item.decorationInfo = decorationInfo;
            return item;
        }

        protected boolean isCountedForAccessibility() {
            return viewType == VIEW_TYPE_ICON || viewType == VIEW_TYPE_FOLDER
                    || viewType == VIEW_TYPE_ELYRA_CATEGORY_CARD;
        }

        /**
         * Returns true if the items represent the same object
         */
        public boolean isSameAs(AdapterItem other) {
            return (other.viewType == viewType) && (other.getClass() == getClass());
        }

        /**
         * This is called only if {@link #isSameAs} returns true to check if the
         * contents are same
         * as well. Returning true will prevent redrawing of thee item.
         */
        public boolean isContentSame(AdapterItem other) {
            return itemInfo == null && other.itemInfo == null;
        }

        @Nullable
        public SectionDecorationInfo getDecorationInfo() {
            return decorationInfo;
        }

        /** Sets the alpha of the decorator for this item. */
        protected void setDecorationFillAlpha(int alpha) {
            if (decorationInfo == null || decorationInfo.getDecorationHandler() == null) {
                return;
            }
            decorationInfo.getDecorationHandler().setFillAlpha(alpha);
        }
    }

    protected final T mActivityContext;
    public final AlphabeticalAppsList<T> mApps;
    // The text to show when there are no search results and no market search
    // handler.
    protected int mAppsPerRow;

    protected final LayoutInflater mLayoutInflater;
    protected final OnClickListener mOnIconClickListener;
    protected final OnLongClickListener mOnIconLongClickListener;
    protected OnFocusChangeListener mIconFocusListener;

    public BaseAllAppsAdapter(T activityContext, LayoutInflater inflater,
            AlphabeticalAppsList<T> apps, SearchAdapterProvider<?> adapterProvider) {
        mActivityContext = activityContext;
        mApps = apps;
        mLayoutInflater = inflater;

        mOnIconClickListener = mActivityContext.getItemOnClickListener();
        mOnIconLongClickListener = mActivityContext.getAllAppsItemLongClickListener();

        mAdapterProvider = adapterProvider;
    }

    /** Checks if the passed viewType represents all apps divider. */
    public static boolean isDividerViewType(int viewType) {
        return isViewType(viewType, VIEW_TYPE_MASK_DIVIDER);
    }

    /** Checks if the passed viewType represents all apps icon. */
    public static boolean isIconViewType(int viewType) {
        return isViewType(viewType, VIEW_TYPE_MASK_ICON);
    }

    /** Checks if the passed viewType represents private space header. */
    public static boolean isPrivateSpaceHeaderView(int viewType) {
        return isViewType(viewType, VIEW_TYPE_MASK_PRIVATE_SPACE_HEADER);
    }

    /**
     * Checks if the passed viewType represents private space system apps divider.
     */
    public static boolean isPrivateSpaceSysAppsDividerView(int viewType) {
        return isViewType(viewType, VIEW_TYPE_MASK_PRIVATE_SPACE_SYS_APPS_DIVIDER);
    }

    public void setIconFocusListener(OnFocusChangeListener focusListener) {
        mIconFocusListener = focusListener;
    }

    /**
     * Returns the layout manager.
     */
    public abstract RecyclerView.LayoutManager getLayoutManager();


    private View createElyraCategoryTabsView(ViewGroup parent) {
        Context context = parent.getContext();

        FrameLayout header = new FrameLayout(context);
        header.setMinimumHeight(dimen(R.dimen.elyra_drawer_header_height));

        FrameLayout capsule = new FrameLayout(context);
        capsule.setPadding(dp(3), dp(3), dp(3), dp(3));
        capsule.setBackground(roundedDrawableWithStroke(
                context.getColor(R.color.elyra_drawer_capsule_surface),
                dp(22),
                context.getColor(R.color.elyra_drawer_outline_level_1),
                dp(1)));

        View selectedPill = new View(context);
        selectedPill.setBackground(roundedDrawableWithStroke(
                context.getColor(R.color.elyra_drawer_selected_pill_surface),
                dp(19),
                context.getColor(R.color.elyra_drawer_outline_level_2),
                dp(1)));
        FrameLayout.LayoutParams pillLp = new FrameLayout.LayoutParams(
                0, dimen(R.dimen.elyra_drawer_segment_indicator_height));
        pillLp.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
        capsule.addView(selectedPill, pillLp);

        LinearLayout tabs = new LinearLayout(context);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setGravity(Gravity.CENTER);
        tabs.addView(createElyraSegmentButton(context));
        tabs.addView(createElyraSegmentButton(context));
        capsule.addView(tabs, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        FrameLayout.LayoutParams capsuleLp = new FrameLayout.LayoutParams(
                dimen(R.dimen.elyra_drawer_segment_width),
                dimen(R.dimen.elyra_drawer_segment_height),
                Gravity.CENTER);
        header.addView(capsule, capsuleLp);

        ImageButton overflow = new ImageButton(context);
        overflow.setImageResource(R.drawable.elyra_ic_more_vert);
        overflow.setImageTintList(ColorStateList.valueOf(
                context.getColor(R.color.elyra_drawer_icon_foreground)));
        overflow.setBackgroundResource(R.drawable.pill_ripple);
        overflow.setScaleType(ImageView.ScaleType.CENTER);
        overflow.setContentDescription(context.getString(R.string.elyra_drawer_options_title));
        FrameLayout.LayoutParams overflowLp = new FrameLayout.LayoutParams(
                dimen(R.dimen.elyra_drawer_overflow_size),
                dimen(R.dimen.elyra_drawer_overflow_size),
                Gravity.END | Gravity.CENTER_VERTICAL);
        header.addView(overflow, overflowLp);

        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(16), dp(8), dp(12), dp(8));
        header.setLayoutParams(lp);
        return header;
    }

    private TextView createElyraSegmentButton(Context context) {
        TextView button = new TextView(context);
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(true);
        button.setIncludeFontPadding(false);
        button.setTextSize(13);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setClickable(true);
        button.setFocusable(true);
        button.setMinHeight(dimen(R.dimen.elyra_drawer_segment_indicator_height));
        button.setPadding(dp(10), 0, dp(10), 0);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        return button;
    }

    private LinearLayout createElyraSuggestionsView(ViewGroup parent) {
        Context context = parent.getContext();
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setClipChildren(false);
        card.setClipToPadding(false);
        card.setPadding(dp(14), dp(10), dp(14), dp(10));
        card.setElevation(dp(1));
        card.setBackground(roundedDrawableWithStroke(
                context.getColor(R.color.elyra_drawer_suggestion_surface),
                dp(24),
                context.getColor(R.color.elyra_drawer_card_outline),
                dp(1)));

        LinearLayout icons = new LinearLayout(context);
        icons.setOrientation(LinearLayout.HORIZONTAL);
        icons.setGravity(Gravity.CENTER);
        // Icon-only cells keep the strip compact. The real app label remains on
        // each icon as its accessibility description.
        icons.setClipChildren(false);
        icons.setClipToPadding(false);
        card.addView(icons, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(16), dp(4), dp(16), dp(6));
        card.setLayoutParams(lp);
        return card;
    }

    private LinearLayout createElyraCategoryCardView(ViewGroup parent) {
        Context context = parent.getContext();
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.TOP);
        card.setMinimumHeight(dp(152));
        card.setPadding(dp(14), dp(10), dp(14), dp(10));
        card.setClickable(true);
        card.setFocusable(true);
        card.setElevation(dp(1));
        card.setBackground(roundedRippleDrawable(
                context.getColor(R.color.elyra_drawer_card_surface),
                dimen(R.dimen.elyra_radius_large),
                context.getColor(R.color.elyra_drawer_card_outline),
                dp(1)));

        TextView label = new TextView(context);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setIncludeFontPadding(false);
        label.setTextSize(14);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setTextColor(context.getColor(R.color.elyra_drawer_text_primary));
        label.setGravity(Gravity.START);
        card.addView(label, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView count = new TextView(context);
        count.setSingleLine(true);
        count.setIncludeFontPadding(false);
        count.setTextSize(11);
        count.setTextColor(context.getColor(R.color.elyra_drawer_text_secondary));
        count.setGravity(Gravity.START);
        LinearLayout.LayoutParams countLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        countLp.topMargin = dp(2);
        card.addView(count, countLp);

        GridLayout preview = new GridLayout(context);
        preview.setColumnCount(2);
        preview.setRowCount(2);
        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(dp(76), dp(76));
        previewLp.gravity = Gravity.CENTER_HORIZONTAL;
        previewLp.topMargin = dp(8);
        card.addView(preview, previewLp);

        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(152));
        lp.setMargins(dp(7), dp(4), dp(7), dp(4));
        card.setLayoutParams(lp);
        return card;
    }

    private TextView createElyraCategoryHeaderView(ViewGroup parent) {
        TextView header = new TextView(parent.getContext());
        header.setSingleLine(true);
        header.setTextSize(17);
        header.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.setTextColor(drawerTextColor(parent.getContext(), R.color.elyra_drawer_text_primary,
                android.R.attr.textColorPrimary));
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setClickable(false);
        header.setFocusable(false);
        header.setPadding(dp(24), dp(8), dp(24), dp(12));
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(4));
        header.setLayoutParams(lp);
        return header;
    }

    private void bindElyraCategoryTabs(View itemView, AdapterItem item) {
        FrameLayout header = (FrameLayout) itemView;
        FrameLayout capsule = (FrameLayout) header.getChildAt(0);
        ImageButton overflow = (ImageButton) header.getChildAt(1);
        LinearLayout tabs = (LinearLayout) capsule.getChildAt(1);
        TextView all = (TextView) tabs.getChildAt(0);
        TextView categories = (TextView) tabs.getChildAt(1);
        all.setText(R.string.elyra_drawer_tab_all);
        categories.setText(R.string.elyra_drawer_tab_categories);
        bindElyraSegment(all, !item.elyraCategoryCardsSelected);
        bindElyraSegment(categories, item.elyraCategoryCardsSelected);
        updateElyraSegmentPill(capsule, item.elyraCategoryCardsSelected);
        all.setOnClickListener(view -> {
            mApps.showElyraAllApps();
            scrollElyraActiveRecyclerViewToTop();
        });
        categories.setOnClickListener(view -> {
            mApps.showElyraCategoryCards();
            scrollElyraActiveRecyclerViewToTop();
        });
        overflow.setOnClickListener(view ->
                ElyraDrawerOptions.show(view, () -> {
                    mApps.showElyraAllApps();
                    scrollElyraActiveRecyclerViewToTop();
                }));
        updateElyraDrawerVisualState();
    }

    private void bindElyraSegment(TextView button, boolean selected) {
        button.setSelected(selected);
        Context context = button.getContext();
        int selectedColor = context.getColor(R.color.elyra_drawer_text_selected);
        int unselectedColor = context.getColor(R.color.elyra_drawer_text_unselected);
        button.setTextColor(selected ? selectedColor : unselectedColor);
        button.setBackgroundColor(Color.TRANSPARENT);
    }

    private void updateElyraSegmentPill(FrameLayout capsule, boolean categoriesSelected) {
        View pill = capsule.getChildAt(0);
        capsule.post(() -> {
            int innerWidth = capsule.getWidth() - capsule.getPaddingLeft() - capsule.getPaddingRight();
            if (innerWidth <= 0) {
                return;
            }
            int pillWidth = innerWidth / 2;
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) pill.getLayoutParams();
            if (lp.width != pillWidth) {
                lp.width = pillWidth;
                lp.height = capsule.getHeight() - capsule.getPaddingTop() - capsule.getPaddingBottom();
                pill.setLayoutParams(lp);
            }
            float target = categoriesSelected ? pillWidth : 0f;
            Object previous = pill.getTag();
            if (previous == null) {
                pill.setTranslationX(target);
            } else if (!previous.equals(categoriesSelected)) {
                pill.animate().translationX(target).setDuration(160).start();
            } else {
                pill.setTranslationX(target);
            }
            pill.setTag(categoriesSelected);
        });
    }

    private void bindElyraSuggestions(LinearLayout card, List<AppInfo> suggestions) {
        LinearLayout icons = (LinearLayout) card.getChildAt(0);
        icons.removeAllViews();
        if (suggestions == null) {
            card.setVisibility(GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);
        int maxSuggestions = ElyraDrawerLayoutPolicy.suggestionCount(
                mActivityContext.getDeviceProfile().numShownAllAppsColumns);
        int suggestionIndex = 0;
        for (AppInfo app : suggestions) {
            if (suggestionIndex++ >= maxSuggestions) {
                break;
            }
            BubbleTextView icon = (BubbleTextView) mLayoutInflater.inflate(
                    R.layout.all_apps_prediction_row_icon, icons, false);
            icon.reset();
            icon.applyFromApplicationInfo(app);
            icon.setText(null);
            icon.setContentDescription(app.title);
            icon.setGravity(Gravity.CENTER);
            icon.setCenterVertically(false);
            icon.setCompoundDrawablePadding(0);
            icon.setPadding(0, 0, 0, 0);
            icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            icon.setOnClickListener(mOnIconClickListener);
            icon.setOnLongClickListener(mOnIconLongClickListener);
            int iconCellHeight = Math.max(
                    dp(48), mActivityContext.getDeviceProfile().allAppsIconSizePx);
            icon.setLayoutParams(new LinearLayout.LayoutParams(
                    0, iconCellHeight, 1f));
            icons.addView(icon);
        }
    }

    private void bindElyraCategoryCard(LinearLayout card,
            ElyraCategoryCardModel.CategoryCard categoryCard) {
        if (categoryCard == null) {
            card.setVisibility(GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);
        TextView label = (TextView) card.getChildAt(0);
        TextView count = (TextView) card.getChildAt(1);
        GridLayout preview = (GridLayout) card.getChildAt(2);
        int appCount = categoryCard.getApps().size();
        CharSequence countLabel = card.getResources().getQuantityString(
                R.plurals.elyra_category_app_count, appCount, appCount);
        label.setText(categoryCard.getLabel());
        count.setText(countLabel);
        card.setContentDescription(categoryCard.getLabel() + ", " + countLabel);
        card.setOnClickListener(view -> {
            mApps.selectElyraCategory(categoryCard.getCategory());
            scrollElyraActiveRecyclerViewToTop();
        });

        preview.removeAllViews();
        for (int previewIndex = 0; previewIndex < 4; previewIndex++) {
            View previewItem;
            if (previewIndex == 3 && appCount > 4) {
                TextView more = new TextView(card.getContext());
                more.setText(card.getContext().getString(
                        R.string.elyra_category_more_apps, appCount - 3));
                more.setTextSize(11);
                more.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                more.setGravity(Gravity.CENTER);
                more.setTextColor(
                        card.getContext().getColor(R.color.elyra_drawer_text_primary));
                more.setBackground(roundedDrawableWithStroke(
                        card.getContext().getColor(R.color.elyra_drawer_capsule_surface),
                        dp(10),
                        card.getContext().getColor(R.color.elyra_drawer_card_outline),
                        dp(1)));
                previewItem = more;
            } else if (previewIndex < categoryCard.getPreview().size()) {
                AppInfo app = categoryCard.getPreview().get(previewIndex);
                ImageView icon = new ImageView(card.getContext());
                icon.setImageDrawable(app.newIcon(card.getContext(), false));
                icon.setContentDescription(app.title);
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                previewItem = icon;
            } else {
                previewItem = new View(card.getContext());
                previewItem.setVisibility(View.INVISIBLE);
                previewItem.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            }
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = dp(32);
            lp.height = dp(32);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            preview.addView(previewItem, lp);
        }
        updateElyraDrawerVisualState();
    }

    private void bindElyraCategoryHeader(TextView header, CharSequence label) {
        CharSequence safeLabel = label == null ? "" : label;
        header.setText(safeLabel);
        header.setContentDescription(safeLabel);
        header.setOnClickListener(null);
        updateElyraDrawerVisualState();
    }

    private void scrollElyraActiveRecyclerViewToTop() {
        ActivityAllAppsContainerView<?> appsView = mActivityContext.getAppsView();
        if (appsView == null) {
            return;
        }
        AllAppsRecyclerView recyclerView = appsView.getActiveRecyclerView();
        if (recyclerView != null) {
            recyclerView.scrollToTop();
        }
        appsView.updateElyraDrawerVisualState();
    }

    private void updateElyraDrawerVisualState() {
        ActivityAllAppsContainerView<?> appsView = mActivityContext.getAppsView();
        if (appsView != null) {
            appsView.updateElyraDrawerVisualState();
        }
    }

    private GradientDrawable roundedDrawable(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private GradientDrawable roundedDrawableWithStroke(int color, int radius, int strokeColor,
            int strokeWidth) {
        GradientDrawable drawable = roundedDrawable(color, radius);
        drawable.setStroke(strokeWidth, strokeColor);
        return drawable;
    }

    private Drawable roundedRippleDrawable(int color, int radius, int strokeColor,
            int strokeWidth) {
        GradientDrawable content = roundedDrawableWithStroke(
                color, radius, strokeColor, strokeWidth);
        GradientDrawable mask = roundedDrawable(Color.WHITE, radius);
        int rippleColor = translucentColor(
                Themes.getAttrColor(mActivityContext, android.R.attr.colorControlHighlight), 48);
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, mask);
    }

    private int translucentColor(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private int drawerTextColor(Context context, int lightColorRes, int darkAttr) {
        return Utilities.isDarkTheme(context)
                ? Themes.getAttrColor(context, darkAttr)
                : context.getColor(lightColorRes);
    }

    private int dimen(int resId) {
        return mActivityContext.getResources().getDimensionPixelSize(resId);
    }

    private int dp(int value) {
        return Math.round(value * mActivityContext.getResources().getDisplayMetrics().density);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_ICON:
                int layout = !FeatureFlags.twoLineAllApps(parent.getContext()) ? R.layout.all_apps_icon
                        : R.layout.all_apps_icon_twoline;
                BubbleTextView icon = (BubbleTextView) mLayoutInflater.inflate(
                        layout, parent, false);
                icon.setLongPressTimeoutFactor(1f);
                icon.setOnFocusChangeListener(mIconFocusListener);
                icon.setOnClickListener(mOnIconClickListener);
                icon.setOnLongClickListener(mOnIconLongClickListener);
                // Ensure the all apps icon height matches the workspace icons in portrait mode.
                icon.getLayoutParams().height = mActivityContext.getDeviceProfile().allAppsCellHeightPx;
                return new ViewHolder(icon);
            case VIEW_TYPE_EMPTY_SEARCH:
                return new ViewHolder(mLayoutInflater.inflate(R.layout.all_apps_empty_search,
                        parent, false));
            case VIEW_TYPE_ALL_APPS_DIVIDER, VIEW_TYPE_PRIVATE_SPACE_SYS_APPS_DIVIDER:
                return new ViewHolder(mLayoutInflater.inflate(
                        R.layout.private_space_divider, parent, false));
            case VIEW_TYPE_WORK_EDU_CARD:
                return new ViewHolder(mLayoutInflater.inflate(
                        R.layout.work_apps_edu, parent, false));
            case VIEW_TYPE_WORK_DISABLED_CARD:
                return new ViewHolder(mLayoutInflater.inflate(
                        R.layout.work_apps_paused, parent, false));
            case VIEW_TYPE_PRIVATE_SPACE_HEADER:
                return new ViewHolder(mLayoutInflater.inflate(
                        R.layout.private_space_header, parent, false));
            case VIEW_TYPE_FOLDER:
                FrameLayout fl = new FrameLayout(mActivityContext);
                ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
                return new ViewHolder(fl);
            case VIEW_TYPE_ELYRA_CATEGORY_TABS:
                return new ViewHolder(createElyraCategoryTabsView(parent));
            case VIEW_TYPE_ELYRA_CATEGORY_CARD:
                return new ViewHolder(createElyraCategoryCardView(parent));
            case VIEW_TYPE_ELYRA_SUGGESTIONS:
                return new ViewHolder(createElyraSuggestionsView(parent));
            case VIEW_TYPE_ELYRA_CATEGORY_HEADER:
                return new ViewHolder(createElyraCategoryHeaderView(parent));
            default:
                if (mAdapterProvider.isViewSupported(viewType)) {
                    return mAdapterProvider.onCreateViewHolder(mLayoutInflater, parent, viewType);
                }
                throw new RuntimeException("Unexpected view type : " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.itemView.setVisibility(View.VISIBLE);
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_ICON: {
                AdapterItem adapterItem = mApps.getAdapterItems().get(position);
                BubbleTextView icon = (BubbleTextView) holder.itemView;
                icon.reset();
                icon.applyFromApplicationInfo(adapterItem.itemInfo);
                icon.setOnFocusChangeListener(mIconFocusListener);
                PrivateProfileManager privateProfileManager = mApps.getPrivateProfileManager();
                if (privateProfileManager != null) {
                    // Set the alpha of the private space icon to 0 upon expanding the header so the
                    // alpha can animate -> 1. This should only be in effect when doing a
                    // transitioning between Locked/Unlocked state.
                    boolean isPrivateSpaceItem = privateProfileManager.isPrivateSpaceItem(adapterItem);
                    if (icon.getAlpha() == 0 || icon.getAlpha() == 1) {
                        icon.setAlpha(isPrivateSpaceItem
                                && privateProfileManager.isStateTransitioning()
                                && (privateProfileManager.isScrolling() ||
                                        privateProfileManager.getReadyToAnimate())
                                && privateProfileManager.getCurrentState() == STATE_ENABLED
                                        ? 0
                                        : 1);
                    }
                    // Views can still be bounded before the app list is updated hence showing icons
                    // after collapsing.
                    if (privateProfileManager.getCurrentState() == STATE_DISABLED
                            && isPrivateSpaceItem) {
                        adapterItem.decorationInfo = null;
                        icon.setVisibility(GONE);
                    }
                }
                break;
            }
            case VIEW_TYPE_EMPTY_SEARCH: {
                AppInfo info = mApps.getAdapterItems().get(position).itemInfo;
                if (info != null) {
                    ((TextView) holder.itemView).setText(mActivityContext.getString(
                            R.string.all_apps_no_search_results, info.title));
                }
                break;
            }
            case VIEW_TYPE_PRIVATE_SPACE_HEADER:
                RelativeLayout psHeaderLayout = holder.itemView.findViewById(
                        R.id.ps_header_layout);
                mApps.getPrivateProfileManager().bindPrivateSpaceHeaderViewElements(psHeaderLayout);
                AdapterItem adapterItem = mApps.getAdapterItems().get(position);
                int roundRegions = ROUND_TOP_LEFT | ROUND_TOP_RIGHT;
                if (mApps.getPrivateProfileManager().getCurrentState() == STATE_DISABLED) {
                    roundRegions |= (ROUND_BOTTOM_LEFT | ROUND_BOTTOM_RIGHT);
                }
                adapterItem.decorationInfo = new SectionDecorationInfo(mActivityContext, roundRegions,
                        false /* decorateTogether */);
                break;
            case VIEW_TYPE_PRIVATE_SPACE_SYS_APPS_DIVIDER:
                adapterItem = mApps.getAdapterItems().get(position);
                adapterItem.decorationInfo = mApps.getPrivateProfileManager().getCurrentState() == STATE_DISABLED ? null
                        : new SectionDecorationInfo(mActivityContext,
                                ROUND_NOTHING, true /* decorateTogether */);
                break;
            case VIEW_TYPE_ALL_APPS_DIVIDER:
            case VIEW_TYPE_WORK_DISABLED_CARD:
                // nothing to do
                break;
            case VIEW_TYPE_WORK_EDU_CARD:
                ((WorkEduCard) holder.itemView).setPosition(position);
                break;
            case VIEW_TYPE_FOLDER:
                FolderInfo folderInfo = mApps.getAdapterItems().get(position).folderInfo;
                ViewGroup container = (ViewGroup) holder.itemView;
                container.removeAllViews();
                container.addView(FolderIcon.inflateFolderAndIcon(R.layout.all_apps_folder_icon, mActivityContext,
                    container, folderInfo));
                break;
            case VIEW_TYPE_ELYRA_CATEGORY_TABS:
                bindElyraCategoryTabs(holder.itemView, mApps.getAdapterItems().get(position));
                break;
            case VIEW_TYPE_ELYRA_CATEGORY_CARD:
                bindElyraCategoryCard((LinearLayout) holder.itemView,
                        mApps.getAdapterItems().get(position).elyraCategoryCard);
                break;
            case VIEW_TYPE_ELYRA_SUGGESTIONS:
                bindElyraSuggestions((LinearLayout) holder.itemView,
                        mApps.getAdapterItems().get(position).elyraSuggestions);
                break;
            case VIEW_TYPE_ELYRA_CATEGORY_HEADER:
                bindElyraCategoryHeader((TextView) holder.itemView,
                        mApps.getAdapterItems().get(position).elyraCategoryLabel);
                break;
            default:
                if (mAdapterProvider.isViewSupported(holder.getItemViewType())) {
                    mAdapterProvider.onBindView(holder, position);
                }
        }
    }

    @Override
    public boolean onFailedToRecycleView(ViewHolder holder) {
        // Always recycle and we will reset the view when it is bound
        return true;
    }

    @Override
    public int getItemCount() {
        return mApps.getAdapterItems().size();
    }

    @Override
    public int getItemViewType(int position) {
        AdapterItem item = mApps.getAdapterItems().get(position);
        return item.viewType;
    }

    protected static boolean isViewType(int viewType, int viewTypeMask) {
        return (viewType & viewTypeMask) != 0;
    }

}
