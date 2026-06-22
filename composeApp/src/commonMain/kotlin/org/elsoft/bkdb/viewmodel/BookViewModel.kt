package org.elsoft.bkdb.viewmodel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.elsoft.bkdb.models.Category
import org.elsoft.bkdb.models.EBook
import org.elsoft.bkdb.models.EBooks
import org.elsoft.bkdb.models.LibraryUiState
import org.elsoft.bkdb.models.PendingBookImport
import org.elsoft.bkdb.repository.BookRepository
import org.elsoft.bkdb.repository.JsonBookRepository
import org.elsoft.bkdb.ui.ReadFilter
import org.elsoft.bkdb.utils.FilenamePattern
import org.elsoft.bkdb.utils.FilenameParser
import org.elsoft.bkdb.utils.DropboxService
import org.elsoft.bkdb.utils.ConfigManager

class BookViewModel(private val repository: BookRepository) : ViewModel() {
    private val _uiEvents = Channel<String>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

    var uiState by mutableStateOf<LibraryUiState>(LibraryUiState.Idle)
        private set

    val isSyncing: Boolean
        get() = (repository as? JsonBookRepository)?.isSyncing ?: false

    val titleListState = LazyListState()
    val authorListState = LazyListState()

    var readFilter by mutableStateOf<ReadFilter>(ReadFilter.ALL)
        private set

    var searchQuery by mutableStateOf("")
        private set

    var categories by mutableStateOf<List<Category>>(emptyList())
        private set

    var selectedCategory by mutableStateOf<Category?>(null)

    var syncScanDirectory by mutableStateOf("")
    var syncFilenamePattern by mutableStateOf(FilenamePattern.AUTHOR_DASH_TITLE)

    var pendingImports by mutableStateOf<List<PendingBookImport>>(emptyList())
    var deletedBooks by mutableStateOf<List<EBook>>(emptyList())
    var approvedDeletions by mutableStateOf<Set<Int>>(emptySet())

    // Using 'by' makes this look like a regular List to the rest of the app
    var allBooks by mutableStateOf<List<EBook>>(emptyList())
        private set // Only the ViewModel can change the list

//    // 1. Derived Author List (Sorted Alphabetically)
//    val authors by derivedStateOf {
//        allBooks.map { it.author }
//            .distinct()
//            .sortedBy { it.lowercase() }
//    }

    // 1. The Filtered List (Replaces the combine {...}.stateIn block)
    val filteredBooks by derivedStateOf {
        allBooks.filter { book ->
            // Text Search (Title or Author)
            val matchesQuery = searchQuery.isBlank() ||
                    book.title.contains(searchQuery, ignoreCase = true) ||
                    book.author.contains(searchQuery, ignoreCase = true)

            // Category Filter
            val matchesCategory = selectedCategory == null || book.category == selectedCategory?.id

            // Read/Unread Filter
            val matchesRead = when (readFilter) {
                ReadFilter.ALL -> true
                ReadFilter.UNREAD -> !book.isRead
                ReadFilter.READ -> book.isRead
            }

            matchesQuery && matchesCategory && matchesRead
        }
    }

    // 2. The Grouped Map (Replaces the map {...}.stateIn block)
    val booksByAuthor by derivedStateOf {
        filteredBooks.groupBy { it.author }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
            .mapValues { entry ->
                entry.value.sortedBy { it.title }
            }
    }

    // The "Active" book being edited or deleted
    var bookToEdit by mutableStateOf<EBook?>(null)

    var bookToDelete by mutableStateOf<EBook?>(null)

    init {
        println("ViewModel Initializing with Repository: ${repository::class.simpleName}")
        refreshBooks()
    }

    fun refreshBooks() {
        viewModelScope.launch {
            repository.getBooksFlow().collect { newList ->
                allBooks = newList
            }
        }
        viewModelScope.launch {
            repository.getCategoriesFlow().collect { newList ->
                categories = newList
            }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun updateReadFilter(filter: ReadFilter) {
        readFilter = filter
    }

    fun startEditing(book: EBook) {
        uiState = LibraryUiState.Editing(book)
    }

    fun cancelEditing() { bookToEdit = null }

    fun startDeleteConfirmation(book: EBook) {
        uiState = LibraryUiState.ConfirmDelete(book)
    }

    fun cancelDeleteConfirmation() { bookToDelete = null }

    fun resetUiState() {
        uiState = LibraryUiState.Idle
    }

    fun resetAllFilters() {
        readFilter = ReadFilter.ALL
        selectedCategory = null
        searchQuery = ""
    }

    fun performDeletion() {
        val state = uiState
        if (state is LibraryUiState.ConfirmDelete) {
            val bookToDelete = state.book

            viewModelScope.launch {
                // 1. Tell the Repository to delete it (e.g., update the JSON)
                repository.delete(bookToDelete.id)
                    .onSuccess {
                        // 2. Update the local UI state by filtering
                        allBooks = allBooks.filter { it.id != bookToDelete.id }

                        // 3. Close the dialog
                        resetUiState()
                    }
                    .onFailure {
                        _uiEvents.send("Failed to delete ${bookToDelete.title}: ${it.message}")
                    }
            }
        }
    }

    fun toggleBookRead(book: EBook) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateReadStatus(book.id, !book.isRead)
        }
    }

