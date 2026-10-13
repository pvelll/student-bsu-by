package github.sushkpavel.studentbsuby.services.lock

enum class BiometryType {
    None,
    Fingerprint,
    Face,
    Generic,
}

sealed interface AuthResult {
    data object Success : AuthResult
    data object Cancelled : AuthResult
    data object Unavailable : AuthResult
    data class Failed(val message: String?) : AuthResult
}

interface BiometricAuthenticator {

    fun biometryType(): BiometryType

    fun canAuthenticate(): Boolean

    suspend fun authenticate(title: String, subtitle: String): AuthResult
}
