package de.westnordost.streetcomplete.screens.settings.quest_selection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ChipDefaults
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import de.westnordost.streetcomplete.data.user.achievements.EditTypeAchievement
import de.westnordost.streetcomplete.resources.*
import de.westnordost.streetcomplete.ui.common.ClearIcon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterialApi::class, ExperimentalLayoutApi::class)
@Composable
fun QuestInlineFilterPanel(
    search: String,
    onSearchChange: (String) -> Unit,
    filters: QuestFilters,
    onFiltersChanged: (QuestFilters) -> Unit,
    allQuests: List<QuestSelection>,
    filteredCount: Int,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    Column(modifier = modifier.background(MaterialTheme.colors.surface)) {
        // Persistent search bar
        OutlinedTextField(
            value = search,
            onValueChange = onSearchChange,
            placeholder = { Text(stringResource(Res.string.quest_inline_search_hint, allQuests.size)) },
            leadingIcon = { Icon(painterResource(Res.drawable.ic_search_24), null) },
            trailingIcon = if (search.isNotEmpty()) {
                { IconButton(onClick = { onSearchChange("") }) { ClearIcon() } }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        )

        // Active filter chips row (always visible when any filter active)
        if (!filters.isDefault || search.isNotBlank()) {
            ActiveFiltersRow(
                filters = filters,
                onFiltersChanged = onFiltersChanged,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (!expanded) {
            // Collapsed: showing count + expand button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(Res.string.quest_inline_showing, filteredCount, allQuests.size).uppercase(),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onToggleExpanded) {
                    Text(stringResource(Res.string.quest_inline_expand))
                    Icon(painterResource(Res.drawable.ic_arrow_drop_down_24), null, Modifier.padding(start = 4.dp))
                }
            }
            Divider()
        } else {
            // Expanded panel
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(Res.string.quest_filter_quick_filters),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    // INLINE PANEL pill
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colors.secondary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = stringResource(Res.string.quest_inline_panel),
                            style = MaterialTheme.typography.overline,
                            color = MaterialTheme.colors.secondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Segmented control Enabled / Disabled / All
                StatusSegmentedControl(
                    filters = filters,
                    onFiltersChanged = onFiltersChanged,
                    allQuests = allQuests
                )

                // Topics scroll row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(Res.string.quest_filter_topics_scroll),
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        val newSet = if (filters.achievements.size == EditTypeAchievement.entries.size) emptySet() else EditTypeAchievement.entries.toSet()
                        onFiltersChanged(filters.copy(achievements = newSet))
                    }) {
                        Text(stringResource(Res.string.quest_inline_toggle_all))
                    }
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(EditTypeAchievement.entries.toList()) { ach ->
                        val selected = ach in filters.achievements
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val newSet = if (selected) filters.achievements - ach else filters.achievements + ach
                                onFiltersChanged(filters.copy(achievements = newSet))
                            },
                            leadingIcon = {
                                Icon(
                                    painterResource(ach.iconRes),
                                    null,
                                    modifier = Modifier.size(18.dp),
                                    tint = androidx.compose.ui.graphics.Color.Unspecified
                                )
                            },
                            colors = ChipDefaults.filterChipColors(
                                selectedBackgroundColor = MaterialTheme.colors.primary,
                                selectedContentColor = MaterialTheme.colors.onPrimary
                            )
                        ) {
                            Text(stringResource(ach.titleRes))
                        }
                    }
                }

                // Availability
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = filters.disabledInCurrentCountry == true,
                        onClick = {
                            onFiltersChanged(filters.copy(disabledInCurrentCountry = if (filters.disabledInCurrentCountry == true) null else true))
                        },
                        leadingIcon = { if (filters.disabledInCurrentCountry == true) Icon(painterResource(Res.drawable.ic_check_circle_24), null) },
                    ) { Text(stringResource(Res.string.quest_filter_disabled_in_country)) }
                    FilterChip(
                        selected = filters.disabledByDefault == true,
                        onClick = {
                            onFiltersChanged(filters.copy(disabledByDefault = if (filters.disabledByDefault == true) null else true))
                        },
                        leadingIcon = { if (filters.disabledByDefault == true) Icon(painterResource(Res.drawable.ic_check_circle_24), null) },
                    ) { Text(stringResource(Res.string.quest_filter_disabled_by_default)) }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(Res.string.quest_inline_showing, filteredCount, allQuests.size),
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { onFiltersChanged(QuestFilters()) }) {
                        Text(stringResource(Res.string.quest_filter_clear))
                    }
                    Button(
                        onClick = onApply,
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(Res.string.quest_inline_apply), color = MaterialTheme.colors.onSecondary)
                    }
                }
            }
            Divider()
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ActiveFiltersRow(
    filters: QuestFilters,
    onFiltersChanged: (QuestFilters) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        // Enabled only / Disabled only
        if (filters.selected != null) {
            item("status") {
                val label = if (filters.selected == true) stringResource(Res.string.quest_filter_status_enabled) else stringResource(Res.string.quest_filter_status_disabled)
                FilterChip(
                    selected = true,
                    onClick = { onFiltersChanged(filters.copy(selected = null)) },
                    leadingIcon = { Icon(painterResource(Res.drawable.ic_check_circle_24), null) },
                    colors = ChipDefaults.filterChipColors(selectedBackgroundColor = MaterialTheme.colors.primary, selectedContentColor = MaterialTheme.colors.onPrimary)
                ) { Text(label) }
            }
        }
        // Achievements — show with achievement icon as in design (Bicyclist bicycle, Rare star)
        for (ach in filters.achievements) {
            item(ach.id) {
                val label = stringResource(ach.titleRes)
                FilterChip(
                    selected = true,
                    onClick = {
                        onFiltersChanged(filters.copy(achievements = filters.achievements - ach))
                    },
                    leadingIcon = { Icon(painterResource(ach.iconRes), null, modifier = Modifier.size(18.dp), tint = androidx.compose.ui.graphics.Color.Unspecified) },
                    colors = ChipDefaults.filterChipColors(selectedBackgroundColor = MaterialTheme.colors.primary, selectedContentColor = MaterialTheme.colors.onPrimary)
                ) { Text(label) }
            }
        }
        if (filters.disabledInCurrentCountry == true) {
            item("inCountry") {
                FilterChip(selected = true, onClick = { onFiltersChanged(filters.copy(disabledInCurrentCountry = null)) },
                    leadingIcon = { Icon(painterResource(Res.drawable.ic_check_circle_24), null) }) {
                    Text(stringResource(Res.string.quest_filter_disabled_in_country))
                }
            }
        }
        if (filters.disabledByDefault == true) {
            item("byDefault") {
                FilterChip(selected = true, onClick = { onFiltersChanged(filters.copy(disabledByDefault = null)) },
                    leadingIcon = { Icon(painterResource(Res.drawable.ic_check_circle_24), null) }) {
                    Text(stringResource(Res.string.quest_filter_disabled_by_default))
                }
            }
        }
    }
}

