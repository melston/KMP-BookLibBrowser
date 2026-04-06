package org.elsoft.bkdb.utils

import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.WriteMode
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream

actual object DropboxService {
    private val cacheDir = File(PlatformUtils.getCacheDir())
    private val httpClient = OkHttpClient()
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val client: DbxClientV2 by lazy {
        val config = DbxRequestConfig.newBuilder("elsoft.EBookLib").build()

        // Use the credential constructor instead of a simple string
        val credential = DbxCredential(
            "", // Access token (can be empty, it will auto-refresh)
            -1L, // Expires in (auto-handled)
            ConfigManager.dropboxRefreshToken,
            ConfigManager.dropboxAppKey,
            ConfigManager.dropboxAppSecret
        )

        // Create the client
        val clientV2 = DbxClientV2(config, credential)

        // MANDATORY: Force a refresh if the token is currently empty
        // This populates the internal 'Bearer' header correctly.
        try {
            credential.refresh(config)
        } catch (e: Exception) {
            println("Initial Dropbox token refresh failed: ${e.message}")
        }

        clientV2
    }

    init {
        if (!cacheDir.exists()) cacheDir.mkdirs()
    }

    actual suspend fun uploadFile(dropboxPath: String, contents: String): Result<Unit> {
        return try {
            // 1. Ensure the path starts with a forward slash
            val path = if (dropboxPath.startsWith("/")) dropboxPath else "/$dropboxPath"

            // 2. Convert the String to a Stream
            val inputStream = ByteArrayInputStream(contents.toByteArray(Charsets.UTF_8))

            // 3. Upload with "Overwrite" mode so it replaces the existing JSON
            inputStream.use { stream ->
                client.files()
                    .uploadBuilder(path)
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(stream)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    actual suspend fun downloadToFile(dropboxPath: String, outFileName: String): String {
        // 1. Create a cache file in the cache directory
        val outFile = File(cacheDir, outFileName)

        if (!outFile.exists()) {
            // 2. Download from Dropbox
            // This will throw if an exception happens.
            FileOutputStream(outFile).use { outputStream ->
                client.files().downloadBuilder(dropboxPath).download(outputStream)
                outputStream.flush();
            }
            Thread.sleep(100)
        }

        outFile.setReadable(true, true)
        return outFile.path
    }

    actual suspend fun downloadToString(dropboxPath: String): String {
        return try {
            // client is your DbxClientV2 instance
            client.files().download(dropboxPath).inputStream.use { stream ->
                stream.bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "" // Or handle the error appropriately
        }
    }

    actual suspend fun delete(remotePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Get Token (Throw exception if null to trigger runCatching failure)
            val token = getFreshAccessToken()
                ?: throw Exception("Dropbox authorization failed")

            // 2. Setup Client
            val config = DbxRequestConfig.newBuilder("ebook-manager").build()
            val client = DbxClientV2(config, token)

            // 3. Format Path
            val dropboxPath = remotePath.substringAfter("dropbox:")
            val formattedPath = if (dropboxPath.startsWith("/")) dropboxPath else "/$dropboxPath"

            println("Dropbox: Attempting to delete $formattedPath")

            // 4. Execute
            client.files().deleteV2(formattedPath)

            // 5. Return success unit
            Unit
        }
    }

    private suspend fun getFreshAccessToken(): String? = withContext(Dispatchers.IO) {
        try {
            val clientID = ConfigManager.dropboxAppKey.trim()
            val secret = ConfigManager.dropboxAppSecret.trim()
            val refreshToken = ConfigManager.dropboxRefreshToken.trim()

            val requestBody = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .add("client_id", clientID)
                .add("client_secret", secret)
                .build()

            val request = Request.Builder()
                .url("https://api.dropbox.com/oauth2/token")
                .post(requestBody)
                .build()

            // Replace the JSONObject logic with this:
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    println("Dropbox API Error: Code ${response.code}, Body: $errorBody")
                    return@withContext null
                }

                val responseData = response.body?.string() ?: ""

                val mapType = object : TypeToken<Map<String, Any>>() {}.type
                val data: Map<String, Any> = gson.fromJson(responseData, mapType)

                data["access_token"]?.toString()
            }
        } catch (e: Exception) {
            println("Dropbox Auth Error: ${e.message}")
            null
        }
    }

}