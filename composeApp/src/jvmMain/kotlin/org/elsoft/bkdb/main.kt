package org.elsoft.bkdb

//import androidx.compose.runtime.CompositionLocalProvider
//import androidx.compose.runtime.staticCompositionLocalOf
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.window.Window
//import androidx.compose.ui.unit.DpSize
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.window.*
//import org.elsoft.bkdb.ui.EBookApp
//import java.awt.GraphicsEnvironment
//import java.io.File
//import java.util.Properties

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.russhwolf.settings.PreferencesSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.elsoft.bkdb.repository.JsonBookRepository
import org.elsoft.bkdb.repository.RepositoryProvider
import org.elsoft.bkdb.ui.EBookApp
import org.elsoft.bkdb.utils.DropboxService
import java.util.prefs.Preferences
import org.elsoft.bkdb.utils.appSettings
import org.elsoft.bkdb.viewmodel.BookViewModel
import org.elsoft.bkdb.viewmodel.LocalBookViewModel

// 1. Create a simple owner for the Desktop window
class DesktopViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}

val viewModelStoreOwner = DesktopViewModelStoreOwner()
const val bookDBPath = "/Apps/EBookLibBrowser/books.json"
const val catDBPath = "/Apps/EBookLibBrowser/cats.json"

fun main() {
    // 1. Grab your existing preferences delegate
    val prefs = Preferences.userRoot().node("org.elsoft.bkdb")
    appSettings = PreferencesSettings(prefs)

    // 2. Load last known values (with sensible defaults)
    val lastX = prefs.getInt("window_x", 100)
    val lastY = prefs.getInt("window_y", 100)
    val lastWidth = prefs.getInt("window_width", 1200)
    val lastHeight = prefs.getInt("window_height", 800)

    val repo = runBlocking(Dispatchers.IO) {
        val books = DropboxService.downloadToString(bookDBPath)
        val cats = DropboxService.downloadToString(catDBPath)
        val r = JsonBookRepository(
            ebookJsonString = books,
            catJsonString = cats,
            onSaveBooks = { newContent ->
                DropboxService.uploadFile(bookDBPath, newContent)
            },
            onSaveCategories = { newContent ->
                DropboxService.uploadFile(catDBPath, newContent)
            }
        )
        r.initialize()
        r
    }

    application {
        val windowState = rememberWindowState(
            position = WindowPosition(lastX.dp, lastY.dp),
            size = DpSize(lastWidth.dp, lastHeight.dp)
        )

        RepositoryProvider.repository = repo

        Window(
            onCloseRequest = {
                prefs.putInt("window_x", windowState.position.x.value.toInt())
                prefs.putInt("window_y", windowState.position.y.value.toInt())
                prefs.putInt("window_width", windowState.size.width.value.toInt())
                prefs.putInt("window_height", windowState.size.height.value.toInt())

                exitApplication()
            },
            state = windowState,
            title = "EBook Library Browser"
        ) {
            // 1. Provide the StoreOwner (The "House" for the VM)
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {

                // 2. Initialize the VM ONLY ONCE here using the factory
                val vm: BookViewModel = viewModel {
                    BookViewModel(RepositoryProvider.repository)
                }

                // 3. Pass THAT instance into your custom Local provider
                CompositionLocalProvider(LocalBookViewModel provides vm) {
                    EBookApp()
                }
            }
        }
    }
}