    fun toggleBookFavorite(book: EBook) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateFavoriteStatus(book.id, !book.isFavorite)
        }
    }

    fun updateBookMetadata(book: EBook, title: String, author: String, desc: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTitle(book.id, title)
            repository.updateAuthor(book.id, author)
            repository.updateDescription(book.id, desc)
        }
    }

    fun openBook(book: EBook) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = EBooks.open(book)

            result.onFailure { error ->
                _uiEvents.send("Error opening book: ${error.message}")
            }
        }
    }

    fun startSyncConfiguration() {
        syncScanDirectory = ConfigManager.dropboxCloudScanPath
        syncFilenamePattern = FilenamePattern.AUTHOR_DASH_TITLE
        uiState = LibraryUiState.ConfigureSync
    }

    fun startSyncScan() {
        ConfigManager.dropboxCloudScanPath = syncScanDirectory
        uiState = LibraryUiState.ScanningDropbox
        viewModelScope.launch(Dispatchers.Default) {
            val dropboxFiles = DropboxService.listSourceFiles(syncScanDirectory, listOf(".epub", ".pdf"))
            
            // Format files to match books.json filePath (which is e.g. "dropbox:/path/to/book.epub")
            val indexedPaths = allBooks.map { it.filePath }
            
            val newFilePaths = dropboxFiles.filter { file ->
                val formattedPath = "dropbox:$file"
                formattedPath !in indexedPaths
            }

            val matchedFiles = dropboxFiles.map { "dropbox:$it" }.toSet()
            
            val scanDirPrefix = if (syncScanDirectory.trim() == "/" || syncScanDirectory.isBlank()) {
                "dropbox:"
            } else {
                val cleanedDir = if (syncScanDirectory.startsWith("/")) syncScanDirectory else "/$syncScanDirectory"
                val dirWithTrailingSlash = if (cleanedDir.endsWith("/")) cleanedDir else "$cleanedDir/"
                "dropbox:$dirWithTrailingSlash"
            }

            val deleted = allBooks.filter { book ->
                book.filePath.startsWith("dropbox:") &&
                book.filePath.startsWith(scanDirPrefix, ignoreCase = true) &&
                book.filePath !in matchedFiles
            }

            val parsedImports = newFilePaths.map { filePath ->
                val fileName = filePath.substringAfterLast("/")
                val parsed = FilenameParser.parseFilename(fileName, syncFilenamePattern)
                PendingBookImport(
                    filePath = "dropbox:$filePath",
                    defaultTitle = parsed.second,
                    defaultAuthor = parsed.first,
                    title = parsed.second,
                    author = parsed.first,
                    categoryId = 0,
                    isFavorite = false,
                    isRead = false,
                    description = "",
                    isApproved = false
                )
            }

            pendingImports = parsedImports
            deletedBooks = deleted
            approvedDeletions = deleted.map { it.id }.toSet()

            uiState = LibraryUiState.SyncWizard(pendingImports, deletedBooks)
        }
    }

    fun updatePendingImport(index: Int, updated: PendingBookImport) {
        pendingImports = pendingImports.toMutableList().apply {
            this[index] = updated
        }
        val current = uiState
        if (current is LibraryUiState.SyncWizard) {
            uiState = current.copy(newFiles = pendingImports)
        }
    }

    fun selectAllImports(approved: Boolean) {
        pendingImports = pendingImports.map { it.copy(isApproved = approved) }
        val current = uiState
        if (current is LibraryUiState.SyncWizard) {
            uiState = current.copy(newFiles = pendingImports)
        }
    }

    fun toggleDeletedBookSelection(bookId: Int) {
        approvedDeletions = if (bookId in approvedDeletions) {
            approvedDeletions - bookId
        } else {
            approvedDeletions + bookId
        }
    }

    fun commitSync() {
        viewModelScope.launch(Dispatchers.Default) {
            // 1. Process deletions
            if (approvedDeletions.isNotEmpty()) {
                repository.deleteBooks(approvedDeletions)
            }

            // 2. Process additions
            val nextStartId = (allBooks.maxOfOrNull { it.id } ?: 0) + 1
            var idCounter = nextStartId
            val booksToInsert = pendingImports.filter { it.isApproved }.map { import ->
                EBook(
                    id = idCounter++,
                    title = import.title,
                    author = import.author,
                    filePath = import.filePath,
                    isRead = import.isRead,
                    isFavorite = import.isFavorite,
                    category = import.categoryId,
                    description = import.description.ifBlank { null }
                )
            }

            if (booksToInsert.isNotEmpty()) {
                repository.addBooks(booksToInsert)
            }

            // 3. Clear states
            pendingImports = emptyList()
            deletedBooks = emptyList()
            approvedDeletions = emptySet()

            // 4. Force refresh of the book list
            refreshBooks()
            uiState = LibraryUiState.Idle

            _uiEvents.send("Sync completed successfully.")
        }
    }
}