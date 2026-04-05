package org.elsoft.bkdb.utils

actual object PlatformUtils {
    actual fun openFile(path: String, customCommand: String?) : Result<Unit> {
       TODO("Not yet implemented")
    }

    actual fun getLegacyConfigPath(): String {
        TODO("Not yet implemented")
    }

    actual fun getCacheDir(): String {
        TODO("Not yet implemented")
    }
}