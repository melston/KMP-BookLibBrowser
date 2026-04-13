package org.elsoft.bkdb.ui

import androidx.compose.runtime.Composable


@Composable
expect fun AdaptiveDialog(
    onCloseRequest: () -> Unit,
    title: String,
    content: @Composable () -> Unit
)