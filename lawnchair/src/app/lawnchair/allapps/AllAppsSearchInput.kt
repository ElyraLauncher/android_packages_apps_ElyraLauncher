package app.lawnchair.allapps

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.Rect
import android.provider.SearchRecentSuggestions
import android.text.Selection
import android.text.SpannableStringBuilder
import android.text.Spanned.SPAN_POINT_MARK
import android.text.method.TextKeyListener
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.View.OnFocusChangeListener
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import app.lawnchair.launcher
import app.lawnchair.preferences.PreferenceManager
import app.lawnchair.preferences2.PreferenceManager2
import app.lawnchair.preferences2.subscribeBlocking
import app.lawnchair.qsb.AssistantIconView
import app.lawnchair.qsb.LawnQsbLayout.Companion.getLensIntent
import app.lawnchair.qsb.LawnQsbLayout.Companion.getSearchProvider
import app.lawnchair.qsb.ThemingMethod
import app.lawnchair.qsb.providers.Google
import app.lawnchair.qsb.providers.GoogleGo
import app.lawnchair.qsb.providers.PixelSearch
import app.lawnchair.qsb.setThemedIconResource
import app.lawnchair.search.LawnchairRecentSuggestionProvider
import app.lawnchair.search.algorithms.LawnchairSearchAlgorithm
import app.lawnchair.theme.drawable.DrawableTokens
import app.lawnchair.util.viewAttachedScope
import com.android.launcher3.Insettable
import com.android.launcher3.InvariantDeviceProfile.OnIDPChangeListener
import com.android.launcher3.LauncherState
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.android.launcher3.allapps.ActivityAllAppsContainerView
import com.android.launcher3.allapps.AllAppsStore
import com.android.launcher3.allapps.BaseAllAppsAdapter.AdapterItem
import com.android.launcher3.allapps.SearchUiManager
import com.android.launcher3.allapps.search.AllAppsSearchBarController
import com.android.launcher3.search.SearchCallback
import com.android.launcher3.util.Themes
import app.lawnchair.search.algorithms.ElyraAppColorSearchAlgorithm
import com.elyra.launcher.allapps.ElyraAppColorBucket
import com.elyra.launcher.allapps.ElyraAppIconColorExtractor
import com.elyra.launcher.allapps.ElyraBottomSearch
import com.patrykmichalik.opto.core.firstBlocking
import java.util.Locale
import kotlin.math.max
import kotlinx.coroutines.launch

