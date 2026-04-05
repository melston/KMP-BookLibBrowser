package org.elsoft.bkdb.viewmodel

import androidx.compose.runtime.staticCompositionLocalOf

// This creates the "Key" for the provider
val LocalBookViewModel = staticCompositionLocalOf<BookViewModel> {
    error("No BookViewModel provided")
}