@Composable
private fun StatusSegmentedControl(
    filters: QuestFilters,
    onFiltersChanged: (QuestFilters) -> Unit,
    allQuests: List<QuestSelection>,
) {
    val enabledCount = allQuests.count { it.selected }
    val disabledCount = allQuests.size - enabledCount
    val total = allQuests.size
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.onSurface.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(2.dp)
    ) {
        SegmentButton(
            label = stringResource(Res.string.quest_inline_enabled, enabledCount),
            selected = filters.selected == true,
            onClick = { onFiltersChanged(filters.copy(selected = if (filters.selected == true) null else true)) },
            modifier = Modifier.weight(1f)
        )
        SegmentButton(
            label = stringResource(Res.string.quest_inline_disabled, disabledCount),
            selected = filters.selected == false,
            onClick = { onFiltersChanged(filters.copy(selected = if (filters.selected == false) null else false)) },
            modifier = Modifier.weight(1f)
        )
        SegmentButton(
            label = stringResource(Res.string.quest_inline_all),
            selected = filters.selected == null,
            onClick = { onFiltersChanged(filters.copy(selected = null)) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SegmentButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) MaterialTheme.colors.surface else androidx.compose.ui.graphics.Color.Transparent
    val contentColor = if (selected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(vertical = 8.dp)
    ) {
        TextButton(onClick = onClick) {
            Text(label, color = contentColor, style = MaterialTheme.typography.button)
        }
    }
}

private val EditTypeAchievement.titleRes get() = when (this) {
    EditTypeAchievement.RARE -> Res.string.achievement_rare_title
    EditTypeAchievement.CAR -> Res.string.achievement_car_title
    EditTypeAchievement.VEG -> Res.string.achievement_veg_title
    EditTypeAchievement.PEDESTRIAN -> Res.string.achievement_pedestrian_title
    EditTypeAchievement.BUILDING -> Res.string.achievement_building_title
    EditTypeAchievement.POSTMAN -> Res.string.achievement_postman_title
    EditTypeAchievement.BLIND -> Res.string.achievement_blind_title
    EditTypeAchievement.WHEELCHAIR -> Res.string.achievement_wheelchair_title
    EditTypeAchievement.BICYCLIST -> Res.string.achievement_bicyclist_title
    EditTypeAchievement.CITIZEN -> Res.string.achievement_citizen_title
    EditTypeAchievement.OUTDOORS -> Res.string.achievement_outdoors_title
    EditTypeAchievement.LIFESAVER -> Res.string.achievement_lifesaver_title
}

private val EditTypeAchievement.iconRes get() = when (this) {
    EditTypeAchievement.RARE -> Res.drawable.ic_star_24
    EditTypeAchievement.CAR -> Res.drawable.quest_car
    EditTypeAchievement.VEG -> Res.drawable.quest_restaurant_vegetarian
    EditTypeAchievement.PEDESTRIAN -> Res.drawable.quest_pedestrian
    EditTypeAchievement.BUILDING -> Res.drawable.quest_building
    EditTypeAchievement.POSTMAN -> Res.drawable.achievement_postman
    EditTypeAchievement.BLIND -> Res.drawable.quest_blind
    EditTypeAchievement.WHEELCHAIR -> Res.drawable.achievement_wheelchair
    EditTypeAchievement.BICYCLIST -> Res.drawable.quest_bicycle
    EditTypeAchievement.CITIZEN -> Res.drawable.achievement_citizen
    EditTypeAchievement.OUTDOORS -> Res.drawable.achievement_outdoors
    EditTypeAchievement.LIFESAVER -> Res.drawable.achievement_lifesaver
}
