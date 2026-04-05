package org.elsoft.bkdb.utils

actual object DropboxService {

    actual suspend fun uploadFile(dropboxPath: String, contents: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    actual suspend fun downloadToFile(dropboxPath: String, outFileName: String): String {
        TODO("not implemented")
    }

    actual suspend fun downloadToString(dropboxPath: String): String {
        TODO("not implemented")
    }

    actual suspend fun delete(remotePath: String): Result<Unit> {
        TODO("not implemented")
    }

}