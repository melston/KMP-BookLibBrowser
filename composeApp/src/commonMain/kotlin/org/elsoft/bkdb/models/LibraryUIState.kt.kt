package org.elsoft.bkdb.models

sealed class LibraryUiState {
    object Idle : LibraryUiState()
    data class Editing(val book: EBook) : LibraryUiState()
    data class ConfirmDelete(val book: EBook) : LibraryUiState()
    data class DuplicateResults(
        val groups: List<List<EBook>>,
        val deletedIds: Set<Int> = emptySet()
    ) : LibraryUiState()

    // Sync Wizard UI States
    object ConfigureSync : LibraryUiState()
    object ScanningDropbox : LibraryUiState()
    data class SyncWizard(
        val newFiles: List<PendingBookImport>,
        val deletedBooks: List<EBook>
    ) : LibraryUiState()
}

data class PendingBookImport(
    val filePath: String,
    val defaultTitle: String,
    val defaultAuthor: String,
    val title: String,
    val author: String,
    val categoryId: Int,
    val isFavorite: Boolean = false,
    val isRead: Boolean = false,
    val description: String = "",
    val isApproved: Boolean = true
)
