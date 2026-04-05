package org.elsoft.bkdb.utils

import org.jetbrains.skiko.OS
import java.io.File

enum class OS {
    WINDOWS, LINUX, MACOS, UNKNOWN
}

actual object PlatformUtils {

    actual fun openFile(path: String, customCommand: String?): Result<Unit> {
        // Use custom command, or fall back to the platform default
        val commandToUse = when {
            !customCommand.isNullOrBlank() -> customCommand.replace("%f", path)
            else -> getDefaultViewerCommand(path)
        }

        if (commandToUse.isEmpty()) {
            return Result.failure(Exception("No viewer command available for this platform."))
        }

        return try {
            val parts = parseCommand(commandToUse)
            ProcessBuilder(parts)
                .directory(File(System.getProperty("user.home")))
                .start()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual fun getLegacyConfigPath(): String {
        val userHome = System.getProperty("user.home")
        val os = System.getProperty("os.name").lowercase()

        return if (os.contains("win")) {
            // Windows: AppData/Roaming/EBookLibrary/config.properties
            val appData = System.getenv("APPDATA")
            "$appData/EBookLibrary/config.properties"
        } else {
            // Linux: .config/ebooklibrary/config.properties
            "$userHome/.config/ebooklibrary/config.properties"
        }
    }

    actual fun getCacheDir(): String {
        val userHome = System.getProperty("user.home")
        val os = System.getProperty("os.name").lowercase()

        return if (os.contains("win")) {
            // Windows: AppData/Local/EBookLibrary/Cache
            val appData = System.getenv("LOCALAPPDATA")
            "$appData/EBookLibrary/Cache"
        } else {
            // Linux: ~/EbookLibrary/Cache
            "$userHome/EbookLibrary/Cache"
        }
    }

    /**
     * Provides a sensible default command if the config is empty.
     */
    fun getDefaultViewerCommand(path: String): String {
        return when (current) {
            org.elsoft.bkdb.utils.OS.WINDOWS -> "cmd /c start \"\" \"$path\""
            org.elsoft.bkdb.utils.OS.LINUX -> "xdg-open \"$path\""
            else -> ""
        }
    }

    val current: org.elsoft.bkdb.utils.OS = System.getProperty("os.name").lowercase().let {
        when {
            it.contains("win") -> org.elsoft.bkdb.utils.OS.WINDOWS
            it.contains("linux") -> org.elsoft.bkdb.utils.OS.LINUX
            it.contains("mac") -> org.elsoft.bkdb.utils.OS.MACOS
            else -> org.elsoft.bkdb.utils.OS.UNKNOWN
        }
    }


    private fun parseCommand(command: String): List<String> {
        val regex = """[^\s"']+|"([^"]*)"|'([^']*)'""".toRegex()
        return regex.findAll(command).map {
            it.groupValues[1].ifEmpty {
                it.groupValues[2].ifEmpty { it.groupValues[0] }
            }
        }.toList()
    }

}