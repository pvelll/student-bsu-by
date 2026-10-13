package github.sushkpavel.studentbsuby

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.navigation.compose.rememberNavController
import github.sushkpavel.studentbsuby.navigation.Route
import github.sushkpavel.studentbsuby.services.lock.AppLockManager
import github.sushkpavel.studentbsuby.ui.screens.MainScreen
import github.sushkpavel.studentbsuby.ui.screens.UpdateRequiredDialog
import github.sushkpavel.studentbsuby.ui.screens.lock.AppLockEffects
import github.sushkpavel.studentbsuby.ui.screens.lock.AppLockOverlay
import github.sushkpavel.studentbsuby.ui.theme.StudentbsubyTheme
import github.sushkpavel.studentbsuby.util.communication.collectAsState
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.runtime.collectAsState as collectFlowAsState

@Composable
fun App() {

    val mainActivityViewModel = koinViewModel<MainActivityViewModel>()
    val appLock = koinInject<AppLockManager>()

    LaunchedEffect(Unit) {
        mainActivityViewModel.handle(MainActivityEvent.Initialized)
    }

    val locked by appLock.locked.collectFlowAsState()

    StudentbsubyTheme(lightSystemBarIcons = locked) {
        val navController = rememberNavController()

        AppLockEffects(appLock = appLock, navController = navController)

        Box(Modifier.fillMaxSize()) {
            Box(if (locked) Modifier.clearAndSetSemantics { } else Modifier) {
                MainScreen(navController)
            }
            AppLockOverlay(
                appLock = appLock,
                onLoggedOut = {
                    navController.navigate(Route.AuthScreen.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }

        if (mainActivityViewModel.showUpdateDialog.collectAsState().value){
            UpdateRequiredDialog(mainActivityViewModel)
        }
    }
}