class AllAppsSearchInput(context: Context, attrs: AttributeSet?) :
    FrameLayout(context, attrs),
    Insettable,
    OnIDPChangeListener,
    SearchUiManager,
    SearchCallback<AdapterItem>,
    AllAppsStore.OnUpdateListener,
    ViewTreeObserver.OnGlobalLayoutListener {

    private lateinit var hint: TextView
    private lateinit var input: FallbackSearchInputView
    private lateinit var actionButton: ImageButton
    private lateinit var searchIcon: ImageButton
    private lateinit var searchWrapper: View
    private lateinit var searchActions: LinearLayout
    private lateinit var colorButton: ImageButton
    private lateinit var colorPanel: HorizontalScrollView
    private lateinit var colorPanelRow: LinearLayout

    private lateinit var micIcon: AssistantIconView
    private lateinit var lensIcon: ImageButton

    private val qsbMarginTopAdjusting = resources.getDimensionPixelSize(R.dimen.qsb_margin_top_adjusting)
    private val allAppsSearchVerticalOffset = resources.getDimensionPixelSize(R.dimen.all_apps_search_vertical_offset)

    // Elyra: when the bottom-search flag is on, this search surface is anchored to
    // the bottom of the drawer (above the nav/gesture inset) instead of the top.
    // The read mirrors ActivityAllAppsContainerView#isElyraBottomSearch() so both
    // sides agree; when off, all positioning below is the unchanged upstream path.
    private val bottomAligned = ElyraBottomSearch.isEnabled(context)
    private val bottomSearchInsetMargin =
        resources.getDimensionPixelSize(R.dimen.elyra_spacing_medium)
    private val colorSearchEnabled = bottomAligned && ElyraBottomSearch.colorSearchEnabled(context)
    private val colorButtonSpace = resources.getDimensionPixelSize(R.dimen.search_box_height) +
        resources.getDimensionPixelSize(R.dimen.elyra_spacing_small)

    private val launcher = context.launcher
    private val searchBarController = AllAppsSearchBarController()
    private val searchQueryBuilder = SpannableStringBuilder().apply {
        Selection.setSelection(this, 0)
    }

    private lateinit var apps: LawnchairAlphabeticalAppsList<*>
    private lateinit var appsView: ActivityAllAppsContainerView<*>
    private var searchAlgorithm: LawnchairSearchAlgorithm? = null

    private var focusedResultTitle = ""
    private var canShowHint = false

    private val bg = DrawableTokens.SearchInputFg.resolve(context)
    private val bgAlphaAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 300
        interpolator = DecelerateInterpolator()
    }
    private var bgVisible = true
    private var bgAlpha = 1f
    private val suggestionsRecent = SearchRecentSuggestions(launcher, LawnchairRecentSuggestionProvider.AUTHORITY, LawnchairRecentSuggestionProvider.MODE)
    private val prefs = PreferenceManager.getInstance(launcher)
    private val prefs2 = PreferenceManager2.getInstance(launcher)

    private var initialPaddingLeft: Int = 0
    private var initialPaddingRight: Int = 0
    private val latestInsets = Rect()
    private var latestImeBottom = 0
    private var latestImeVisible = false
    private var selectedColorBucket: ElyraAppColorBucket? = null
    private var colorPanelVisible = false

    override fun onFinishInflate() {
        super.onFinishInflate()

        searchWrapper = ViewCompat.requireViewById(this, R.id.search_wrapper)
        searchWrapper.background = bg
        setupPadding()
        launcher.deviceProfile.inv.addOnChangeListener(this)
        bgAlphaAnimator.addUpdateListener { updateBgAlpha() }

        hint = ViewCompat.requireViewById(this, R.id.hint)

        input = ViewCompat.requireViewById(this, R.id.input)

        searchIcon = ViewCompat.requireViewById(this, R.id.search_icon)
        searchActions = ViewCompat.requireViewById(this, R.id.search_actions)
        colorButton = ViewCompat.requireViewById(this, R.id.color_btn)
        micIcon = ViewCompat.requireViewById(this, R.id.mic_btn)
        lensIcon = ViewCompat.requireViewById(this, R.id.lens_btn)

        val shouldShowIcons = prefs2.matchHotseatQsbStyle.firstBlocking()

        val searchProvider = getSearchProvider(context, prefs2)
        val isGoogle = searchProvider == Google || searchProvider == GoogleGo || searchProvider == PixelSearch
        val supportsLens = searchProvider == Google || searchProvider == PixelSearch

        val lensIntent = getLensIntent(context)
        val voiceIntent = AssistantIconView.getVoiceIntent(searchProvider, context)

        micIcon.isVisible = !colorSearchEnabled && shouldShowIcons && voiceIntent != null
        lensIcon.isVisible = !colorSearchEnabled && shouldShowIcons && supportsLens && lensIntent != null

        actionButton = ViewCompat.requireViewById(this, R.id.action_btn)
        with(actionButton) {
            isVisible = false
            setOnClickListener {
                selectedColorBucket = null
                colorPanelVisible = false
                input.reset()
                refreshColorSearchResults()
                updateHint()
                rebuildColorPanel()
                updateColorButtonVisibility()
            }
        }

        colorPanel = createColorDotsPanel()
        addView(colorPanel)

        with(colorButton) {
            isVisible = false
            setOnClickListener { showColorPanel() }
        }
        updateColorButtonVisibility()
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, windowInsets ->
            val imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            latestImeBottom = imeInsets.bottom
            latestImeVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime())
            applyBottomSearchInsets()
            windowInsets
        }
        ViewCompat.requestApplyInsets(this)

        prefs2.themedHotseatQsb.subscribeBlocking(scope = viewAttachedScope) { themed ->
            with(searchIcon) {
                isVisible = true

                val iconRes = if (themed) searchProvider.themedIcon else searchProvider.icon
                val resId = if (shouldShowIcons) iconRes else R.drawable.ic_qsb_search
                val isThemed = themed || resId == R.drawable.ic_qsb_search
                val method = if (shouldShowIcons) searchProvider.themingMethod else ThemingMethod.TINT

                setThemedIconResource(
                    resId = resId,
                    themed = isThemed,
                    method = method,
                )

                setOnClickListener {
                    val launcher = context.launcher
                    launcher.lifecycleScope.launch {
                        searchProvider.launch(launcher)
                    }
                }
            }
            with(micIcon) {
                setIcon(isGoogle, themed)
                setOnClickListener {
                    context.startActivity(voiceIntent)
                }
            }
            with(lensIcon) {
                if (lensIntent != null) {
                    setThemedIconResource(R.drawable.ic_lens_color, themed)
                    setOnClickListener {
                        runCatching { context.startActivity(lensIntent) }
                    }
                }
            }
            updateColorButtonVisibility()
        }
        val currentPaddingLeft = initialPaddingLeft
        val currentPaddingRight = initialPaddingRight
        input.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                if (prefs2.searchAlgorithm.firstBlocking() != LawnchairSearchAlgorithm.APP_SEARCH) {
                    input.setHint(R.string.all_apps_device_search_hint)
                } else {
                    input.setHint(R.string.all_apps_search_bar_hint)
                }

                if (input.text.toString().isEmpty()) {
                    searchAlgorithm?.doZeroStateSearch(this)
                }

                setBackgroundVisibility(false, 0f)
                animateHintVisibility(true)
                animatePadding(currentPaddingLeft / 2, currentPaddingRight / 2)

                hideColorPanel(clearFilter = false)
                updateColorButtonVisibility()
                applyBottomSearchInsets()

                // Sometimes the user has to click the input bar one more time
                // for the keyboard to show.
                input.showKeyboard()
            } else {
                setBackgroundVisibility(true, 1f)
                animateHintVisibility(false)
                if (prefs.searchResulRecentSuggestion.get()) {
                    val query = editText.text.toString()
                    suggestionsRecent.saveRecentQuery(query, null)
                }

                animatePadding(currentPaddingLeft, currentPaddingRight)
                focusedResultTitle = ""
                input.setHint("")
                hint.text = ""
                updateColorButtonVisibility()
                applyBottomSearchInsets()
            }
        }

        input.addTextChangedListener(
            beforeTextChanged = { _, _, _, _ ->
                hint.isInvisible = true
            },
            afterTextChanged = {
                updateHint()
                if (input.text.isNullOrEmpty()) {
                    searchAlgorithm?.doZeroStateSearch(this)
                    post {
                        if (selectedColorBucket != null && input.text.isNullOrEmpty()) {
                            refreshColorSearchResults()
                        }
                    }
                }
                if (input.text.toString() == "/lawnchairdebug") {
                    val enableDebugMenu = prefs.enableDebugMenu
                    enableDebugMenu.set(!enableDebugMenu.get())
                    launcher.stateManager.goToState(LauncherState.NORMAL)
                }

                updateActionButtonVisibility()
                micIcon.isVisible = !colorSearchEnabled &&
                    shouldShowIcons && voiceIntent != null && it.isNullOrEmpty()
                lensIcon.isVisible = !colorSearchEnabled &&
                    shouldShowIcons && supportsLens && lensIntent != null && it.isNullOrEmpty()
            },
        )

        val hide = prefs2.hideAppDrawerSearchBar.firstBlocking()
        if (hide) {
            isInvisible = true
            layoutParams.height = 0
        }
    }

    private fun setupPadding() {
        launcher.deviceProfile.let { dp ->
            val padding = dp.getAllAppsIconStartMargin(context)
            initialPaddingLeft = padding
            initialPaddingRight = padding
            setPadding(padding, paddingTop, padding, paddingBottom)
        }
    }

    private fun animateHintVisibility(visible: Boolean) {
        val targetAlpha = if (visible) 1f else 0f
        val duration = if (visible) 300L else 200L

        if (visible) {
            hint.alpha = 0f
            hint.isVisible = true
        }

        hint.animate()
            .alpha(targetAlpha)
            .setDuration(duration)
            .setInterpolator(FastOutSlowInInterpolator())
            .withEndAction {
                if (!visible) hint.isVisible = false
            }
            .start()
    }

    private fun animatePadding(newPaddingLeft: Int, newPaddingRight: Int) {
        val currentPaddingLeft = paddingLeft
        val currentPaddingRight = paddingRight

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { animation ->
                val fraction = animation.animatedFraction
                val leftPadding = currentPaddingLeft + (newPaddingLeft - currentPaddingLeft) * fraction
                val rightPadding = currentPaddingRight + (newPaddingRight - currentPaddingRight) * fraction
                setPadding(leftPadding.toInt(), paddingTop, rightPadding.toInt(), paddingBottom)
            }
            start()
        }
    }

    override fun setFocusedResultTitle(title: CharSequence?, sub: CharSequence?, showArrow: Boolean) {
        focusedResultTitle = title?.toString().orEmpty()
        updateHint()
    }

    override fun refreshResults() {
        onAppsUpdated()
    }

    private fun updateHint() {
        val inputString = input.text.toString()
        val inputLowerCase = inputString.lowercase(Locale.getDefault())
        val focusedLowerCase = focusedResultTitle.lowercase(Locale.getDefault())
        if (canShowHint &&
            inputLowerCase.isNotEmpty() &&
            focusedLowerCase.isNotEmpty() &&
            focusedLowerCase.matches(Regex("^[\\x00-\\x7F]*$")) &&
            focusedLowerCase.startsWith(inputLowerCase)
        ) {
            val hintColor = Themes.getAttrColor(context, android.R.attr.textColorTertiary)
            val hintText = SpannableStringBuilder(inputString)
                .append(focusedLowerCase.substring(inputLowerCase.length))
            hintText.setSpan(ForegroundColorSpan(Color.TRANSPARENT), 0, inputLowerCase.length, SPAN_POINT_MARK)
            hintText.setSpan(ForegroundColorSpan(hintColor), inputLowerCase.length, hintText.length, SPAN_POINT_MARK)
            hint.text = hintText
            hint.isVisible = true
        }
    }

    override fun onGlobalLayout() {
        canShowHint = input.layout?.getEllipsisCount(0) == 0
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        appsView.appsStore?.addUpdateListener(this)
        input.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        appsView.appsStore?.removeUpdateListener(this)
        input.viewTreeObserver.removeOnGlobalLayoutListener(this)
    }

    override fun onAppsUpdated() {
        if (colorSearchEnabled) {
            ElyraAppIconColorExtractor.clear()
        }
        searchBarController.refreshSearchResult()
    }

    override fun initializeSearch(appsView: ActivityAllAppsContainerView<*>) {
        apps = appsView.searchResultList as LawnchairAlphabeticalAppsList<*>
        this.appsView = appsView
        // Elyra bottom search is a local app drawer search: unless the user opts into
        // drawer web results, force local installed-app search so no web/provider/store
        // rows appear and no network/provider search runs. When bottom search is off,
        // the base search-algorithm preference is honored unchanged.
        val localAppsOnly = bottomAligned && !ElyraBottomSearch.webResultsEnabled(context)
        val baseAlgorithm = LawnchairSearchAlgorithm.create(context, localAppsOnly)
        val algorithm = if (colorSearchEnabled) {
            ElyraAppColorSearchAlgorithm(context, baseAlgorithm) { selectedColorBucket }
        } else {
            baseAlgorithm
        }
        this.searchAlgorithm = algorithm
        searchBarController.initialize(
            algorithm,
            input,
            launcher,
            this,
        )
        input.initialize(appsView)
    }

    override fun resetSearch() {
        hideColorPanel(clearFilter = false)
        searchBarController.reset()
    }

    override fun canHandleElyraColorPanelBack(): Boolean = colorPanelVisible

    override fun handleElyraColorPanelBack(): Boolean {
        if (!colorPanelVisible) return false
        hideColorPanel(clearFilter = false)
        return true
    }

    override fun preDispatchKeyEvent(event: KeyEvent) {
        // Determine if the key event was actual text, if so, focus the search bar and then dispatch
        // the key normally so that it can process this key event
        if (!searchBarController.isSearchFieldFocused && event.action == KeyEvent.ACTION_DOWN) {
            val unicodeChar = event.unicodeChar
            val isKeyNotWhitespace = unicodeChar > 0 &&
                !Character.isWhitespace(unicodeChar) &&
                !Character.isSpaceChar(unicodeChar)
            if (isKeyNotWhitespace) {
                val gotKey = TextKeyListener.getInstance().onKeyDown(input, searchQueryBuilder, event.keyCode, event)
                if (gotKey && searchQueryBuilder.isNotEmpty()) {
                    searchBarController.focusSearchField()
                }
            }
        }
    }

    override fun onSearchResult(query: String, items: ArrayList<AdapterItem>?) {
        if (items != null) {
            apps.setSearchResults(items)
            notifyResultChanged()
            appsView.setSearchResults(items)
        }
    }

    override fun clearSearchResult() {
        if (apps.setSearchResults(null)) {
            notifyResultChanged()
        }

        // Clear the search query
        searchQueryBuilder.clear()
        searchQueryBuilder.clearSpans()
        Selection.setSelection(searchQueryBuilder, 0)
        appsView.onClearSearchResult()
        appsView.floatingHeaderView?.setFloatingRowsCollapsed(false)
    }

    private fun notifyResultChanged() {
        appsView.mSearchRecyclerView.onSearchResultsChanged()
    }

    override fun setInsets(insets: Rect) {
        latestInsets.set(insets)
        if (applyBottomSearchInsets()) return
        (layoutParams as MarginLayoutParams).apply {
            topMargin = if (isInvisible) {
                insets.top - allAppsSearchVerticalOffset
            } else {
                max(-allAppsSearchVerticalOffset, insets.top - qsbMarginTopAdjusting)
            }
        }
        requestLayout()
    }

    private fun applyBottomSearchInsets(): Boolean {
        val relativeParams = layoutParams as? RelativeLayout.LayoutParams ?: return false
        if (!bottomAligned) return false

        val imeActive = latestImeVisible && input.hasFocus()
        if (imeActive && colorPanelVisible) {
            colorPanelVisible = false
            updateDrawerVisualState()
        }
        val bottomInset = if (imeActive) {
            max(latestInsets.bottom, latestImeBottom)
        } else {
            latestInsets.bottom
        }
        relativeParams.apply {
            removeRule(RelativeLayout.ALIGN_PARENT_TOP)
            addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            topMargin = 0
            bottomMargin = if (isInvisible) 0 else bottomInset + bottomSearchInsetMargin
        }
        if (::appsView.isInitialized) {
            val imePadding = if (imeActive) max(0, latestImeBottom - latestInsets.bottom) else 0
            appsView.setElyraBottomSearchImeInset(imePadding)
        }
        updateColorButtonVisibility()
        requestLayout()
        return true
    }

    private fun updateActionButtonVisibility() {
        actionButton.isVisible = !input.text.isNullOrEmpty() || selectedColorBucket != null
    }

    private fun updateColorButtonVisibility() {
        if (!::colorButton.isInitialized || !::colorPanel.isInitialized) return
        val imeActive = latestImeVisible && input.hasFocus()
        val showColorPanel = colorSearchEnabled && colorPanelVisible && !imeActive
        val showColorButton = colorSearchEnabled && !showColorPanel && !imeActive
        colorPanel.isVisible = showColorPanel
        searchWrapper.isVisible = !showColorPanel
        searchIcon.isVisible = !showColorPanel
        searchActions.isVisible = !showColorPanel
        colorButton.isVisible = showColorButton
        val endMargin = if (showColorButton) colorButtonSpace else 0
        searchWrapper.updateLayoutParams<FrameLayout.LayoutParams> {
            marginEnd = endMargin
        }
        searchActions.updateLayoutParams<FrameLayout.LayoutParams> {
            marginEnd = endMargin
        }
        updateActionButtonVisibility()
    }

    private fun refreshColorSearchResults() {
        val query = input.text?.toString().orEmpty()
        if (query.isBlank()) {
            searchAlgorithm?.doZeroStateSearch(this)
        } else {
            searchAlgorithm?.doSearch(query, this)
        }
    }

    private fun showColorPanel() {
        if (!colorSearchEnabled || latestImeVisible && input.hasFocus()) return
        input.clearFocus()
        colorPanelVisible = true
        rebuildColorPanel()
        updateColorButtonVisibility()
        updateDrawerVisualState()
        colorPanel.requestFocus()
    }

    private fun hideColorPanel(clearFilter: Boolean) {
        if (clearFilter) {
            selectedColorBucket = null
            if (input.text.isNullOrEmpty()) {
                clearSearchResult()
            } else {
                refreshColorSearchResults()
            }
        }
        colorPanelVisible = false
        rebuildColorPanel()
        updateColorButtonVisibility()
        updateDrawerVisualState()
    }

    private fun updateDrawerVisualState() {
        if (::appsView.isInitialized) {
            appsView.updateElyraDrawerVisualState()
        }
    }

    private fun createColorDotsPanel(): HorizontalScrollView {
        colorPanelRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                resources.getDimensionPixelSize(R.dimen.elyra_spacing_medium),
                0,
                resources.getDimensionPixelSize(R.dimen.elyra_spacing_medium),
                0,
            )
        }
        return HorizontalScrollView(context).apply {
            isVisible = false
            isFocusable = true
            isFocusableInTouchMode = true
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            background = roundedPanelBackground()
            addView(colorPanelRow, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.search_box_height),
                Gravity.CENTER,
            )
        }.also { rebuildColorPanel() }
    }

    private fun rebuildColorPanel() {
        if (!::colorPanelRow.isInitialized) return
        colorPanelRow.removeAllViews()
        colorPanelRow.addView(createColorDot(null, context.getString(R.string.elyra_color_filter_clear)))
        ElyraAppColorBucket.entries.forEach { bucket ->
            colorPanelRow.addView(createColorDot(bucket, context.getString(bucket.labelRes)))
        }
    }

    private fun createColorDot(bucket: ElyraAppColorBucket?, label: String): TextView {
        val selected = selectedColorBucket == bucket
        val size = resources.getDimensionPixelSize(R.dimen.elyra_color_dot_size)
        return TextView(context).apply {
            // "×" (multiplication sign) reads universally as clear/reset; the
            // colored swatches carry no glyph so the color is the whole affordance.
            text = if (bucket == null) "×" else ""
            gravity = Gravity.CENTER
            textSize = 13f
            setSingleLine(true)
            setTextColor(Themes.getAttrColor(context, android.R.attr.textColorSecondary))
            contentDescription = label
            background = colorDotBackground(bucket, selected)
            val margin = resources.getDimensionPixelSize(R.dimen.elyra_color_dot_spacing)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginStart = margin
                marginEnd = margin
                gravity = Gravity.CENTER_VERTICAL
            }
            setOnClickListener {
                if (bucket == null) {
                    hideColorPanel(clearFilter = true)
                } else {
                    selectedColorBucket = bucket
                    refreshColorSearchResults()
                    rebuildColorPanel()
                    updateColorButtonVisibility()
                }
            }
        }
    }

    private fun colorDotBackground(bucket: ElyraAppColorBucket?, selected: Boolean): Drawable {
        val hairline = resources.getDimensionPixelSize(R.dimen.elyra_color_dot_hairline)
        val outline = Themes.getAttrColor(context, android.R.attr.textColorTertiary)

        // The reset swatch is a neutral outlined circle so it reads distinctly from
        // the color swatches rather than as another selectable color.
        if (bucket == null) {
            return GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
                setStroke(hairline, outline)
            }
        }

        val fill = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(bucket.swatchColor)
            setStroke(hairline, Color.argb(40, Color.red(outline), Color.green(outline), Color.blue(outline)))
        }
        if (!selected) return fill

        // Selected: an accent highlight ring around the swatch with a small gap, so
        // the active color is unmistakable without recoloring the swatch itself.
        val ringWidth = resources.getDimensionPixelSize(R.dimen.elyra_color_dot_ring)
        val gap = resources.getDimensionPixelSize(R.dimen.elyra_color_dot_ring_gap)
        val ring = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.TRANSPARENT)
            setStroke(ringWidth, Themes.getAttrColor(context, android.R.attr.textColorPrimary))
        }
        val inset = ringWidth + gap
        val insetFill = InsetDrawable(fill, inset, inset, inset, inset)
        return LayerDrawable(arrayOf(ring, insetFill))
    }

    private fun roundedPanelBackground(): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = resources.getDimensionPixelSize(R.dimen.search_group_radius).toFloat()
        val base = Themes.getColorBackgroundFloating(context)
        val alpha = resources.getInteger(R.integer.elyra_surface_alpha_panel)
        setColor(Color.argb(alpha, Color.red(base), Color.green(base), Color.blue(base)))
        setStroke(
            resources.getDimensionPixelSize(R.dimen.search_decoration_padding),
            Themes.getAttrColor(context, android.R.attr.textColorTertiary),
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        // The vertical offset lifts the top-anchored bar into the header; it must
        // not be applied when the bar is anchored to the bottom of the drawer. The
        // RelativeLayout.LayoutParams check keeps the taskbar/floating case upstream.
        val bottomAnchored = bottomAligned && layoutParams is RelativeLayout.LayoutParams
        if (!bottomAnchored) {
            offsetTopAndBottom(allAppsSearchVerticalOffset)
        }
    }

    override fun getEditText() = input

    override fun setBackgroundVisibility(visible: Boolean, maxAlpha: Float) {
        if (bgVisible != visible) {
            bgVisible = visible
            bgAlpha = maxAlpha
            if (visible) {
                bgAlphaAnimator.start()
            } else {
                bgAlphaAnimator.reverse()
            }
        } else if (bgAlpha != maxAlpha && !bgAlphaAnimator.isRunning && visible) {
            bgAlpha = maxAlpha
            bgAlphaAnimator.setCurrentFraction(maxAlpha)
            updateBgAlpha()
        }
    }

    override fun getBackgroundVisibility(): Boolean {
        return bgVisible
    }

    private fun updateBgAlpha() {
        val fraction = bgAlphaAnimator.animatedFraction
        bg.alpha = (Utilities.mapRange(fraction, 0f, bgAlpha) * 255).toInt()
    }

    override fun onIdpChanged(modelPropertiesChanged: Boolean) {
        setupPadding()
        invalidate()
        requestLayout()
    }
}
