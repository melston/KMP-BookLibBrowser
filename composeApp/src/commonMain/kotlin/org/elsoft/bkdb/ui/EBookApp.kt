package org.elsoft.bkdb.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import org.elsoft.bkdb.repository.RepositoryProvider
import org.elsoft.bkdb.utils.ConfigManager
import org.elsoft.bkdb.viewmodel.BookViewModel
import org.elsoft.bkdb.viewmodel.LocalBookViewModel
import org.jetbrains.skiko.MainUIDispatcher

@Composable
fun EBookApp() {
    var isConfigured by remember { mutableStateOf(ConfigManager.isConfigured()) }
    var forceSetup by remember { mutableStateOf(false) }

    // 1. Create the ViewModel once using our RepositoryProvider
    //val viewModel: BookViewModel = viewModel { BookViewModel(RepositoryProvider.repository) }
    val viewModel: BookViewModel = LocalBookViewModel.current

    MaterialTheme {
        CompositionLocalProvider(LocalBookViewModel provides viewModel) {
            if (!isConfigured || forceSetup) {
                SetupScreen(onConfigSaved = {
                    isConfigured = true
                    forceSetup = false
                })
            } else {
                // Your existing Library UI (Tabs, List, etc.)
                MainScreen()
//                MainScreen(onOpenSettings = { forceSetup = true })
//                DebugScreen()
            }
        }
    }
}