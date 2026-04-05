package org.elsoft.bkdb.utils




// Remove: import com.dropbox.core.DbxWebAuth.Request (if present)

expect object DropboxService {

    /**
     * Upload the contents of the string as a file to the specified location.
     */
    suspend fun uploadFile(dropboxPath: String, contents: String): Result<Unit>

    /**
     * Download a file from the specified path in Dropbox and write it
     * to the filename provided.  The filename is expected to have no
     * directory information and the file will be written in the
     * cacheDir directory. If there is already a file at that
     * path then skip the download.
     *
     * @return the full path to the file.
     */
    suspend fun downloadToFile(dropboxPath: String, outFileName: String): String

    /**
     * Download a file and return the contents as a string
     *
     * @return the contents of the file
     */
    suspend fun downloadToString(dropboxPath: String): String

    /**
     * Delete the file on the Dropbox server
     */
    suspend fun delete(remotePath: String): Result<Unit>
}