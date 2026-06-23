package org.elsoft.bkdb.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.elsoft.bkdb.models.EBook
import org.elsoft.bkdb.models.LibraryUiState
import org.elsoft.bkdb.models.PendingBookImport
import org.elsoft.bkdb.utils.FilenamePattern
import org.elsoft.bkdb.viewmodel.LocalBookViewModel

@Composable
fun SyncWizardDialog() {
    val vm = LocalBookViewModel.current
    when (val uiState = vm.uiState) {
        is LibraryUiState.ConfigureSync -> {
            AdaptiveDialog(
                onCloseRequest = { vm.resetUiState() },
                title = "Sync Configuration"
            ) {
                ConfigureSyncView(
                    scanDirectory = vm.syncScanDirectory,
                    onDirectoryChange = { vm.syncScanDirectory = it },
                    selectedPattern = vm.syncFilenamePattern,
                    onPatternChange = { vm.syncFilenamePattern = it },
                    onStartScan = { vm.startSyncScan() },
                    onCancel = { vm.resetUiState() }
                )
            }
        }
        is LibraryUiState.ScanningDropbox -> {
            AdaptiveDialog(
                onCloseRequest = { /* Prevent closing while scanning */ },
                title = "Scanning Dropbox"
            ) {
                ScanningProgressView()
            }
        }
        is LibraryUiState.SyncWizard -> {
            AdaptiveDialog(
                onCloseRequest = { vm.resetUiState() },
                title = "Library Synchronization Wizard"
            ) {
                SyncWizardView(
                    newFiles = uiState.newFiles,
                    deletedBooks = uiState.deletedBooks,
                    approvedDeletions = vm.approvedDeletions,
                    onUpdateImport = { index, updated -> vm.updatePendingImport(index, updated) },
                    onToggleDeletion = { bookId -> vm.toggleDeletedBookSelection(bookId) },
                    onSelectAllNewFiles = { approved -> vm.selectAllImports(approved) },
                    onApply = { vm.commitSync() },
                    onCancel = { vm.resetUiState() }
                )
            }
        }
        else -> { /* No dialog to show */ }
    }
}

