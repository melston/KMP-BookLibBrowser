package org.elsoft.bkdb.data.remote

import org.elsoft.bkdb.models.Category
import org.elsoft.bkdb.models.EBook

actual class DatabaseManager{

    actual companion object {
        actual fun testConnection(url: String, user: String, pass: String): Boolean {
            return true
        }

    }

    actual suspend fun isAvailable(): Boolean {
        return true
    }

    actual suspend fun fetchCategories(): List<Category> {
        return emptyList()
    }

    actual suspend fun fetchBooks(): List<EBook> {
        return emptyList()
    }

    actual suspend fun updateReadStatus(bookId: Int, isRead: Boolean): Result<Unit> {
        return Result.success(Unit)
    }

    actual suspend fun updateFavoriteStatus(bookId: Int, isFavorite: Boolean): Result<Unit> {
        return Result.success(Unit)
    }

    actual suspend fun updateTitle(bookId: Int, title: String): Result<Unit> {
        return Result.success(Unit)
    }

    actual suspend fun updateAuthor(bookId: Int, authorName: String): Result<Unit> {
        return Result.success(Unit)
    }

    actual suspend fun updateDescription(bookId: Int, description: String?): Result<Unit> {
        return Result.success(Unit)
    }

    actual suspend fun delete(bookId: Int): Result<Int> {
        return Result.success(Int.MIN_VALUE)
    }
}