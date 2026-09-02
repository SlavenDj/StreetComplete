package de.westnordost.streetcomplete.screens.settings.quest_selection

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.westnordost.streetcomplete.data.osm.edits.EditType
import de.westnordost.streetcomplete.data.osm.osmquests.OsmElementQuestType
import de.westnordost.streetcomplete.data.preferences.Preferences
import de.westnordost.streetcomplete.data.presets.EditTypePreset
import de.westnordost.streetcomplete.data.presets.EditTypePresetsSource
import de.westnordost.streetcomplete.data.quest.QuestType
import de.westnordost.streetcomplete.data.quest.QuestTypeRegistry
import de.westnordost.streetcomplete.data.visiblequests.QuestTypeOrderController
import de.westnordost.streetcomplete.data.visiblequests.QuestTypeOrderSource
import de.westnordost.streetcomplete.data.visiblequests.VisibleEditTypeController
import de.westnordost.streetcomplete.data.visiblequests.VisibleEditTypeSource
import de.westnordost.streetcomplete.util.countryboundaries.AllCountries
import de.westnordost.streetcomplete.util.countryboundaries.AllCountriesExcept
import de.westnordost.streetcomplete.util.countryboundaries.CountryBoundaries
import de.westnordost.streetcomplete.util.countryboundaries.NoCountriesExcept
import de.westnordost.streetcomplete.util.ktx.containsAll
import de.westnordost.streetcomplete.util.ktx.containsAny
import de.westnordost.streetcomplete.util.ktx.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.resources.getString

@Stable
abstract class QuestSelectionViewModel : ViewModel() {
    abstract val searchText: StateFlow<String>
    abstract val filteredQuests: StateFlow<List<QuestSelection>>
    abstract val allQuests: StateFlow<List<QuestSelection>>
    abstract val currentCountry: String?
    abstract val selectedEditTypePresetName: StateFlow<String?>
    abstract val questFilters: StateFlow<QuestFilters>
    /** true if any filter or search is active — drag is disabled while filtering */
    abstract val isFiltered: StateFlow<Boolean>
    abstract val inlineExpanded: StateFlow<Boolean>

    abstract fun select(questType: QuestType, selected: Boolean)
    abstract fun order(questType: QuestType, toAfter: QuestType)
    abstract fun unselectAll()
    abstract fun resetAll()
    abstract fun updateSearchText(text: String)
    abstract fun updateFilters(filters: QuestFilters)
    abstract fun clearFilters()
    abstract fun setInlineExpanded(expanded: Boolean)
}

