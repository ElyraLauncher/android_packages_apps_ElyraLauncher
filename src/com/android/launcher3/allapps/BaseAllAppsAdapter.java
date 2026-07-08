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
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.GridLayout;
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
import com.android.launcher3.allapps.search.SearchAdapterProvider;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.util.Themes;
import com.android.launcher3.views.ActivityContext;
import com.elyra.launcher.drawer.ElyraCategoryCardModel;

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

    public static final int NEXT_ID = 26;

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
        LinearLayout tabs = new LinearLayout(parent.getContext());
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setGravity(Gravity.CENTER);
        tabs.setPadding(dp(4), dp(4), dp(4), dp(4));
        tabs.setBackground(roundedDrawable(Themes.getColorBackgroundFloating(parent.getContext()), dp(20)));

        TextView all = createElyraSegmentButton(parent.getContext());
        TextView categories = createElyraSegmentButton(parent.getContext());
        tabs.addView(all);
        tabs.addView(categories);

        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(16), dp(8), dp(16), dp(12));
        tabs.setLayoutParams(lp);
        return tabs;
    }

    private TextView createElyraSegmentButton(Context context) {
        TextView button = new TextView(context);
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(true);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setClickable(true);
        button.setFocusable(true);
        button.setMinHeight(dp(40));
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return button;
    }

    private LinearLayout createElyraCategoryCardView(ViewGroup parent) {
        LinearLayout card = new LinearLayout(parent.getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setMinimumHeight(dp(152));
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setClickable(true);
        card.setFocusable(true);
        card.setBackground(roundedDrawable(Themes.getColorBackgroundFloating(parent.getContext()), dp(18)));

        TextView label = new TextView(parent.getContext());
        label.setSingleLine(true);
        label.setTextSize(15);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setTextColor(Themes.getAttrColor(parent.getContext(), android.R.attr.textColorPrimary));
        label.setGravity(Gravity.START);
        card.addView(label, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        GridLayout preview = new GridLayout(parent.getContext());
        preview.setColumnCount(3);
        preview.setRowCount(2);
        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        previewLp.topMargin = dp(10);
        card.addView(preview, previewLp);

        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(6), dp(6), dp(6), dp(6));
        card.setLayoutParams(lp);
        return card;
    }

    private TextView createElyraCategoryHeaderView(ViewGroup parent) {
        TextView header = new TextView(parent.getContext());
        header.setSingleLine(true);
        header.setTextSize(16);
        header.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.setTextColor(Themes.getAttrColor(parent.getContext(), android.R.attr.textColorPrimary));
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setClickable(true);
        header.setFocusable(true);
        header.setPadding(dp(16), dp(8), dp(16), dp(12));
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(4));
        header.setLayoutParams(lp);
        return header;
    }

    private void bindElyraCategoryTabs(View itemView, AdapterItem item) {
        LinearLayout tabs = (LinearLayout) itemView;
        TextView all = (TextView) tabs.getChildAt(0);
        TextView categories = (TextView) tabs.getChildAt(1);
        all.setText(R.string.elyra_drawer_tab_all);
        categories.setText(R.string.elyra_drawer_tab_categories);
        bindElyraSegment(all, !item.elyraCategoryCardsSelected);
        bindElyraSegment(categories, item.elyraCategoryCardsSelected);
        all.setOnClickListener(view -> {
            mApps.showElyraAllApps();
            scrollElyraActiveRecyclerViewToTop();
        });
        categories.setOnClickListener(view -> {
            mApps.showElyraCategoryCards();
            scrollElyraActiveRecyclerViewToTop();
        });
    }

    private void bindElyraSegment(TextView button, boolean selected) {
        int textColor = Themes.getAttrColor(button.getContext(), android.R.attr.textColorPrimary);
        button.setTextColor(textColor);
        button.setSelected(selected);
        button.setBackground(selected
                ? roundedDrawable(Themes.getColorBackground(button.getContext()), dp(16))
                : roundedDrawable(Color.TRANSPARENT, dp(16)));
    }

    private void bindElyraCategoryCard(LinearLayout card,
            ElyraCategoryCardModel.CategoryCard categoryCard) {
        if (categoryCard == null) {
            card.setVisibility(GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);
        TextView label = (TextView) card.getChildAt(0);
        GridLayout preview = (GridLayout) card.getChildAt(1);
        label.setText(categoryCard.getLabel());
        card.setContentDescription(categoryCard.getLabel());
        card.setOnClickListener(view -> {
            mApps.selectElyraCategory(categoryCard.getCategory());
            scrollElyraActiveRecyclerViewToTop();
        });

        preview.removeAllViews();
        for (AppInfo app : categoryCard.getPreview()) {
            ImageView icon = new ImageView(card.getContext());
            icon.setImageDrawable(app.newIcon(card.getContext(), false));
            icon.setContentDescription(app.title);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = dp(36);
            lp.height = dp(36);
            lp.setMargins(0, 0, dp(8), dp(8));
            preview.addView(icon, lp);
        }
    }

    private void bindElyraCategoryHeader(TextView header, CharSequence label) {
        CharSequence safeLabel = label == null ? "" : label;
        header.setText("< " + safeLabel);
        header.setContentDescription(safeLabel);
        header.setOnClickListener(view -> {
            mApps.showElyraCategoryCards();
            scrollElyraActiveRecyclerViewToTop();
        });
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
    }

    private GradientDrawable roundedDrawable(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
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
