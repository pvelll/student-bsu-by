package github.sushkpavel.studentbsuby.services.lock

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import github.sushkpavel.studentbsuby.util.CurrentActivityHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/** Biometry with a fallback to the device PIN / pattern / password. */
private const val Authenticators = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

/**
 * androidx.biometric prompt. Supported from Android 9: on older versions the library
 * shows its own fingerprint dialog, which requires an AppCompat theme the app does not use.
 */
class BiometricAuthenticatorImpl(
    private val context: Context,
) : BiometricAuthenticator {

    private val isSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    override fun biometryType(): BiometryType {
        if (!isSupported ||
            BiometricManager.from(context).canAuthenticate(BIOMETRIC_WEAK) != BiometricManager.BIOMETRIC_SUCCESS
        ) return BiometryType.None

        val packageManager = context.packageManager
        val fingerprint = packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        val face = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                packageManager.hasSystemFeature(PackageManager.FEATURE_FACE)
        val iris = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                packageManager.hasSystemFeature(PackageManager.FEATURE_IRIS)

        return when {
            fingerprint && !face && !iris -> BiometryType.Fingerprint
            face && !fingerprint && !iris -> BiometryType.Face
            else -> BiometryType.Generic
        }
    }

    override fun canAuthenticate(): Boolean = isSupported &&
            BiometricManager.from(context).canAuthenticate(Authenticators) == BiometricManager.BIOMETRIC_SUCCESS

    override suspend fun authenticate(title: String, subtitle: String): AuthResult =
        withContext(Dispatchers.Main) {
            val activity = CurrentActivityHolder.activity as? FragmentActivity
                ?: return@withContext AuthResult.Failed(null)

            suspendCancellableCoroutine<AuthResult> { continuation ->
                val executor = ContextCompat.getMainExecutor(activity)
                val prompt = BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            if (continuation.isActive)
                                continuation.resume(AuthResult.Success)
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            if (!continuation.isActive)
                                return
                            continuation.resume(
                                when (errorCode) {
                                    BiometricPrompt.ERROR_USER_CANCELED,
                                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                                    BiometricPrompt.ERROR_CANCELED -> AuthResult.Cancelled
                                    else -> AuthResult.Failed(errString.toString())
                                }
                            )
                        }

                        // A single unrecognised attempt: the prompt stays open and lets the
                        // user try again, nothing to report yet.
                        override fun onAuthenticationFailed() = Unit
                    }
                )

                prompt.authenticate(
                    BiometricPrompt.PromptInfo.Builder()
                        .setTitle(title)
                        .setSubtitle(subtitle)
                        .setAllowedAuthenticators(Authenticators)
                        .setConfirmationRequired(false)
                        .build()
                )

                continuation.invokeOnCancellation {
                    executor.execute { prompt.cancelAuthentication() }
                }
            }
        }
}
