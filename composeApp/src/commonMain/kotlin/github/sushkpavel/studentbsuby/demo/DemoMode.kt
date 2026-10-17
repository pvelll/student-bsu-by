package github.sushkpavel.studentbsuby.demo

import com.russhwolf.settings.ObservableSettings
import kotlin.concurrent.Volatile

/**
 * Whether the signed in account is the demo one. Kept with the credentials, so a restored
 * session stays in the demo mode until the logout.
 */
class DemoMode(private val settings: ObservableSettings) {

    @Volatile
    var isEnabled: Boolean = settings.getBoolean(KEY, false)
        private set

    fun enable() = set(true)

    fun disable() = set(false)

    private fun set(enabled: Boolean) {
        if (enabled == isEnabled)
            return
        settings.putBoolean(KEY, enabled)
        isEnabled = enabled
    }

    private companion object {
        const val KEY = "demoMode"
    }
}
