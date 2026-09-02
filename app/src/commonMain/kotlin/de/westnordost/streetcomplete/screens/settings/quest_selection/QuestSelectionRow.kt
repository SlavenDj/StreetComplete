package de.westnordost.streetcomplete.screens.settings.quest_selection

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import de.westnordost.streetcomplete.ui.theme.surfaceContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import de.westnordost.streetcomplete.quests.surface.AddRoadSurface
import de.westnordost.streetcomplete.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Single item in the quest selection list — card style matching Variant C mock */
@Composable
fun QuestSelectionRow(
    item: QuestSelection,
    onToggleSelection: (isSelected: Boolean) -> Unit,
    displayCountry: String,
    modifier: Modifier = Modifier
) {
    val alpha = if (!item.selected) ContentAlpha.disabled else ContentAlpha.high

    Card(
        shape = RoundedCornerShape(12.dp),
        backgroundColor = MaterialTheme.colors.surfaceContainer,
        elevation = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = item.selected,
                onCheckedChange = onToggleSelection,
                enabled = item.isInteractionEnabled,
                modifier = Modifier.alpha(alpha)
            )
            Image(
                painter = painterResource(item.questType.icon),
                contentDescription = item.questType.name,
                modifier = Modifier.size(40.dp).alpha(alpha).padding(start = 4.dp),
            )
            Column(
                modifier = Modifier.padding(start = 12.dp).weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(item.questType.title),
                    modifier = Modifier.alpha(alpha),
                    style = MaterialTheme.typography.body1,
                )
                if (!item.enabledInCurrentCountry) {
                    DisabledHint(stringResource(Res.string.questList_disabled_in_country, displayCountry))
                } else if (item.questType.defaultDisabledMessage != null) {
                    DisabledHint(stringResource(Res.string.questList_disabled_by_default))
                } else {
                    // subtle XP placeholder like mock — keep row height consistent
                    Text(
                        text = "",
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.alpha(0f)
                    )
                }
            }
            if (item.isInteractionEnabled) {
                Icon(
                    painterResource(Res.drawable.ic_drag_vertical_24),
                    contentDescription = "Reorder",
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp).alpha(alpha)
                )
            } else {
                Spacer(Modifier.width(18.dp))
            }
        }
    }
}

@Composable
private fun DisabledHint(text: String) {
    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
        Text(
            text = text,
            style = MaterialTheme.typography.body2,
            fontStyle = FontStyle.Italic,
        )
    }
}

@Preview
@Composable
private fun QuestSelectionRowPreview() {
    var selected by remember { mutableStateOf(true) }

    QuestSelectionRow(
        item = QuestSelection(AddRoadSurface(), selected, false),
        onToggleSelection = { selected = !selected },
        displayCountry = "Atlantis",
    )
}
