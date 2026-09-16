package github.alexzhirkevich.studentbsuby.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SharedPreferencesSettings
import java.io.IOException
import javax.crypto.AEADBadTagException
import javax.crypto.BadPaddingException

private const val TAG = "Settings"
private const val PLAIN_SUFFIX = "_plain"
private const val DEFAULT_PREFERENCES_KEY = "\$default"
private const val PREF_LEGACY_CREDENTIALS_SCRUBBED = "legacy_plain_credentials_scrubbed"

/**
 * Keys that may only live in the secure stores. They ended up in the default
 * preferences when [EncryptedSharedPreferences] failed to open and the previous
 * implementation silently fell back to the default file — and were then wiped by the
 * legacy-credentials scrub on the next access, which left the app logged in without a
 * username.
 */
private val secureOnlyKeys = listOf("username", "password", "autoLogin", "student.bsu.by")

private val settingsLock = Any()
private val sharedPreferencesCache = mutableMapOf<String, SharedPreferences>()

actual fun provideSettings(name: String): ObservableSettings =
    SharedPreferencesSettings(
        AndroidAppContext.context.getSharedPreferences(name, Context.MODE_PRIVATE)
    )

actual fun provideSecureSettings(name: String): ObservableSettings =
    SharedPreferencesSettings(provideSecureSharedPreferences(name))

actual fun provideDefaultSettings(): ObservableSettings =
    SharedPreferencesSettings(defaultSharedPreferences(AndroidAppContext.context))

/**
 * Encrypted preferences file [name], created once per process.
 *
 * [EncryptedSharedPreferences.create] is expensive and must not run concurrently for the
 * same file: parallel first-time initialisation generates the master key twice and leaves
 * a keyset that can never be decrypted again. Instances are therefore cached and creation
 * is serialized.
 *
 * A keyset that can not be decrypted any more is unrecoverable, so the store is
 * recreated (its content is lost either way). Only if the store still can not be opened
 * (e.g. a transient keystore failure) a dedicated plain file (`<name>_plain`) is used, and
 * its content is moved back into the encrypted store as soon as that opens again.
 */
fun provideSecureSharedPreferences(name: String): SharedPreferences = synchronized(settingsLock) {
    sharedPreferencesCache.getOrPut(name) {
        openSecurePreferences(AndroidAppContext.context, name)
    }
}

private fun openSecurePreferences(context: Context, name: String): SharedPreferences {
    val plainName = name + PLAIN_SUFFIX
    var failure: Throwable? = null

    // Attempts 1 and 2 open the existing store (a transient keystore hiccup is retried);
    // only a keyset that proved undecryptable twice is considered lost and the store is
    // recreated for the 3rd attempt.
    repeat(3) { attempt ->
        try {
            val preferences = createEncryptedPrefs(name, context)
            migratePlainFallback(context, plainName, preferences)
            return preferences
        } catch (t: Throwable) {
            failure = t
            Log.w(TAG, "Failed to open encrypted preferences '$name' (attempt ${attempt + 1})", t)
            when {
                attempt == 1 && t.isUnrecoverableKeyset() -> {
                    Log.w(TAG, "Keyset of '$name' can not be decrypted, recreating the store")
                    kotlin.runCatching { context.deleteSharedPreferences(name) }
                }
                attempt < 2 -> Thread.sleep(150)
            }
        }
    }

    Log.e(TAG, "Encrypted preferences '$name' are unavailable, using plain fallback file", failure)
    return context.getSharedPreferences(plainName, Context.MODE_PRIVATE)
}

private fun Throwable.isUnrecoverableKeyset(): Boolean =
    generateSequence(this) { it.cause }.any {
        it is AEADBadTagException ||
                it is BadPaddingException ||
                it.javaClass.name.endsWith("InvalidProtocolBufferException") ||
                (it is IOException && it.message?.contains("keyset", ignoreCase = true) == true)
    }

private fun migratePlainFallback(context: Context, plainName: String, target: SharedPreferences) {
    val plain = context.getSharedPreferences(plainName, Context.MODE_PRIVATE)
    val entries = plain.all
    if (entries.isEmpty())
        return

    kotlin.runCatching {
        target.edit().apply {
            entries.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                    is Set<*> -> putStringSet(key, value.filterIsInstance<String>().toSet())
                }
            }
        }.commit()
        plain.edit().clear().commit()
        Log.i(TAG, "Moved ${entries.size} entries from '$plainName' into the encrypted store")
    }.onFailure {
        Log.w(TAG, "Failed to migrate the plain fallback '$plainName'", it)
    }
}

private fun createEncryptedPrefs(name : String, context: Context) =
    EncryptedSharedPreferences.create(
        context,
        name,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setUserAuthenticationRequired(false)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

/**
 * The default preferences file. Credentials stored in plain text by very old versions of
 * the app (and the leftovers of the encrypted-preferences fallback described above) are
 * removed exactly once.
 */
fun defaultSharedPreferences(context: Context) : SharedPreferences = synchronized(settingsLock) {
    sharedPreferencesCache.getOrPut(DEFAULT_PREFERENCES_KEY) {
        context.getSharedPreferences(
            context.packageName + "_preferences",
            Context.MODE_PRIVATE
        ).also { prefs ->
            kotlin.runCatching {
                if (!prefs.getBoolean(PREF_LEGACY_CREDENTIALS_SCRUBBED, false)) {
                    prefs.edit().apply {
                        secureOnlyKeys.forEach(::remove)
                        putBoolean(PREF_LEGACY_CREDENTIALS_SCRUBBED, true)
                    }.apply()
                }
            }
        }
    }
}
