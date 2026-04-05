package org.elsoft.bkdb.utils

// Define the interface that the common UI/Logic will use
expect object PlatformUtils {
    fun openFile(path: String, customCommand: String?): Result<Unit>
    fun getLegacyConfigPath(): String
    fun getCacheDir(): String
}