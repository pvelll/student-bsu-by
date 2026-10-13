@file:OptIn(ExperimentalForeignApi::class)

package github.sushkpavel.studentbsuby.services.lock

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.LocalAuthentication.LABiometryTypeFaceID
import platform.LocalAuthentication.LABiometryTypeNone
import platform.LocalAuthentication.LABiometryTypeTouchID
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAErrorAppCancel
import platform.LocalAuthentication.LAErrorSystemCancel
import platform.LocalAuthentication.LAErrorUserCancel
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import kotlin.coroutines.resume

class BiometricAuthenticatorImpl : BiometricAuthenticator {

    override fun biometryType(): BiometryType {
        val context = LAContext()
        if (!context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null))
            return BiometryType.None

        return when (context.biometryType) {
            LABiometryTypeFaceID -> BiometryType.Face
            LABiometryTypeTouchID -> BiometryType.Fingerprint
            LABiometryTypeNone -> BiometryType.None
            else -> BiometryType.Generic
        }
    }

    override fun canAuthenticate(): Boolean =
        LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, null)

    override suspend fun authenticate(title: String, subtitle: String): AuthResult =
        suspendCancellableCoroutine<AuthResult> { continuation ->
            val context = LAContext()
            context.evaluatePolicy(LAPolicyDeviceOwnerAuthentication, subtitle) { success, error ->
                if (!continuation.isActive)
                    return@evaluatePolicy
                continuation.resume(
                    when {
                        success -> AuthResult.Success
                        error?.code in CancelCodes -> AuthResult.Cancelled
                        else -> AuthResult.Failed(error?.localizedDescription)
                    }
                )
            }
            continuation.invokeOnCancellation { context.invalidate() }
        }

    private companion object {
        val CancelCodes = setOf(LAErrorUserCancel, LAErrorSystemCancel, LAErrorAppCancel)
    }
}
