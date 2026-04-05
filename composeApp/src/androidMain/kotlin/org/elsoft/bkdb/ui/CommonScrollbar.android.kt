package org.elsoft.bkdb.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

actual @Composable
fun CommonScrollbar(
    scrollState: ScrollState,
    modifier: Modifier
) {
    // Android doesn't need a visible scrollbar thumb
    Spacer(modifier = modifier)
}

@Composable
actual fun CommonScrollbar(
    lazyListState: LazyListState,
    modifier: Modifier
) {
    // Android doesn't need a visible scrollbar thumb
    Spacer(modifier = modifier)
}