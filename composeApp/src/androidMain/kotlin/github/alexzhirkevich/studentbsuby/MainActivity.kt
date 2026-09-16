package github.alexzhirkevich.studentbsuby

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import github.alexzhirkevich.studentbsuby.util.CurrentActivityHolder

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate: keeps the launch splash (logo on the brand
        // background) until the first frame is drawn and then swaps in the app theme.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        CurrentActivityHolder.set(this)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CurrentActivityHolder.clear(this)
    }
}
