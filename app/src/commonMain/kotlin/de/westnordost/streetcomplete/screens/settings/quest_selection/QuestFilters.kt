package de.westnordost.streetcomplete.screens.settings.quest_selection

import de.westnordost.streetcomplete.data.user.achievements.EditTypeAchievement

/** Filters for the quest selection screen — purely presentational, global order stays flat */
data class QuestFilters(
    /** null = show all, true = only selected, false = only not selected */
    val selected: Boolean? = null,
    /** empty = no achievement filter, otherwise quest must have at least one of these */
    val achievements: Set<EditTypeAchievement> = emptySet(),
    /** null = show all, true = only disabled in current country, false = only enabled in current country */
    val disabledInCurrentCountry: Boolean? = null,
    /** null = show all, true = only disabled by default, false = only enabled by default */
    val disabledByDefault: Boolean? = null,
) {
    val isDefault: Boolean get() =
        selected == null && achievements.isEmpty() && disabledInCurrentCountry == null && disabledByDefault == null

    fun countActive(): Int {
        var c = 0
        if (selected != null) c++
        if (achievements.isNotEmpty()) c++
        if (disabledInCurrentCountry != null) c++
        if (disabledByDefault != null) c++
        return c
    }

    /** Encode to prefs string: selected|achievements|disabledInCountry|disabledByDefault */
    fun encode(): String? {
        if (isDefault) return null
        val sel = when (selected) { null -> "" ; true -> "1" ; false -> "0" }
        val ach = achievements.joinToString(",") { it.id }
        val country = when (disabledInCurrentCountry) { null -> "" ; true -> "1" ; false -> "0" }
        val def = when (disabledByDefault) { null -> "" ; true -> "1" ; false -> "0" }
        return listOf(sel, ach, country, def).joinToString("|")
    }

    companion object {
        fun decode(s: String?): QuestFilters {
            if (s.isNullOrEmpty()) return QuestFilters()
            val parts = s.split("|")
            if (parts.size != 4) return QuestFilters()
            val sel = when (parts[0]) { "1" -> true ; "0" -> false ; else -> null }
            val ach = parts[1].takeIf { it.isNotEmpty() }
                ?.split(",")?.mapNotNull { id -> EditTypeAchievement.entries.find { it.id == id } }?.toSet()
                ?: emptySet()
            val country = when (parts[2]) { "1" -> true ; "0" -> false ; else -> null }
            val def = when (parts[3]) { "1" -> true ; "0" -> false ; else -> null }
            return QuestFilters(sel, ach, country, def)
        }
    }
}
