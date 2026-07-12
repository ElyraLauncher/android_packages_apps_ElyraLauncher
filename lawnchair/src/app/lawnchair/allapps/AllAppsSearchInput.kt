package app.lawnchair.allapps

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.View.OnFocusChangeListener
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import com.elyra.launcher.allapps.ElyraColorPickerLayout
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
    private lateinit var bottomControls: LinearLayout
    private lateinit var searchField: FrameLayout
    private lateinit var actionButton: ImageButton
    private lateinit var searchIcon: ImageButton
    private lateinit var searchWrapper: View
    private lateinit var searchActions: LinearLayout
    private lateinit var colorButton: ImageButton
    private lateinit var colorPanel: LinearLayout
    private lateinit var colorPanelRow: GridLayout
    private lateinit var colorPopup: PopupWindow

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
        resources.getDimensionPixelSize(R.dimen.elyra_bottom_search_bottom_spacing)
    private val colorSearchEnabled = bottomAligned && ElyraBottomSearch.colorSearchEnabled(context)

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

    private val bg = if (bottomAligned) {
        requireNotNull(context.getDrawable(R.drawable.elyra_drawer_search_surface))
    } else {
        DrawableTokens.SearchInputFg.resolve(context)
    }
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

        bottomControls = ViewCompat.requireViewById(this, R.id.bottom_controls)
        searchField = ViewCompat.requireViewById(this, R.id.search_field)
        searchWrapper = ViewCompat.requireViewById(this, R.id.search_wrapper)
        searchWrapper.background = bg
        setupPadding()
        launcher.deviceProfile.inv.addOnChangeListener(this)
        bgAlphaAnimator.addUpdateListener { updateBgAlpha() }

        hint = ViewCompat.requireViewById(this, R.id.hint)

        input = ViewCompat.requireViewById(this, R.id.input)
        if (bottomAligned && !Utilities.isDarkTheme(context)) {
            val primary = ContextCompat.getColor(context, R.color.elyra_drawer_text_primary)
            val hintColor = ContextCompat.getColor(context, R.color.elyra_drawer_search_hint)
            hint.setTextColor(hintColor)
            input.setTextColor(primary)
            input.setHintTextColor(hintColor)
            if (Utilities.ATLEAST_Q) {
                input.textCursorDrawable = ContextCompat.getDrawable(
                    context, R.drawable.elyra_drawer_search_cursor)
            }
        }
        // The bottom drawer bar shows a persistent placeholder when empty/unfocused
        // so the inactive row reads as a real search field, not a blank rectangle.
        if (bottomAligned) {
            input.hint = context.getString(R.string.elyra_drawer_search_hint)
        }

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
            if (bottomAligned && !Utilities.isDarkTheme(context)) {
                imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.elyra_drawer_icon_foreground))
            }
            setOnClickListener {
                hideColorPanel(clearFilter = false)
                if (input.text.isNullOrEmpty()) {
                    selectedColorBucket = null
                    clearLocalQuery(keepFocus = true)
                } else {
                    clearLocalQuery(keepFocus = true)
                    if (selectedColorBucket != null) refreshColorSearchResults()
                }
                rebuildColorPanel()
                updateColorButtonVisibility()
            }
        }

        colorPanel = createColorDotsPanel()
        colorPopup = PopupWindow(
            colorPanel,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true,
        ).apply {
            isOutsideTouchable = true
            isClippingEnabled = true
            elevation = resources.getDimension(R.dimen.elyra_color_popup_elevation)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setTouchInterceptor { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    hideColorPanel(clearFilter = false)
                    true
                } else {
                    false
                }
            }
            setOnDismissListener {
                colorPanelVisible = false
                colorPanel.alpha = 1f
                colorPanel.scaleX = 1f
                colorPanel.scaleY = 1f
                colorPanel.translationY = 0f
                updateColorButtonVisibility()
                updateDrawerVisualState()
            }
        }

        with(colorButton) {
            isVisible = false
            setOnClickListener {
                if (colorPanelVisible) hideColorPanel(clearFilter = false) else showColorPanel()
            }
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
                val resId = if (bottomAligned) {
                    R.drawable.ic_qsb_search
                } else if (shouldShowIcons) {
                    iconRes
                } else {
                    R.drawable.ic_qsb_search
                }
                val isThemed = themed || resId == R.drawable.ic_qsb_search
                val method = if (shouldShowIcons) searchProvider.themingMethod else ThemingMethod.TINT

                setThemedIconResource(
                    resId = resId,
                    themed = isThemed,
                    method = method,
                )
                if (bottomAligned && !Utilities.isDarkTheme(context)) {
                    imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.elyra_drawer_icon_foreground))
                }

                setOnClickListener {
                    if (bottomAligned) {
                        input.requestFocus()
                        input.showKeyboard()
                    } else {
                        val launcher = context.launcher
                        launcher.lifecycleScope.launch {
                            searchProvider.launch(launcher)
                        }
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
                if (bottomAligned) {
                    input.setHint(R.string.elyra_drawer_search_hint)
                    setBackgroundVisibility(true, 1f)
                    clearSupplementalHint()
                    animatePadding(currentPaddingLeft, currentPaddingRight)
                } else {
                    if (prefs2.searchAlgorithm.firstBlocking() != LawnchairSearchAlgorithm.APP_SEARCH) {
                        input.setHint(R.string.all_apps_device_search_hint)
                    } else {
                        input.setHint(R.string.all_apps_search_bar_hint)
                    }
                    setBackgroundVisibility(false, 0f)
                    animateHintVisibility(true)
                    animatePadding(currentPaddingLeft / 2, currentPaddingRight / 2)
                }

                if (input.text.toString().isEmpty()) {
                    searchAlgorithm?.doZeroStateSearch(this)
                }

                hideColorPanel(clearFilter = false, animate = false)
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
                // Keep a visible placeholder in the bottom drawer bar when empty, so
                // the inactive row never looks like a blank rectangle.
                input.hint = if (bottomAligned) context.getString(R.string.elyra_drawer_search_hint) else ""
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
                if (bottomAligned) clearSupplementalHint() else updateHint()
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
            // The top search bar aligns its content with the icon grid; the bottom
            // drawer bar is a centered pill, so it uses a clean symmetric side inset
            // instead. This keeps the bar a predictable width with both rounded ends
            // fully visible rather than tied to grid-alignment math.
            val padding = if (bottomAligned) 0 else dp.getAllAppsIconStartMargin(context)
            initialPaddingLeft = padding
            initialPaddingRight = padding
            setPadding(padding, paddingTop, padding, paddingBottom)
            val rowPadding = if (bottomAligned) {
                resources.getDimensionPixelSize(R.dimen.elyra_bottom_search_side_padding)
            } else {
                0
            }
            bottomControls.setPaddingRelative(rowPadding, 0, rowPadding, 0)
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
        if (bottomAligned) {
            clearSupplementalHint()
            return
        }
        focusedResultTitle = title?.toString().orEmpty()
        updateHint()
    }

    override fun refreshResults() {
        onAppsUpdated()
    }

    private fun clearSupplementalHint() {
        focusedResultTitle = ""
        hint.animate().cancel()
        hint.text = ""
        hint.isVisible = false
    }

    private fun updateHint() {
        if (bottomAligned) {
            clearSupplementalHint()
            return
        }
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
        if (::colorPopup.isInitialized && colorPopup.isShowing) {
            hideColorPanel(clearFilter = false, animate = false)
        }
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
        // The bottom drawer always owns a local installed-app session. Provider and
        // web algorithms remain available to other search surfaces, but are never
        // selected by normal drawer search.
        val localAppsOnly = bottomAligned && ElyraBottomSearch.localOnlyDrawerSearchEnabled()
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
        searchAlgorithm?.cancel(true)
        clearSupplementalHint()
        input.hideKeyboard()
        searchBarController.reset()
        selectedColorBucket = null
        rebuildColorPanel()
        updateColorButtonVisibility()
        updateDrawerVisualState()
    }

    private fun clearLocalQuery(keepFocus: Boolean) {
        searchAlgorithm?.cancel(true)
        input.reset()
        clearSearchResult()
        clearSupplementalHint()
        input.hint = context.getString(R.string.elyra_drawer_search_hint)
        if (!keepFocus) {
            input.hideKeyboard()
            input.clearFocus()
        }
        updateActionButtonVisibility()
        updateDrawerVisualState()
    }

    override fun canHandleElyraDrawerSearchBack(): Boolean =
        bottomAligned && (input.hasFocus() || !input.text.isNullOrEmpty() || appsView.isSearching)

    override fun handleElyraDrawerSearchBack(): Boolean {
        if (!canHandleElyraDrawerSearchBack()) return false
        resetSearch()
        return true
    }

    override fun canHandleElyraColorPanelBack(): Boolean = colorPanelVisible

    override fun handleElyraColorPanelBack(): Boolean {
        if (!colorPanelVisible) return false
        hideColorPanel(clearFilter = false)
        return true
    }

    override fun onElyraDrawerModeChanged(categoryMode: Boolean) {
        if (categoryMode && colorPanelVisible) {
            hideColorPanel(clearFilter = false)
        }
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
        val currentQuery = input.text?.toString().orEmpty()
        if (query != currentQuery) return
        if (!ElyraBottomSearch.shouldAcceptDrawerResult(
                query, currentQuery, selectedColorBucket != null)) {
            clearSearchResult()
            return
        }
        if (items != null) {
            apps.setSearchResults(items)
            notifyResultChanged()
            appsView.setSearchResults(items)
        }
    }

    override fun clearSearchResult() {
        if (bottomAligned) clearSupplementalHint()
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
            hideColorPanel(clearFilter = false, animate = false)
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
            // ActivityAllAppsContainerView owns system side insets. This child owns
            // only the row's visual padding, avoiding duplicate inset application.
            leftMargin = 0
            rightMargin = 0
            bottomMargin = if (isInvisible) 0 else bottomInset + bottomSearchInsetMargin
        }
        updateBottomContentInset()
        updateColorButtonVisibility()
        requestLayout()
        return true
    }

    private fun updateBottomContentInset() {
        if (!::appsView.isInitialized || !::bottomControls.isInitialized) return
        val bottomInset = (layoutParams as? RelativeLayout.LayoutParams)
            ?.bottomMargin
            ?.coerceAtLeast(0)
            ?: latestInsets.bottom.coerceAtLeast(0)
        val controlsHeight = if (isInvisible) 0 else bottomControls.measuredHeight
        appsView.setElyraBottomControlsLayout(controlsHeight, bottomInset)
        appsView.refreshElyraBottomContentInsets()
    }

    private fun updateActionButtonVisibility() {
        actionButton.isVisible = !input.text.isNullOrEmpty() || selectedColorBucket != null
    }

    private fun updateColorButtonVisibility() {
        if (!::colorButton.isInitialized || !::colorPanel.isInitialized) return
        val imeActive = latestImeVisible && input.hasFocus()
        searchField.isVisible = true
        searchIcon.isVisible = true
        searchActions.isVisible = true
        colorButton.isVisible = colorSearchEnabled && !imeActive
        colorButton.isSelected = colorPanelVisible
        colorButton.isActivated = selectedColorBucket != null
        colorButton.alpha = 1f
        if (colorSearchEnabled && ::micIcon.isInitialized && ::lensIcon.isInitialized) {
            micIcon.isVisible = false
            lensIcon.isVisible = false
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
        if (!colorSearchEnabled || latestImeVisible && input.hasFocus() || colorPopup.isShowing) return
        input.clearFocus()
        rebuildColorPanel()
        val sideMargin = resources.getDimensionPixelSize(R.dimen.elyra_bottom_search_side_padding)
        val availableWidth = (if (width > 0) width else resources.displayMetrics.widthPixels) -
            sideMargin * 2
        val panelPadding = resources.getDimensionPixelSize(R.dimen.elyra_color_popup_padding)
        val cellSize = resources.getDimensionPixelSize(R.dimen.elyra_color_chip_cell_size)
        val columns = ElyraColorPickerLayout.columnCount(
            availableWidth,
            panelPadding,
            cellSize,
            ElyraAppColorBucket.entries.size + 1,
        )
        colorPanelRow.columnCount = columns
        colorPanelRow.rowCount = ElyraColorPickerLayout.rowCount(
            ElyraAppColorBucket.entries.size + 1,
            columns,
        )
        val popupWidth = ElyraColorPickerLayout.popupWidth(columns, panelPadding, cellSize)
        colorPanel.measure(
            View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        colorPopup.width = popupWidth
        colorPopup.height = colorPanel.measuredHeight
        val gap = resources.getDimensionPixelSize(R.dimen.elyra_color_popup_anchor_gap)
        colorPanelVisible = true
        colorPanel.pivotX = popupWidth.toFloat()
        colorPanel.pivotY = colorPanel.measuredHeight.toFloat()
        colorPopup.showAsDropDown(
            colorButton,
            colorButton.width - popupWidth,
            -(colorButton.height + colorPanel.measuredHeight + gap),
        )
        colorPanel.alpha = 0f
        colorPanel.scaleX = 0.92f
        colorPanel.scaleY = 0.92f
        colorPanel.translationY = gap / 2f
        colorPanel.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(colorAnimationDuration(190L))
            .setInterpolator(FastOutSlowInInterpolator())
            .start()
        updateBottomContentInset()
        updateColorButtonVisibility()
        updateDrawerVisualState()
    }

    private fun hideColorPanel(clearFilter: Boolean, animate: Boolean = true) {
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
        updateBottomContentInset()
        updateColorButtonVisibility()
        updateDrawerVisualState()
        if (!::colorPopup.isInitialized || !colorPopup.isShowing) return
        if (!animate) {
            colorPopup.dismiss()
            return
        }
        val gap = resources.getDimensionPixelSize(R.dimen.elyra_color_popup_anchor_gap)
        colorPanel.animate().cancel()
        colorPanel.animate()
            .alpha(0f)
            .scaleX(0.92f)
            .scaleY(0.92f)
            .translationY(gap / 2f)
            .setDuration(colorAnimationDuration(170L))
            .setInterpolator(FastOutSlowInInterpolator())
            .withEndAction {
                if (colorPopup.isShowing) colorPopup.dismiss()
            }
            .start()
    }

    private fun updateDrawerVisualState() {
        if (::appsView.isInitialized) {
            appsView.updateElyraDrawerVisualState()
        }
    }

    private fun colorAnimationDuration(duration: Long): Long =
        if (ValueAnimator.areAnimatorsEnabled()) duration else 0L

    private fun createColorDotsPanel(): LinearLayout {
        colorPanelRow = GridLayout(context).apply {
            columnCount = 4
            rowCount = 4
            orientation = GridLayout.HORIZONTAL
            alignmentMode = GridLayout.ALIGN_BOUNDS
            setClipChildren(false)
            setClipToPadding(false)
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isFocusable = true
            isFocusableInTouchMode = true
            setClipChildren(false)
            setClipToPadding(false)
            val horizontal = resources.getDimensionPixelSize(R.dimen.elyra_color_popup_padding)
            val vertical = resources.getDimensionPixelSize(R.dimen.elyra_color_popup_padding)
            setPadding(horizontal, vertical, horizontal, vertical)
            background = roundedPanelBackground()
            addView(colorPanelRow, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { gravity = Gravity.CENTER })
        }.also { rebuildColorPanel() }
    }

    private fun rebuildColorPanel() {
        if (!::colorPanelRow.isInitialized) return
        colorPanelRow.removeAllViews()
        val orderedBuckets = listOf(ElyraAppColorBucket.Multicolor) +
            ElyraAppColorBucket.entries.filterNot { it == ElyraAppColorBucket.Multicolor }
        orderedBuckets.forEach { bucket ->
            colorPanelRow.addView(createColorDot(bucket, context.getString(bucket.labelRes)))
        }
        colorPanelRow.addView(createColorDot(null, context.getString(R.string.elyra_color_filter_clear)))
    }

    private fun createColorDot(bucket: ElyraAppColorBucket?, label: String): FrameLayout {
        val selected = selectedColorBucket == bucket
        val cellSize = resources.getDimensionPixelSize(R.dimen.elyra_color_chip_cell_size)
        val chipSize = resources.getDimensionPixelSize(R.dimen.elyra_color_dot_size)
        val chip = TextView(context).apply {
            // "×" (multiplication sign) reads universally as clear/reset; the
            // colored swatches carry no glyph so the color is the whole affordance.
            text = if (bucket == null) "×" else ""
            gravity = Gravity.CENTER
            textSize = 13f
            setSingleLine(true)
            setTextColor(if (Utilities.isDarkTheme(context)) {
                Themes.getAttrColor(context, android.R.attr.textColorSecondary)
            } else {
                ContextCompat.getColor(context, R.color.elyra_drawer_text_secondary)
            })
            background = colorDotBackground(bucket, selected)
            isClickable = false
            layoutParams = FrameLayout.LayoutParams(chipSize, chipSize, Gravity.CENTER)
        }
        return FrameLayout(context).apply {
            isClickable = true
            isFocusable = true
            contentDescription = label
            foreground = ContextCompat.getDrawable(context, R.drawable.elyra_color_chip_ripple)
            addView(chip)
            layoutParams = GridLayout.LayoutParams().apply {
                width = cellSize
                height = cellSize
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

        val fill: Drawable = if (bucket == ElyraAppColorBucket.Multicolor) {
            requireNotNull(ContextCompat.getDrawable(context, R.drawable.elyra_ic_color_wheel)).mutate()
        } else {
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(bucket.swatchColor)
                setStroke(
                    hairline,
                    Color.argb(40, Color.red(outline), Color.green(outline), Color.blue(outline)),
                )
            }
        }
        if (!selected) return fill

        // Selected: an accent highlight ring around the swatch with a small gap, so
        // the active color is unmistakable without recoloring the swatch itself.
        val ringWidth = resources.getDimensionPixelSize(R.dimen.elyra_color_dot_ring)
        val gap = resources.getDimensionPixelSize(R.dimen.elyra_color_dot_ring_gap)
        val ring = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.TRANSPARENT)
            setStroke(ringWidth, if (Utilities.isDarkTheme(context)) {
                Themes.getAttrColor(context, android.R.attr.textColorPrimary)
            } else {
                ContextCompat.getColor(context, R.color.elyra_drawer_text_primary)
            })
        }
        val inset = ringWidth + gap
        val insetFill = InsetDrawable(fill, inset, inset, inset, inset)
        return LayerDrawable(arrayOf(ring, insetFill))
    }

    private fun roundedPanelBackground(): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = resources.getDimensionPixelSize(R.dimen.elyra_radius_large).toFloat()
        setColor(if (Utilities.isDarkTheme(context)) {
            ContextCompat.getColor(context, R.color.elyra_drawer_control_surface)
        } else {
            ContextCompat.getColor(context, R.color.elyra_drawer_popup_surface)
        })
        setStroke(
            resources.getDimensionPixelSize(R.dimen.elyra_color_dot_hairline),
            ContextCompat.getColor(context, R.color.elyra_drawer_surface_stroke),
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        // The vertical offset lifts the top-anchored bar into the header; it must
        // not be applied when the bar is anchored to the bottom of the drawer. The
        // RelativeLayout.LayoutParams check keeps the taskbar/floating case upstream.
        val bottomAnchored = bottomAligned && layoutParams is RelativeLayout.LayoutParams
        if (bottomAnchored && ::appsView.isInitialized && ::bottomControls.isInitialized) {
            updateBottomContentInset()
        }
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
