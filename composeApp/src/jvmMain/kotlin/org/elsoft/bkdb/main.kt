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
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.russhwolf.settings.PreferencesSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
val bookDBPath = "/Apps/EBookLibBrowser/books.json"
val catDBPath = "/Apps/EBookLibBrowser/cats.json"

fun main() = application {
    // Initialize settings immediately for Desktop
    val delegate = Preferences.userRoot().node("org.elsoft.bkdb")
    appSettings = PreferencesSettings(delegate)

    val repo = runBlocking(Dispatchers.IO) {
        val books = DropboxService.downloadToString(bookDBPath)
        val cats = DropboxService.downloadToString(catDBPath)
        val repo = JsonBookRepository(
            ebookJsonString = books,
            catJsonString = cats,
            onSaveBooks = { newContent ->
                DropboxService.uploadFile(bookDBPath, newContent)
            },
            onSaveCategories = { newContent ->
                DropboxService.uploadFile(catDBPath, newContent)
            }
        )
        repo.initialize()
        repo
    }

    // RepositoryProvider.repository = JdbcBookRepository()
    RepositoryProvider.repository = repo

    Window(onCloseRequest = ::exitApplication) {
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

//private fun saveWindowProperties(props: Properties, file: File, state: WindowState) {
//    props.setProperty("width", state.size.width.value.toInt().toString())
//    props.setProperty("height", state.size.height.value.toInt().toString())
//
//    val pos = state.position
//    if (pos is WindowPosition.Absolute) {
//        props.setProperty("x", pos.x.value.toInt().toString())
//        props.setProperty("y", pos.y.value.toInt().toString())
//    }
//    file.outputStream().use { props.store(it, null) }
//}
