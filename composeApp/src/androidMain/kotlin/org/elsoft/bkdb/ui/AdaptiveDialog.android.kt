package org.elsoft.bkdb.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
actual fun AdaptiveDialog(
    onCloseRequest: () -> Unit,
    title: String,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onCloseRequest
    ) {
        // You might want a Surface or Card here to make it look like a dialog
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            tonalElevation = 6.dp
        ) {
            content()
        }
    }
}