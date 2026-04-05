package org.elsoft.bkdb.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

// 1. Your Data Enum (Keep this as is)
enum class ReadFilter { ALL, READ, UNREAD }

// 2. Your UI Component (Rename to avoid collision)
@Composable
fun ReadFilterSelector(
    selected: ReadFilter,
    onSelect: (ReadFilter) -> Unit
) {
    val filterTooltip = when(selected) {
        ReadFilter.ALL -> "Currently showing All (Click for Unread)"
        ReadFilter.UNREAD -> "Currently showing Unread (Click for Read)"
        ReadFilter.READ -> "Currently showing Read (Click for All)"
    }
    WithTooltip(filterTooltip) {
        // A simple icon button to cycle through filters
        IconButton(onClick = {
            onSelect(
                when (selected) {
                    ReadFilter.ALL -> ReadFilter.UNREAD
                    ReadFilter.UNREAD -> ReadFilter.READ
                    ReadFilter.READ -> ReadFilter.ALL
                }
            )
        }) {
            Icon(
                imageVector = when (selected) {
                    ReadFilter.ALL -> Icons.AutoMirrored.Filled.List
                    ReadFilter.UNREAD -> Icons.Default.RadioButtonUnchecked
                    ReadFilter.READ -> Icons.Default.CheckCircle
                },
                contentDescription = "Filter by read status",
                tint = if (selected == ReadFilter.ALL) LocalContentColor.current
                else MaterialTheme.colorScheme.primary
            )
        }
    }
}