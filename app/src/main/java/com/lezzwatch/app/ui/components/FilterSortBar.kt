package com.lezzwatch.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Sort
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
 * Dropdown-style filter chip: a compact FilterChip that opens a DropdownMenu of choices.
 * Used for both country and genre filters so the whole filter row stays a single, low-friction
 * line rather than a separate filter screen (spec: "avoid a complicated filter interface").
 */
@Composable
fun FilterDropdownChip(
    label: String,
    selected: Boolean,
    options: List<String>,
    allLabel: String,
    onOptionSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        FilterChip(
            selected = selected,
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
                text = { Text(allLabel) },
                onClick = { expanded = false; onOptionSelected(null) },
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { expanded = false; onOptionSelected(option) },
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
