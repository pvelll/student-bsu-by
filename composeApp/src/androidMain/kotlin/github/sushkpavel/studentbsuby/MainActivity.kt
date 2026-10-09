package github.sushkpavel.studentbsuby

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import github.sushkpavel.studentbsuby.util.CurrentActivityHolder

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
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
