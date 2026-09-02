package de.westnordost.streetcomplete.screens.settings.quest_selection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.westnordost.streetcomplete.data.user.achievements.EditTypeAchievement
import de.westnordost.streetcomplete.resources.*
import de.westnordost.streetcomplete.ui.common.dialogs.ScrollableAlertDialog
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
@Composable
fun QuestFilterDialog(
    filters: QuestFilters,
    onDismissRequest: () -> Unit,
    onFiltersChanged: (QuestFilters) -> Unit,
) {
    ScrollableAlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.quest_filter_title)) },
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Status: Enabled / Disabled
                FilterSectionLabel(stringResource(Res.string.quest_filter_status))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QuestFilterChip(
                        label = stringResource(Res.string.quest_filter_status_enabled),
                        selected = filters.selected == true,
                        onClick = {
                            onFiltersChanged(filters.copy(selected = if (filters.selected == true) null else true))
                        }
                    )
                    QuestFilterChip(
                        label = stringResource(Res.string.quest_filter_status_disabled),
                        selected = filters.selected == false,
                        onClick = {
                            onFiltersChanged(filters.copy(selected = if (filters.selected == false) null else false))
                        }
                    )
                }

                // Topics (achievements)
                FilterSectionLabel(stringResource(Res.string.quest_filter_achievements))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (ach in EditTypeAchievement.entries) {
                        val selected = ach in filters.achievements
                        QuestFilterChip(
                            label = stringResource(ach.titleRes),
                            selected = selected,
                            onClick = {
                                val newSet = if (selected) filters.achievements - ach else filters.achievements + ach
                                onFiltersChanged(filters.copy(achievements = newSet))
                            }
                        )
                    }
                }

                // Availability
                FilterSectionLabel(stringResource(Res.string.quest_filter_availability))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // disabled in country: we expose as filter "Disabled in country = true/false"
                    // For simplicity offer two chips: enabled vs disabled handling is ambiguous, so we map to disabledInCurrentCountry filter
                    // Show chip for "Disabled in country" when true
                    QuestFilterChip(
                        label = stringResource(Res.string.quest_filter_disabled_in_country),
                        selected = filters.disabledInCurrentCountry == true,
                        onClick = {
                            onFiltersChanged(filters.copy(disabledInCurrentCountry = if (filters.disabledInCurrentCountry == true) null else true))
                        }
                    )
                    QuestFilterChip(
                        label = stringResource(Res.string.quest_filter_disabled_by_default),
                        selected = filters.disabledByDefault == true,
                        onClick = {
                            onFiltersChanged(filters.copy(disabledByDefault = if (filters.disabledByDefault == true) null else true))
                        }
                    )
                }
            }
        },
        buttonRow = {
            TextButton(onClick = {
                onFiltersChanged(QuestFilters())
            }) {
                Text(stringResource(Res.string.quest_filter_clear))
            }
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.ok))
            }
        }
    )
}

@Composable
private fun FilterSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.caption
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun QuestFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val icon = if (selected) Res.drawable.ic_check_circle_24 else Res.drawable.ic_circle_outline_24
    FilterChip(
        selected = selected,
        onClick = onClick,
        leadingIcon = { Icon(painterResource(icon), null) },
        colors = ChipDefaults.filterChipColors(
            selectedBackgroundColor = MaterialTheme.colors.primary,
            selectedContentColor = MaterialTheme.colors.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colors.onPrimary,
        )
    ) {
        Text(label)
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
