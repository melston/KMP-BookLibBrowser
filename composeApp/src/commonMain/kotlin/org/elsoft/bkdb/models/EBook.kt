package org.elsoft.bkdb.models

import kotlinx.serialization.Serializable
import org.elsoft.bkdb.utils.ConfigManager
import org.elsoft.bkdb.utils.DropboxService
import org.elsoft.bkdb.utils.PlatformUtils
import java.io.File

// Standard Java/Kotlin for Linux process execution

@Serializable
data class Category(val id: Int, val name: String)

@Serializable
data class EBook(
    val id: Int,
    val title: String = "AAA",
    val author: String = "BBB",
    val filePath: String = "CCC",
    val isRead: Boolean = false,
    val isFavorite: Boolean = false,
    val category: Int = 0,
    val pubID: String? = null,
    val description: String? = null,
)

object EBooks {
    suspend fun open(book: EBook): Result<Unit> {
        val filePath = book.filePath
        val customCommand = ConfigManager.viewerCommand

        return try {
            val localPath = if (filePath.startsWith("dropbox:")) {
                val dbxpath = filePath.substringAfter(":")
                val uniqueCacheName = cacheName(dbxpath)
                DropboxService.downloadToFile(dbxpath, uniqueCacheName)
            } else {
                filePath
            }

            if (!File(localPath).exists()) {
                return Result.failure(Exception("File not found at $localPath"))
            }

            PlatformUtils.openFile(localPath, customCommand)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cacheName(path: String): String {
        val fullFileName = path.substringAfterLast("/")
        val baseName = fullFileName.substringBeforeLast(".", "")
        val extension = fullFileName.substringAfterLast(".", "")

        // 1. Shorten only the base name (remove a-z and _)
        val shortenedBase = baseName
            .replace("&", "A")
            .replace("!", "")
            .replace(Regex("[a-z_]"), "")
            .replace(" ", "")

        // 2. Build the unique name: book + hash + shortenedBase + .ext
        // This ensures "Java_Pro_1.epub" and "Java_Pro_2.epub" stay unique
        val hash = path.hashCode().toString().takeLast(6)

        return "BK_${hash}_$shortenedBase.$extension"
    }


}