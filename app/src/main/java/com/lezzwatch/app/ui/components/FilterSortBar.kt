package com.lezzwatch.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lezzwatch.app.R
import com.lezzwatch.app.data.model.SortOption

/**
 * Compact FilterChip that opens a DropdownMenu checklist for choosing multiple genres at once —
 * any genre left unchecked is hidden from the Channels list. A "Select all / Deselect all" item
 * pinned above the list is a single quick toggle between everything shown and everything hidden.
 *
 * [selectedGenres] `null` means every genre is selected (the default, unfiltered state).
 */
@Composable
fun GenreFilterChip(
    selectedGenres: Set<String>?,
    availableGenres: List<String>,
    allLabel: String,
    onGenreToggled: (String) -> Unit,
    onToggleSelectAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val allSelected = selectedGenres == null || selectedGenres.size == availableGenres.size
    val selectedCount = selectedGenres?.size ?: availableGenres.size

    val label = when {
        allSelected -> allLabel
        selectedCount == 0 -> stringResource(R.string.channels_no_genres)
        selectedCount == 1 -> selectedGenres!!.first()
        else -> stringResource(R.string.channels_genres_selected_count, selectedCount)
    }

    Box(modifier = modifier) {
        FilterChip(
            selected = !allSelected,
            onClick = { expanded = true },
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                selectedLabelColor = MaterialTheme.colorScheme.primary,
            ),
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (allSelected) {
                            stringResource(R.string.channels_deselect_all)
                        } else {
                            stringResource(R.string.channels_select_all)
                        },
                    )
                },
                leadingIcon = {
                    Checkbox(checked = allSelected, onCheckedChange = { onToggleSelectAll() })
                },
                onClick = onToggleSelectAll,
            )
            Divider()
            availableGenres.forEach { genre ->
                val checked = selectedGenres?.contains(genre) ?: true
                DropdownMenuItem(
                    text = { Text(genre) },
                    leadingIcon = {
                        Checkbox(checked = checked, onCheckedChange = { onGenreToggled(genre) })
                    },
                    onClick = { onGenreToggled(genre) },
                )
            }
        }
    }
}

@Composable
fun SortMenuButton(
    currentSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        FilterChip(
            selected = false,
            onClick = { expanded = true },
            leadingIcon = { Icon(Icons.Filled.Sort, contentDescription = stringResource(R.string.channels_sort)) },
            label = { Text(sortLabel(currentSort)) },
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(sortLabel(option)) },
                    onClick = { expanded = false; onSortSelected(option) },
                )
            }
        }
    }
}

@Composable
private fun sortLabel(option: SortOption): String = when (option) {
    SortOption.NAME_ASC -> stringResource(R.string.sort_az)
    SortOption.NAME_DESC -> stringResource(R.string.sort_za)
    SortOption.COUNTRY -> stringResource(R.string.sort_country)
    SortOption.GENRE -> stringResource(R.string.sort_genre)
    SortOption.FAVORITES_FIRST -> stringResource(R.string.sort_favorites_first)
}
