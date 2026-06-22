package org.elsoft.bkdb.utils

enum class FilenamePattern(val displayName: String) {
    AUTHOR_DASH_TITLE("Author - Title"),
    TITLE_DASH_AUTHOR("Title - Author"),
    TITLE_PAREN_AUTHOR("Title (Author)"),
    TITLE_ONLY("Title Only (Unknown Author)")
}

object FilenameParser {
    fun parseFilename(fileName: String, pattern: FilenamePattern): Pair<String, String> {
        val cleanName = fileName.substringBeforeLast(".")
        return when (pattern) {
            FilenamePattern.AUTHOR_DASH_TITLE -> {
                val parts = splitByDash(cleanName)
                if (parts.size == 2) {
                    Pair(parts[0].trim(), parts[1].trim())
                } else {
                    Pair("Unknown Author", cleanName.trim())
                }
            }
            FilenamePattern.TITLE_DASH_AUTHOR -> {
                val parts = splitByDash(cleanName)
                if (parts.size == 2) {
                    Pair(parts[1].trim(), parts[0].trim())
                } else {
                    Pair("Unknown Author", cleanName.trim())
                }
            }
            FilenamePattern.TITLE_PAREN_AUTHOR -> {
                if (cleanName.contains("(") && cleanName.endsWith(")")) {
                    val title = cleanName.substringBefore("(").trim()
                    val author = cleanName.substringAfter("(").substringBeforeLast(")").trim()
                    Pair(author, title)
                } else {
                    Pair("Unknown Author", cleanName.trim())
                }
            }
            FilenamePattern.TITLE_ONLY -> {
                Pair("Unknown Author", cleanName.trim())
            }
        }
    }

    private fun splitByDash(name: String): List<String> {
        return when {
            name.contains(" - ") -> name.split(" - ", limit = 2)
            name.contains(" -") -> name.split(" -", limit = 2)
            name.contains("- ") -> name.split("- ", limit = 2)
            name.contains("-") -> name.split("-", limit = 2)
            else -> emptyList()
        }
    }
}
