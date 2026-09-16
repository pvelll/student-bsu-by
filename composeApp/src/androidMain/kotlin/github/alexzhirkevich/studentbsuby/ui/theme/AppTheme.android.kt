package github.alexzhirkevich.studentbsuby.ui.theme

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Light or dark icons of the (transparent, edge-to-edge) system bars, following the
 * theme selected in the app rather than the system one.
 */
@Composable
internal actual fun SystemBarAppearance(isDark: Boolean) {
    val view = LocalView.current
    val activity = LocalContext.current as? Activity ?: return

    LaunchedEffect(isDark) {
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }
}
