package github.sushkpavel.studentbsuby

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import github.sushkpavel.studentbsuby.ui.screens.MainScreen
import github.sushkpavel.studentbsuby.ui.screens.UpdateRequiredDialog
import github.sushkpavel.studentbsuby.ui.theme.StudentbsubyTheme
import github.sushkpavel.studentbsuby.util.communication.collectAsState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {

    val mainActivityViewModel = koinViewModel<MainActivityViewModel>()

    // The original activity sent Initialized from onCreate on every recreation;
    // here it fires on the first composition of a (re)created UI tree.
    // On iOS this happens once per application start.
    LaunchedEffect(Unit) {
        mainActivityViewModel.handle(MainActivityEvent.Initialized)
    }

    StudentbsubyTheme {
        MainScreen()
        if (mainActivityViewModel.showUpdateDialog.collectAsState().value){
            UpdateRequiredDialog(mainActivityViewModel)
        }
    }
}
