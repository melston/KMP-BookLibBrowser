package org.elsoft.bkdb.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.elsoft.bkdb.models.Category
import org.elsoft.bkdb.models.EBook
import org.elsoft.bkdb.repository.BookRepository
import org.elsoft.bkdb.utils.ConfigManager
import java.sql.DriverManager
import java.sql.ResultSet

private object Consts {
    const val EBOOK_TBL = "books"
    const val CATEGORY_TBL = "categories"
}

class JdbcBookRepository : BookRepository {

    private val _books = MutableStateFlow<List<EBook>>(emptyList())
    private val _categories = MutableStateFlow<List<Category>>(emptyList())

    private fun getConnection() = DriverManager.getConnection(
        ConfigManager.dbUrl,
        ConfigManager.dbUser,
        ConfigManager.dbPassword
    )

    private fun testConnection(url: String, user: String, pass: String): Boolean {
        return try {
            DriverManager.getConnection(url, user, pass).use { true }
        } catch (e: Exception) {
            false
        }
    }

    private fun executeUpdate(query: String): Result<Unit> {
        try {
            getConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeQuery(query)
                }
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }

        return Result.success(Unit)
    }


    private fun mapRowToBook(rs: ResultSet): EBook {
        val rawDescription = rs.getString("description")
        // Check if the DB value was actually NULL
        val finalDescription = if (rs.wasNull()) {
            null // Or use a default like "No description available"
        } else {
            rawDescription
        }

        val pubID: String? = rs.getString("publisher_id")

        return EBook(
            id = rs.getInt("id"),
            title = rs.getString("title"),
            author = rs.getString("author"),
            pubID = pubID,
            filePath = rs.getString("file_path"),
            isRead = rs.getBoolean("is_read"),
            isFavorite = rs.getBoolean("is_favorite"),
            category = rs.getInt("category_id"),
            description = finalDescription
        )
    }

    override suspend fun getBooksFlow(): Flow<List<EBook>> = _books.asStateFlow()
    override suspend fun getCategoriesFlow(): Flow<List<Category>> = _categories.asStateFlow()

    override suspend fun isAvailable(): Boolean {
        return testConnection(
            ConfigManager.dbUrl,
            ConfigManager.dbUser,
            ConfigManager.dbPassword
        )
    }

    fun initialize() {
        val books = fetchBooks()
        val categories = fetchCategories()

        _books.value = books
        _categories.value = categories
    }

    private fun fetchBooks(): MutableList<EBook> {
        val books = mutableListOf<EBook>()
        val query = "SELECT * FROM ${Consts.EBOOK_TBL} ORDER BY title ASC"
        try {
            getConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(query)
                    while (rs.next()) {
                        books.add(mapRowToBook(rs))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return books
    }

    private fun fetchCategories(): List<Category> {
        val categories = mutableListOf<Category>()
        val query = "SELECT * FROM ${Consts.CATEGORY_TBL} ORDER BY name ASC"
        try {
            getConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery(query)
                    while (rs.next()) {
                        categories.add(
                            Category(
                                id = rs.getInt("id"),
                                name = rs.getString("name"),
                            ))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return categories
    }
    override suspend fun updateReadStatus(bookId: Int, isRead: Boolean): Result<Unit> {
        val query = "UPDATE ${Consts.EBOOK_TBL} SET is_read = ${isRead} WHERE id = ${bookId}"
        return executeUpdate(query)
    }

    override suspend fun updateFavoriteStatus(
        bookId: Int,
        isFavorite: Boolean
    ): Result<Unit> {
        val query = "UPDATE ${Consts.EBOOK_TBL} SET is_favorite = ${isFavorite} WHERE id = ${bookId}"
        return executeUpdate(query)
    }

    override suspend fun updateTitle(bookId: Int, title: String): Result<Unit> {
        val query = "UPDATE ${Consts.EBOOK_TBL} SET title = ${title} WHERE id = ${bookId}"
        return executeUpdate(query)
    }

    override suspend fun updateAuthor(bookId: Int, authorName: String): Result<Unit> {
        val query = "UPDATE ${Consts.EBOOK_TBL} SET author = ${authorName} WHERE id = ${bookId}"
        return executeUpdate(query)
    }

    override suspend fun updateDescription(
        bookId: Int,
        description: String?
    ): Result<Unit> {
        val query = "UPDATE ${Consts.EBOOK_TBL} SET description = ${description} WHERE id = ${bookId}"
        return executeUpdate(query)
    }

    override suspend fun delete(bookId: Int): Result<Unit> {
        val query = "DELETE FROM ${Consts.EBOOK_TBL} WHERE id = ${bookId}"
        return executeUpdate(query)
    }
}