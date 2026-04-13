package org.elsoft.bkdb

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.russhwolf.settings.SharedPreferencesSettings
import kotlinx.coroutines.launch
import org.elsoft.bkdb.repository.JsonBookRepository
import org.elsoft.bkdb.repository.RepositoryProvider
import org.elsoft.bkdb.ui.EBookApp
import org.elsoft.bkdb.utils.ConfigManager
import org.elsoft.bkdb.utils.DropboxService
import org.elsoft.bkdb.utils.PlatformUtils
import org.elsoft.bkdb.utils.appSettings
import org.elsoft.bkdb.viewmodel.BookViewModel
import org.elsoft.bkdb.viewmodel.LocalBookViewModel

class MainActivity : ComponentActivity() {

    val bookDBPath = "/Apps/EBookLibBrowser/books.json"
    val catDBPath = "/Apps/EBookLibBrowser/cats.json"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Android-specific settings
        val sharedPrefs = getSharedPreferences("ebook_prefs", MODE_PRIVATE)
        appSettings = SharedPreferencesSettings(sharedPrefs)

        // Only seed if not already set (allows you to change them in-app later)
        if (ConfigManager.dropboxAppKey.isEmpty()) {
            ConfigManager.dropboxAppKey = BuildKonfig.DROPBOX_APP_KEY
            ConfigManager.dropboxAppSecret = BuildKonfig.DROPBOX_APP_SECRET
            ConfigManager.dropboxRefreshToken = BuildKonfig.DROPBOX_REFRESH_TOKEN
        }

//        Log.d("BKDB_DEBUG", "Key: ${ConfigManager.dropboxAppKey}...")
//        Log.d("BKDB_DEBUG", "Secret: ${ConfigManager.dropboxAppSecret}...")
//        Log.d("BKDB_DEBUG", "Refresh Token: ${ConfigManager.dropboxRefreshToken}...")
//
//        // Also check the paths - if these are wrong, Dropbox returns a 404/Empty
//        Log.d("BKDB_DEBUG", "Book DB Path: $bookDBPath")

        // Seed the Utils with the context
        PlatformUtils.init(this)

        // We use a Coroutine Scope that lives as long as the Activity
        lifecycleScope.launch {
            Log.d("BKDB_DEBUG", "Coroutine started. Current Thread: ${Thread.currentThread().name}")

            // Check Config BEFORE calling Dropbox
            Log.d("BKDB_DEBUG", "Key in Config: ${ConfigManager.dropboxAppKey.take(5)}...")

            // 1. Initialize the repo (identical to your Desktop logic)
            val books = DropboxService.downloadToString(bookDBPath)

            Log.e("BKDB_NET", "Downloaded ${books.length} characters from $bookDBPath")
            if (books.isEmpty()) {
                Log.e("BKDB_NET", "WARNING: Downloaded string is EMPTY for path: $bookDBPath")
            }

            val cats = DropboxService.downloadToString(catDBPath)

            Log.e("BKDB_NET", "Downloaded ${cats.length} characters from $catDBPath")
            if (cats.isEmpty()) {
                Log.e("BKDB_NET", "WARNING: Downloaded string is EMPTY for path: $catDBPath")
            }

            val repo = JsonBookRepository(
                ebookJsonString = books,
                catJsonString = cats,
                onSaveBooks = { content -> DropboxService.uploadFile(bookDBPath, content) },
                onSaveCategories = { content -> DropboxService.uploadFile(catDBPath, content) }
            )
            repo.initialize()

            // 2. Set the global provider
            RepositoryProvider.repository = repo

            // 3. NOW trigger the UI
            setContent {
                CompositionLocalProvider(LocalViewModelStoreOwner provides this@MainActivity) {
                    val vm: BookViewModel = viewModel {
                        BookViewModel(RepositoryProvider.repository)
                    }
                    CompositionLocalProvider(LocalBookViewModel provides vm) {
                        EBookApp()
                    }
                }
            }
        }
    }
}
