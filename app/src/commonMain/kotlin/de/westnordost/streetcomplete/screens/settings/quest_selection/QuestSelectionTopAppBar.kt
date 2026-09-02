package de.westnordost.streetcomplete.screens.settings.quest_selection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import de.westnordost.streetcomplete.resources.*
import de.westnordost.streetcomplete.ui.common.BackIcon
import de.westnordost.streetcomplete.ui.common.DropdownMenuItem
import de.westnordost.streetcomplete.ui.common.FilterIcon
import de.westnordost.streetcomplete.ui.common.MoreIcon
import de.westnordost.streetcomplete.ui.common.SearchIcon
import de.westnordost.streetcomplete.ui.common.TopAppBarWithContent
import de.westnordost.streetcomplete.ui.common.dialogs.ConfirmationDialog
import org.jetbrains.compose.resources.stringResource

/** Top bar for the quest selection screen — search is now persistent inline below */
@Composable
fun QuestSelectionTopAppBar(
    currentPresetName: String,
    onClickBack: () -> Unit,
    onUnselectAll: () -> Unit,
    onReset: () -> Unit,
    filtersCount: Int = 0,
    onClickSearch: () -> Unit = {},
    onClickFilter: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    TopAppBarWithContent(
        title = { QuestSelectionTitle(currentPresetName) },
        modifier = modifier,
        navigationIcon = { IconButton(onClick = onClickBack) { BackIcon() } },
        actions = {
            QuestSelectionTopBarActions(
                onUnselectAll = onUnselectAll,
                onReset = onReset,
                onClickSearch = onClickSearch,
                onClickFilter = onClickFilter,
                filtersCount = filtersCount
            )
        },
    ) {}
}

@Composable
private fun QuestSelectionTitle(currentPresetName: String) {
    Column {
        Text(
            text = stringResource(Res.string.pref_title_quests2),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(Res.string.pref_subtitle_quests_preset_name, currentPresetName),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.body1,
        )
    }
}

@Composable
private fun QuestSelectionTopBarActions(
    onUnselectAll: () -> Unit,
    onReset: () -> Unit,
    onClickSearch: () -> Unit,
    onClickFilter: () -> Unit = {},
    filtersCount: Int = 0,
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var showDeselectAllDialog by remember { mutableStateOf(false) }
    var showActionsDropdown by remember { mutableStateOf(false) }

    IconButton(onClick = onClickSearch) { SearchIcon() }
    androidx.compose.foundation.layout.Box {
        IconButton(onClick = onClickFilter) { FilterIcon() }
        if (filtersCount > 0) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .padding(top = 2.dp, end = 2.dp)
                    .align(androidx.compose.ui.Alignment.TopEnd)
            ) {
                androidx.compose.material.Badge(
                    backgroundColor = MaterialTheme.colors.secondary
                ) { Text(filtersCount.toString()) }
            }
        }
    }
    Box {
        IconButton(onClick = { showActionsDropdown = true }) { MoreIcon() }
        DropdownMenu(
            expanded = showActionsDropdown,
            onDismissRequest = { showActionsDropdown = false },
        ) {
            DropdownMenuItem(onClick = {
                showResetDialog = true
                showActionsDropdown = false
            }) {
                Text(stringResource(Res.string.action_reset))
            }
            DropdownMenuItem(onClick = {
                showDeselectAllDialog = true
                showActionsDropdown = false
            }) {
                Text(stringResource(Res.string.action_deselect_all))
            }
        }
    }

    if (showDeselectAllDialog) {
        ConfirmationDialog(
            onDismissRequest = { showDeselectAllDialog = false },
            onConfirmed = onUnselectAll,
            text = { Text(stringResource(Res.string.pref_quests_deselect_all)) },
        )
    }

    if (showResetDialog) {
        ConfirmationDialog(
            onDismissRequest = { showResetDialog = false },
            onConfirmed = onReset,
            text = { Text(stringResource(Res.string.pref_quests_reset)) },
        )
    }
}

@Preview
@Composable
private fun PreviewQuestSelectionTopBar() {
    QuestSelectionTopAppBar(
        currentPresetName = "Test",
        onClickBack = {},
        onUnselectAll = {},
        onReset = {},
        filtersCount = 3,
        onClickSearch = {},
        onClickFilter = {},
    )
}
