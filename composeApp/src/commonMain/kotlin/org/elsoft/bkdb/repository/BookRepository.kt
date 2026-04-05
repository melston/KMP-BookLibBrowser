package org.elsoft.bkdb.repository

import kotlinx.coroutines.flow.Flow
import org.elsoft.bkdb.models.Category
import org.elsoft.bkdb.models.EBook

interface BookRepository {
    suspend fun isAvailable(): Boolean
    suspend fun getCategoriesFlow(): Flow<List<Category>>
    suspend fun getBooksFlow(): Flow<List<EBook>>
    suspend fun updateReadStatus(bookId: Int, isRead: Boolean): Result<Unit>
    suspend fun updateFavoriteStatus(bookId: Int, isFavorite: Boolean): Result<Unit>
    suspend fun updateTitle(bookId: Int, title: String): Result<Unit>
    suspend fun updateAuthor(bookId: Int, authorName: String): Result<Unit>
    suspend fun updateDescription(bookId: Int, description: String?): Result<Unit>
    suspend fun delete(bookId: Int): Result<Unit>
}