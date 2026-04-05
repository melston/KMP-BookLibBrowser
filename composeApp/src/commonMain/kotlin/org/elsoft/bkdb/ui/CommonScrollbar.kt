// commonMain/.../ui/CommonScrollbar.kt
package org.elsoft.bkdb.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun CommonScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
)

@Composable
expect fun CommonScrollbar(
    lazyListState: LazyListState, // For LazyColumns (MainScreen/Debug)
    modifier: Modifier = Modifier
)