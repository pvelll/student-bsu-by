package github.sushkpavel.studentbsuby.demo

import okio.ByteString.Companion.encodeUtf8

/**
 * The account for store reviewers, who cannot reach student.bsu.by from outside Belarus.
 *
 * The repository is public, so only a digest of the credentials is kept here: the login and
 * password are handed to the stores and kept in demo-account.properties, outside of git
 * (see RELEASE.md for changing them).
 */
class DemoAccount(private val expectedDigest: String = DIGEST) {

    fun matches(login: String, password: String): Boolean =
        login.isNotBlank() && password.isNotBlank() && digest(login, password) == expectedDigest

    companion object {
        private const val DIGEST = "df37d846a3270180fa1ef76488feebfb5900e9cd556e46b44497aa077bc894cd"
        private const val SALT = "studentbsuby-demo"

        // The login is case insensitive: keyboards capitalize the first letter.
        fun digest(login: String, password: String): String =
            "$SALT\n${login.trim().lowercase()}\n${password.trim()}".encodeUtf8().sha256().hex()
    }
}
