package org.elsoft.bkdb.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.elsoft.bkdb.models.EBook
import org.elsoft.bkdb.models.LibraryUiState

@Composable
fun DuplicateDialog(
    state: LibraryUiState.DuplicateResults,
    listState: LazyListState,
    onClose: () -> Unit,
    onDelete: (EBook) -> Unit
) {
    AdaptiveDialog(
        onCloseRequest = onClose,
        title = "Potential Duplicates"
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            LazyColumn(
                state = listState, // Uses the hoisted state
                modifier = Modifier.fillMaxSize().padding(end = 16.dp)
            ) {
                items(
                    items = state.groups,
                    key = { it.firstOrNull()?.id ?: 0 } // Key helps stability
                ) { group ->
                    DuplicateGroupItem(group, state.deletedIds, onDelete)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            ListScrollbar(
                state = listState,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            )
        }
    }
}