package org.elsoft.bkdb.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.elsoft.bkdb.models.LibraryUiState
import org.elsoft.bkdb.viewmodel.LocalBookViewModel

sealed class LibraryTab(val title: String) {
    data object ByTitle : LibraryTab("By Title")
    data object ByAuthor : LibraryTab("By Author")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val vm = LocalBookViewModel.current

    val tabs = listOf(LibraryTab.ByTitle, LibraryTab.ByAuthor)
    val searchQuery = vm.searchQuery
    var showAboutDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val syncing = vm.isSyncing // Correctly observing the state
//    val stats = vm.libraryStats
//    val online = vm.isOnline
    val duplicateDialogListState = rememberLazyListState()

    // Initial tab
    var selectedTab by remember { mutableStateOf<LibraryTab>(LibraryTab.ByTitle) }

    // This "LaunchedEffect" listens to the ViewModel's events from the uiEvents channel
    LaunchedEffect(Unit) {
        vm.uiEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(shadowElevation = 4.dp) {
                Column {
                    TopAppBar(
                        title = { Text("EBook Library Browser") },
                        actions = {
                            ReadFilterSelector(
                                selected = vm.readFilter,
                                onSelect = {
                                    vm.updateReadFilter(
                                        when (vm.readFilter) {
                                            ReadFilter.ALL -> ReadFilter.UNREAD
                                            ReadFilter.UNREAD -> ReadFilter.READ
                                            ReadFilter.READ -> ReadFilter.ALL
                                        }
                                    )
                                })

                            // Info Button with Tooltip
                            ActionIconButton(
                                onClick = { showAboutDialog = true },
                                icon = Icons.Default.Info,
                                tooltipText = "About"
                            )
                        }
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { vm.updateSearchQuery(it) },
                            modifier = Modifier.weight(0.6f), // Take up remaining space
                            placeholder = { Text("Search by title or author...") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { vm.updateSearchQuery( "" ) }) {
                                        Icon(Icons.Default.Close, null)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(Modifier.width(8.dp))

                        // Category Dropdown
                        CategoryDropdown(modifier = Modifier.weight(0.4f))
                    }

                    TabRow(selectedTabIndex = tabs.indexOf(selectedTab)) {
                        tabs.forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                text = { Text(tab.title) }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.height(48.dp) // Slimmer than a standard bottom bar
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (syncing) {
                        // Show spinning indicator while syncing
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp), // Keep it small for the status bar
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Syncing DB ...",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(
//                            text = stats,
                            text = "DB Updated",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

//                    Text(
//                        text = vm.lastSyncTime.value,
//                        style = MaterialTheme.typography.labelMedium,
//                        color = MaterialTheme.colorScheme.primary
//                    )
//
//                    Spacer(Modifier.width(4.dp))
//
//                    Text(
//                        text = vm.pendingTransactions.value,
//                        style = MaterialTheme.typography.labelMedium,
//                        color = MaterialTheme.colorScheme.primary
//                    )
//
//                    val onlineTooltip = when(online) {
//                        true -> "Online"
//                        else -> "Offline"
//                    }
//                    WithTooltip(onlineTooltip) {
//                        Icon(
//                            imageVector =
//                                if (online) Icons.Default.CloudDone
//                                else Icons.Default.CloudOff,
//                            contentDescription = null,
//                            modifier = Modifier.size(18.dp),
//                            tint = if (online) MaterialTheme.colorScheme.onPrimaryContainer
//                            else MaterialTheme.colorScheme.onSecondaryContainer
//                        )
//                    }

                    // A "Refresh" button if you've added new files to Dropbox/DB
                    ActionIconButton(
                        onClick = { vm.refreshBooks() },
                        icon = Icons.Default.Refresh,
                        tooltipText = "Refresh Books"
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            val uiState = vm.uiState
            val filteredBooks = vm.filteredBooks
            val allBooks = vm.allBooks

            // 1. Content Layer
            if (allBooks.isNotEmpty() && filteredBooks.isEmpty()) {
                EmptyLibraryState(onReset = { vm.resetAllFilters() })
            } else {
                when (selectedTab) {
                    is LibraryTab.ByTitle -> TitleListView()
                    is LibraryTab.ByAuthor -> AuthorListView()
                }
            }

            // 2. Dialog Layer (The "Switchboard")
            when (uiState) {
                is LibraryUiState.Editing -> {
                    BookEditDialog(
                        book = uiState.book,
                        onDismiss = {
                            vm.cancelEditing()
                            vm.resetUiState()
                        },
                        onSave = { title, author, desc ->
                            vm.updateBookMetadata(uiState.book, title, author, desc)
                            vm.cancelEditing()
                            vm.resetUiState()
                        }
                    )
                }
                is LibraryUiState.ConfirmDelete -> {
                    AlertDialog(
                        onDismissRequest = {
                            vm.cancelDeleteConfirmation()
                            vm.resetUiState()
                        },
                        title = { Text("Delete Book?") },
                        text = {
                            Column {
                                Text("This will remove the entry from your library, including DropBox.")
                                Text(
                                    text = "File: ${uiState.book.filePath}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { vm.performDeletion() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { vm.resetUiState() }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
                is LibraryUiState.DuplicateResults -> {
                    DuplicateDialog(
                        state = uiState,
                        listState = duplicateDialogListState, // Pass the hoisted state in
                        onClose = { vm.resetUiState() },
                        onDelete = { vm.startDeleteConfirmation(it) }
                    )
                }
                LibraryUiState.Idle -> { /* Nothing to show */ }
            }
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}