@Stable
class QuestSelectionViewModelImpl(
    private val questTypeRegistry: QuestTypeRegistry,
    private val editTypePresetsSource: EditTypePresetsSource,
    private val visibleEditTypeController: VisibleEditTypeController,
    private val questTypeOrderController: QuestTypeOrderController,
    countryBoundaries: Lazy<CountryBoundaries>,
    private val prefs: Preferences,
) : QuestSelectionViewModel() {

    override val searchText = MutableStateFlow(prefs.questSelectionSearch ?: "")

    private val questTitles = MutableStateFlow<Map<String, String>>(emptyMap())

    // persisted filters — restored on init, saved on every change
    override val questFilters = MutableStateFlow(QuestFilters.decode(prefs.questSelectionFilters))

    override val inlineExpanded = MutableStateFlow(prefs.questSelectionInlineExpanded)

    private val visibleEditTypeListener = object : VisibleEditTypeSource.Listener {
        override fun onVisibilityChanged(editType: EditType, visible: Boolean) {
            quests.update { quests ->
                val result = quests.toMutableList()
                val index = result.indexOfFirst { it.questType == editType }
                if (index != -1) {
                    result[index] = result[index].copy(selected = visible)
                }
                return@update result
            }
        }

        // all/many visibilities have changed - re-init list
        override fun onVisibilitiesChanged() { initQuests() }
    }

    private val questTypeOrderListener = object : QuestTypeOrderSource.Listener {
        override fun onQuestTypeOrderAdded(item: QuestType, toAfter: QuestType) {
            quests.update { quests ->
                val result = quests.toMutableList()
                val itemIndex = result.indexOfFirst { it.questType == item }
                val toAfterIndex = result.indexOfFirst { it.questType == toAfter }

                val questType = result.removeAt(itemIndex)
                result.add(toAfterIndex + if (itemIndex > toAfterIndex) 1 else 0, questType)
                return@update result
            }
        }

        // all/many quest orders have been changed - re-init list
        override fun onQuestTypeOrdersChanged() { initQuests() }
    }

    private val editTypePresetsListener = object : EditTypePresetsSource.Listener {
        override fun onSelectionChanged() { updateSelectedEditTypePresetName() }
        override fun onAdded(preset: EditTypePreset) {}
        override fun onRenamed(preset: EditTypePreset) {}
        override fun onDeleted(presetId: Long) {}
    }

    private val quests = MutableStateFlow<List<QuestSelection>>(emptyList())

    override val allQuests: StateFlow<List<QuestSelection>> = quests

    override val filteredQuests: StateFlow<List<QuestSelection>> =
        combine(quests, searchText, questTitles, questFilters) { quests, searchText, titles, filters ->
            filterQuests(quests, searchText, titles, filters)
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    override val isFiltered: StateFlow<Boolean> =
        combine(searchText, questFilters) { text, filters ->
            text.isNotBlank() || !filters.isDefault
        }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val currentCountryCodes = countryBoundaries.value.getIds(prefs.mapPosition)

    override val selectedEditTypePresetName = MutableStateFlow<String?>(null)

    override val currentCountry: String?
        get() = currentCountryCodes.firstOrNull()

    init {
        initQuests()
        updateSelectedEditTypePresetName()
        loadQuestTitles()
        editTypePresetsSource.addListener(editTypePresetsListener)
        visibleEditTypeController.addListener(visibleEditTypeListener)
        questTypeOrderController.addListener(questTypeOrderListener)
    }

    private fun updateSelectedEditTypePresetName() {
        launch(Dispatchers.IO) {
            selectedEditTypePresetName.value = editTypePresetsSource.selectedEditTypePresetName
        }
    }

    private fun loadQuestTitles() {
        // This method loads titles only once. When the system language changes, the titles
        // are not reloaded automatically since there is no listenable callback from the
        // system for when the language changes
        launch(Default) {
            questTitles.value = questTypeRegistry.associate { it.name to getString(it.title) }
        }
    }

    override fun onCleared() {
        editTypePresetsSource.removeListener(editTypePresetsListener)
        visibleEditTypeController.removeListener(visibleEditTypeListener)
        questTypeOrderController.removeListener(questTypeOrderListener)
    }

    override fun select(questType: QuestType, selected: Boolean) {
        launch(Dispatchers.IO) {
            visibleEditTypeController.setVisibility(questType, selected)
        }
    }

    override fun order(questType: QuestType, toAfter: QuestType) {
        launch(Dispatchers.IO) {
            questTypeOrderController.addOrderItem(questType, toAfter)
        }
    }

    override fun unselectAll() {
        launch(Dispatchers.IO) {
            visibleEditTypeController.setVisibilities(questTypeRegistry.associateWith { false })
        }
    }

    override fun resetAll() {
        launch(Dispatchers.IO) {
            visibleEditTypeController.clearVisibilities(questTypeRegistry)
            questTypeOrderController.clear()
        }
    }

    override fun updateSearchText(text: String) {
        searchText.value = text
        prefs.questSelectionSearch = text.takeIf { it.isNotBlank() }
    }

    override fun updateFilters(filters: QuestFilters) {
        questFilters.value = filters
        prefs.questSelectionFilters = filters.encode()
    }

    override fun clearFilters() = updateFilters(QuestFilters())

    override fun setInlineExpanded(expanded: Boolean) {
        inlineExpanded.value = expanded
        prefs.questSelectionInlineExpanded = expanded
    }

    private fun initQuests() {
        launch(Dispatchers.IO) {
            val sortedQuestTypes = questTypeRegistry.toMutableList()
            questTypeOrderController.sort(sortedQuestTypes)
            quests.value = sortedQuestTypes
                .map { QuestSelection(
                    questType = it,
                    selected = visibleEditTypeController.isVisible(it),
                    enabledInCurrentCountry = isQuestEnabledInCurrentCountry(it)
                ) }
                .toMutableList()
        }
    }

    private fun isQuestEnabledInCurrentCountry(questType: QuestType): Boolean {
        if (questType !is OsmElementQuestType<*>) return true
        return when (val countries = questType.enabledInCountries) {
            is AllCountries -> true
            is AllCountriesExcept -> !countries.exceptions.containsAny(currentCountryCodes)
            is NoCountriesExcept -> countries.exceptions.containsAny(currentCountryCodes)
        }
    }

    private fun filterQuests(
        quests: List<QuestSelection>,
        filter: String,
        titles: Map<String, String>,
        filters: QuestFilters,
    ): List<QuestSelection> {
        val words = filter.takeIf { it.isNotBlank() }?.trim()?.lowercase()?.split(' ') ?: emptyList()
        return quests.filter { qs ->
            // text search: title + class name + wiki link
            val textOk = if (words.isEmpty()) true else {
                val title = titles[qs.questType.name]?.lowercase() ?: ""
                val name = qs.questType.name.lowercase()
                val wiki = qs.questType.wikiLink?.lowercase() ?: ""
                // match if ALL words appear in at least one of title/name/wiki (or combined)
                // to keep it flexible, check combined title+name+wiki contains all words
                val combined = "$title $name $wiki"
                combined.containsAll(words) ||
                    title.containsAll(words) || name.containsAll(words) || wiki.containsAll(words)
            }
            if (!textOk) return@filter false

            // selected / not selected
            if (filters.selected != null && qs.selected != filters.selected) return@filter false

            // achievements
            if (filters.achievements.isNotEmpty()) {
                if (qs.questType.achievements.none { it in filters.achievements }) return@filter false
            }

            // disabled in current country
            if (filters.disabledInCurrentCountry != null) {
                val disabledInCountry = !qs.enabledInCurrentCountry
                if (disabledInCountry != filters.disabledInCurrentCountry) return@filter false
            }

            // disabled by default
            if (filters.disabledByDefault != null) {
                val disabledByDefault = qs.questType.defaultDisabledMessage != null
                if (disabledByDefault != filters.disabledByDefault) return@filter false
            }

            true
        }
    }
}
