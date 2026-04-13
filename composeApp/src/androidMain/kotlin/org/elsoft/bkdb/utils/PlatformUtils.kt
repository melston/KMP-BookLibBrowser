package org.elsoft.bkdb.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.lang.ref.WeakReference

actual object PlatformUtils {
    private var contextRef: WeakReference<Context>? = null

    fun init(context: Context) {
        contextRef = WeakReference(context)
    }

    private val context: Context
        get() = contextRef?.get() ?: throw IllegalStateException("PlatformUtils not initialized with Context")

    actual fun openFile(path: String, customCommand: String?): Result<Unit> = runCatching {
        val file = File(path)
        // Android requires a FileProvider to share files with other apps (like a PDF reader)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, context.contentResolver.getType(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    actual fun getLegacyConfigPath(): String {
        // On Android, "Config" usually goes into internal files
        return context.filesDir.absolutePath
    }

    actual fun getCacheDir(): String {
        return context.cacheDir.absolutePath
    }
}