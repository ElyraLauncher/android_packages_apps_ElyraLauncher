package app.lawnchair.allapps

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import app.lawnchair.data.folder.model.FolderOrderUtils
import app.lawnchair.data.folder.model.FolderViewModel
import app.lawnchair.launcher
import app.lawnchair.preferences.PreferenceManager
import app.lawnchair.preferences2.PreferenceManager2
import app.lawnchair.util.categorizeAppsWithSystemAndGoogle
import com.android.launcher3.InvariantDeviceProfile.OnIDPChangeListener
import com.android.launcher3.allapps.AllAppsStore
import com.android.launcher3.allapps.AlphabeticalAppsList
import com.android.launcher3.allapps.BaseAllAppsAdapter.AdapterItem
import com.android.launcher3.allapps.PrivateProfileManager
import com.android.launcher3.allapps.WorkProfileManager
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.views.ActivityContext
import com.elyra.launcher.drawer.ElyraAppCategory
import com.elyra.launcher.drawer.ElyraCategoryCardModel
import com.elyra.launcher.drawer.ElyraDrawer
import com.elyra.launcher.drawer.ElyraDrawerSuggestions
import com.patrykmichalik.opto.core.onEach
import java.util.function.Predicate

@Suppress("SYNTHETIC_PROPERTY_WITHOUT_JAVA_ORIGIN")
class LawnchairAlphabeticalAppsList<T>(
    private val context: T,
    private val appsStore: AllAppsStore<T>,
    workProfileManager: WorkProfileManager?,
    privateProfileManager: PrivateProfileManager?,
) : AlphabeticalAppsList<T>(context, appsStore, workProfileManager, privateProfileManager),
    OnIDPChangeListener
    where T : Context, T : ActivityContext {

    private var hiddenApps: Set<String> = setOf()
    private val prefs2 = PreferenceManager2.getInstance(context)
    private val prefs = PreferenceManager.getInstance(context)

    // Elyra Stage 6 drawer flags, read once when the list is built (a toggle takes
    // effect on the next drawer rebuild). When both are off, the list is built
    // exactly as upstream.
    private val elyraCategories = ElyraDrawer.categoriesEnabled(context)
    private val elyraSuggestions = ElyraDrawer.suggestionsEnabled(context)

    private val viewModel: FolderViewModel by (context as ComponentActivity).viewModels()
    private var folderList = mutableListOf<FolderInfo>()
    private val filteredList = mutableListOf<AppInfo>()
    private var elyraCategoryCardsMode = false
    private var elyraSelectedCategory: ElyraAppCategory? = null

    private val folderOrder = FolderOrderUtils.stringToIntList(prefs.drawerListOrder.get())

    init {
        context.launcher.deviceProfile.inv.addOnChangeListener(this)
        try {
            prefs2.hiddenApps.onEach(launchIn = context.launcher.lifecycleScope) {
                hiddenApps = it
                onAppsUpdated()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to initialize hidden apps", t)
        }
        observeFolders()
    }

    private fun observeFolders() {
        viewModel.foldersLiveData.observe(context as LifecycleOwner) { folders ->
            folderList = folders
                .sortedBy { folderOrder.indexOf(it.id) }
                .toMutableList()
            updateAdapterItems()
        }
    }

    override fun updateItemFilter(itemFilter: Predicate<ItemInfo>?) {
        mItemFilter = Predicate { info ->
            require(info is AppInfo) { "`info` must be an instance of `AppInfo`." }
            val componentKey = info.toComponentKey().toString()
            (itemFilter?.test(info) != false) && !hiddenApps.contains(componentKey)
        }
        onAppsUpdated()
    }

    override fun addAppsWithSections(appList: List<AppInfo?>?, startPosition: Int): Int {
        if (appList.isNullOrEmpty()) return startPosition
        val drawerListDefault = prefs.drawerList.get()
        filteredList.clear()
        var position = startPosition

        // Show app drawer folders only on main profile, to prevent state complexity.
        if (isWorkOrPrivateSpace(appList)) return super.addAppsWithSections(appList, position)

        val validApps = appList.mapNotNull { it }

        if (elyraCategories) {
            mAdapterItems.add(AdapterItem.asElyraCategoryTabs(elyraCategoryCardsMode))
            position++

            val cards = ElyraCategoryCardModel.build(validApps, context)
            if (elyraCategoryCardsMode) {
                cards.forEach { card ->
                    mAdapterItems.add(AdapterItem.asElyraCategoryCard(card))
                    position++
                }
                return position
            }

            elyraSelectedCategory?.let { selectedCategory ->
                val selectedCard = cards.firstOrNull { it.category == selectedCategory }
                if (selectedCard != null) {
                    mAdapterItems.add(AdapterItem.asElyraCategoryHeader(selectedCard.label))
                    position++
                    return super.addAppsWithSections(selectedCard.apps, position)
                }
                elyraSelectedCategory = null
            }
        }

        // Elyra drawer suggestions stay local-first and prepend real app entries to
        // the normal All tab only. Category cards never emit FolderInfo rows and
        // never mutate workspace data; workspace smart folders are deferred.
        if (elyraSuggestions) {
            ElyraDrawerSuggestions.suggest(context, validApps).forEach { app ->
                mAdapterItems.add(AdapterItem.asApp(app))
                position++
            }
        }

        if (!elyraCategories && !drawerListDefault) {
            val validApps = appList.mapNotNull { it }
            val finalCategorizedApps = categorizeAppsWithSystemAndGoogle(validApps, context)

            finalCategorizedApps.forEach { (category, apps) ->
                if (apps.size == 1) {
                    mAdapterItems.add(AdapterItem.asApp(apps.first()))
                } else {
                    val folderInfo = FolderInfo().apply {
                        title = category
                        apps.forEach { add(it) }
                    }
                    mAdapterItems.add(AdapterItem.asFolder(folderInfo))
                }
                position++
            }
        } else {
            folderList.forEach { folder ->
                if (folder.getContents().size > 1) {
                    val folderInfo = FolderInfo()
                    folderInfo.title = folder.title
                    mAdapterItems.add(AdapterItem.asFolder(folderInfo))
                    folder.getContents().forEach { app ->
                        (appsStore.getApp(app.componentKey) as? AppInfo)?.let {
                            folderInfo.add(it)
                            if (prefs.folderApps.get()) filteredList.add(it)
                        }
                    }
                }
                position++
            }
            val remainingApps = appList.filterNot { app -> filteredList.contains(app) && prefs.folderApps.get() }
            position = super.addAppsWithSections(remainingApps, position)
        }

        return position
    }

    override fun isElyraCategoryTabsEnabled(): Boolean = elyraCategories

    override fun isElyraCategoryCardsMode(): Boolean = elyraCategories && elyraCategoryCardsMode

    override fun showElyraAllApps() {
        if (!elyraCategories) return
        elyraCategoryCardsMode = false
        elyraSelectedCategory = null
        updateAdapterItems()
    }

    override fun showElyraCategoryCards() {
        if (!elyraCategories) return
        elyraCategoryCardsMode = true
        elyraSelectedCategory = null
        updateAdapterItems()
    }

    override fun selectElyraCategory(category: ElyraAppCategory) {
        if (!elyraCategories) return
        elyraCategoryCardsMode = false
        elyraSelectedCategory = category
        updateAdapterItems()
    }

    override fun onIdpChanged(modelPropertiesChanged: Boolean) {
        onAppsUpdated()
    }
}
