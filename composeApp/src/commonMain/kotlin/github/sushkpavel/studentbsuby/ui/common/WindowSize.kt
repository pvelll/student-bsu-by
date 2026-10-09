package github.sushkpavel.studentbsuby.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

@Composable
fun isTablet(): Boolean {
    val containerSize = LocalWindowInfo.current.containerSize
    return with(LocalDensity.current) {
        minOf(containerSize.width, containerSize.height).toDp()
    } >= 600.dp
}
