package github.sushkpavel.studentbsuby.util

import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun migrateLegacyCookies(preferences: SharedPreferences) {
    runCatching {
        val legacy = preferences.all.mapNotNull { entry ->
            (entry.value as? Set<*>)?.let { set ->
                entry.key to set.filterIsInstance<String>()
            }
        }
        if (legacy.isEmpty())
            return

        val editor = preferences.edit()
        legacy.forEach { (key, cookies) ->
            editor.putString(key, Json.encodeToString(cookies))
        }
        editor.apply()
    }
}
