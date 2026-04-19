package org.elsoft.bkdb.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.elsoft.bkdb.viewmodel.LocalBookViewModel

@Composable
fun TitleListView() {
    val vm = LocalBookViewModel.current

    // 1. Create the shared scroll state
    val state = vm.titleListState
    val scope = rememberCoroutineScope()
    val books = vm.filteredBooks

    val showButton by remember {
        derivedStateOf {
            state.firstVisibleItemIndex > 0
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 2. Pass the state to the LazyColumn
        LazyColumn(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 12.dp) // Leave room for the bar
        ) {
            items(books, key = { it.id }) { book ->
                BookListItem(
                    book,
                    onEditClicked = { vm.startEditing(book) },
                    onDeleteClicked = {vm.bookToDelete = book },
                    onToggleRead = { book -> vm.toggleBookRead(book) },
                    onToggleFavorite = { book -> vm.toggleBookFavorite(book) },
                    onOpenBook = { book -> vm.openBook(book) },
                )
            }
        }

        ListScrollbar(
            state = state,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
        )

        // The Floating Action Button
        ScrollToTopButton(
            visible = showButton,
            onClick = { scope.launch { state.animateScrollToItem(0) } },
            modifier = Modifier.align(Alignment.BottomEnd).padding(32.dp)
        )
    }
}