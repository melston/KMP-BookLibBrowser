package org.elsoft.bkdb.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberDialogState

@Composable
actual fun AdaptiveDialog(
    onCloseRequest: () -> Unit,
    title: String,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onCloseRequest = onCloseRequest,
        title = title,
        state = rememberDialogState(width = 900.dp, height = 700.dp), // You can customize size here
        content = { content() }
    )
}