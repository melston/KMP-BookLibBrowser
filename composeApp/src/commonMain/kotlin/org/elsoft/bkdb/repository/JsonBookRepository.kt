package org.elsoft.bkdb.repository

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.elsoft.bkdb.models.Category
import org.elsoft.bkdb.models.EBook

class JsonBookRepository(
    private val ebookJsonString: String,
    private val catJsonString: String,
    private val onSaveBooks: suspend (String) -> Result<Unit>, // Callback for Book JSON
    private val onSaveCategories: suspend (String) -> Result<Unit> // Callback for Category JSON
) : BookRepository {

    private val _books = MutableStateFlow<List<EBook>>(emptyList())
    private val _categories = MutableStateFlow<List<Category>>(emptyList())

    private val json = Json {
        ignoreUnknownKeys = true  // If your JSON has extra fields, don't crash
        coerceInputValues = true  // Helps if a null comes in for a non-nullable type
        encodeDefaults = true     // Includes defaults when saving back to Dropbox
    }

    var isSyncing by mutableStateOf(false)
        private set

    private suspend fun persistBooks(): Result<Unit> {
        isSyncing = true
        try {
            val updatedBooksJson = json.encodeToString(_books.value)
            return onSaveBooks(updatedBooksJson)
        } finally {
            isSyncing = false
        }
    }

    private suspend fun persistCategories(): Result<Unit> {
        isSyncing = true
        try {
            val updatedBooksJson = json.encodeToString(_categories.value)
            return onSaveCategories(updatedBooksJson)
        } finally {
            isSyncing = false
        }
    }

    suspend fun initialize() = withContext(Dispatchers.Default) {
        _books.value = json.decodeFromString(ebookJsonString)
        _categories.value = json.decodeFromString(catJsonString)
    }

    override suspend fun getBooksFlow(): Flow<List<EBook>> = _books.asStateFlow()
    override suspend fun getCategoriesFlow(): Flow<List<Category>> = _categories.asStateFlow()

    override suspend fun isAvailable(): Boolean {
        return _books.value.isNotEmpty()
    }

    override suspend fun updateReadStatus(bookId: Int, isRead: Boolean): Result<Unit> {
        // Update the local list for now
        _books.value = _books.value.map {
            if (it.id == bookId) it.copy(isRead = isRead) else it
        }
        return persistBooks()
    }

    override suspend fun updateFavoriteStatus(
        bookId: Int,
        isFavorite: Boolean
    ): Result<Unit> {
        _books.value = _books.value.map {
            if (it.id == bookId) it.copy(isFavorite = isFavorite) else it
        }
        return persistBooks()
    }

    override suspend fun updateTitle(bookId: Int, title: String): Result<Unit> {
        _books.value = _books.value.map {
            if (it.id == bookId) it.copy(title = title) else it
        }
        return persistBooks()
    }

    override suspend fun updateAuthor(bookId: Int, authorName: String): Result<Unit> {
        _books.value = _books.value.map {
            if (it.id == bookId) it.copy(author = authorName) else it
        }
        return persistBooks()
    }

    override suspend fun updateDescription(
        bookId: Int,
        description: String?
    ): Result<Unit> {
        _books.value = _books.value.map {
            if (it.id == bookId) it.copy(description = description) else it
        }
        return persistBooks()
    }

    override suspend fun delete(bookId: Int): Result<Unit> {
        _books.value = _books.value.filter { it.id != bookId }
        // TODO: Add real implementation
        return Result.success(Unit)
    }
}