@Composable
fun ConfigureSyncView(
    scanDirectory: String,
    onDirectoryChange: (String) -> Unit,
    selectedPattern: FilenamePattern,
    onPatternChange: (FilenamePattern) -> Unit,
    onStartScan: () -> Unit,
    onCancel: () -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    var localDirectory by remember { mutableStateOf(TextFieldValue(scanDirectory)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Specify which directory to scan for book files (.epub, .pdf) and select how to parse titles and authors from filenames.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = localDirectory,
            onValueChange = {
                localDirectory = it
                onDirectoryChange(it.text)
            },
            label = { Text("Dropbox Directory to Scan") },
            placeholder = { Text("/ (Leave blank or / for root)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
            shape = RoundedCornerShape(12.dp)
        )

        @OptIn(ExperimentalMaterial3Api::class)
        ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = { dropdownExpanded = !dropdownExpanded }
        ) {
            OutlinedTextField(
                value = selectedPattern.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Filename Parsing Format") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                FilenamePattern.entries.forEach { pattern ->
                    DropdownMenuItem(
                        text = { Text(pattern.displayName) },
                        onClick = {
                            onPatternChange(pattern)
                            dropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onStartScan,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Directory")
            }
        }
    }
}

@Composable
fun ScanningProgressView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Traversing Dropbox...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Checking directories for new and deleted epub/pdf files. This may take a few moments.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun SyncWizardView(
    newFiles: List<PendingBookImport>,
    deletedBooks: List<EBook>,
    approvedDeletions: Set<Int>,
    onUpdateImport: (Int, PendingBookImport) -> Unit,
    onToggleDeletion: (Int) -> Unit,
    onSelectAllNewFiles: (Boolean) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit
) {
    val totalNew = newFiles.size
    val totalDeleted = deletedBooks.size

    if (totalNew == 0 && totalDeleted == 0) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CloudDone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Library up-to-date",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No additions or deletions were found in the scanned directory.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onCancel, shape = RoundedCornerShape(12.dp)) {
                Text("Close")
            }
        }
    } else {
        val listState = rememberLazyListState()
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Deleted files section
                    if (totalDeleted > 0) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Disappeared Files ($totalDeleted)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    Text(
                                        text = "The following files are present in books.json but no longer exist in Dropbox. Choose which to clean up:",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                    deletedBooks.forEach { book ->
                                        val isSelected = book.id in approvedDeletions
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { onToggleDeletion(book.id) },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = MaterialTheme.colorScheme.error
                                                )
                                            )
                                            Column {
                                                Text(
                                                    text = "${book.title} - ${book.author}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = book.filePath.substringAfter("dropbox:"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // New files section
                    if (totalNew > 0) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                Text(
                                    text = "New Files Discovered ($totalNew)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = "Configure details for the new ebook files before committing them to your library.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { onSelectAllNewFiles(true) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Select All", style = MaterialTheme.typography.labelLarge)
                                    }

                                    TextButton(
                                        onClick = { onSelectAllNewFiles(false) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Select None", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }

                        itemsIndexed(newFiles) { index, importItem ->
                            NewBookImportCard(
                                importItem = importItem,
                                // Pass index to uniquely update item
                                onUpdate = { updated -> onUpdateImport(index, updated) }
                            )
                        }
                    }
                }

                ListScrollbar(
                    state = listState,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onApply,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply Sync Changes")
                }
            }
        }
    }
}

@Composable
fun NewBookImportCard(
    importItem: PendingBookImport,
    onUpdate: (PendingBookImport) -> Unit
) {
    val vm = LocalBookViewModel.current
    val categories = vm.categories
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    var localTitle by remember(importItem.filePath, importItem.defaultTitle) { mutableStateOf(TextFieldValue(importItem.title)) }
    var localAuthor by remember(importItem.filePath, importItem.defaultAuthor) { mutableStateOf(TextFieldValue(importItem.author)) }
    var localDescription by remember(importItem.filePath) { mutableStateOf(TextFieldValue(importItem.description)) }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (importItem.isApproved) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (importItem.isApproved) MaterialTheme.colorScheme.outlineVariant
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Checkbox to import
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = importItem.isApproved,
                    onCheckedChange = { onUpdate(importItem.copy(isApproved = it)) }
                )
                Icon(
                    imageVector = if (importItem.filePath.endsWith(".pdf", ignoreCase = true)) Icons.Default.PictureAsPdf else Icons.Default.Book,
                    contentDescription = null,
                    tint = if (importItem.isApproved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = importItem.filePath.substringAfterLast("/"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (importItem.isApproved) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = importItem.isApproved) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Path: " + importItem.filePath.substringAfter("dropbox:"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = localTitle,
                            onValueChange = {
                                localTitle = it
                                onUpdate(importItem.copy(title = it.text))
                            },
                            label = { Text("Title") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = localAuthor,
                            onValueChange = {
                                localAuthor = it
                                onUpdate(importItem.copy(author = it.text))
                            },
                            label = { Text("Author") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // Category dropdown row
                    val selectedCategoryName = categories.firstOrNull { it.id == importItem.categoryId }?.name ?: "No Category"
                    @OptIn(ExperimentalMaterial3Api::class)
                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategoryName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        when (keyEvent.key) {
                                            Key.Spacebar, Key.Enter, Key.DirectionDown -> {
                                                categoryDropdownExpanded = true
                                                true
                                            }
                                            else -> false
                                        }
                                    } else {
                                        false
                                    }
                                },
                            shape = RoundedCornerShape(8.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        onUpdate(importItem.copy(categoryId = category.id))
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = localDescription,
                        onValueChange = {
                            localDescription = it
                            onUpdate(importItem.copy(description = it.text))
                        },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = importItem.isFavorite,
                                onCheckedChange = { onUpdate(importItem.copy(isFavorite = it)) }
                            )
                            Text("Favorite", style = MaterialTheme.typography.bodyMedium)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = importItem.isRead,
                                onCheckedChange = { onUpdate(importItem.copy(isRead = it)) }
                            )
                            Text("Mark as Read", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}