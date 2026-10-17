package github.sushkpavel.studentbsuby.demo

import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DemoAccountTest {

    private val account = DemoAccount(DemoAccount.digest("reviewer", "Secret-Pass-1"))

    @Test
    fun `the demo credentials are accepted`() {
        assertTrue(account.matches("reviewer", "Secret-Pass-1"))
    }

    @Test
    fun `the login is case insensitive and both fields are trimmed`() {
        assertTrue(account.matches(" Reviewer", "Secret-Pass-1 "))
    }

    @Test
    fun `the password is case sensitive`() {
        assertFalse(account.matches("reviewer", "secret-pass-1"))
    }

    @Test
    fun `other and blank credentials are rejected`() {
        assertFalse(account.matches("reviewer", "Secret-Pass-2"))
        assertFalse(account.matches("1234567", "Secret-Pass-1"))
        assertFalse(account.matches("", ""))
        assertFalse(DemoAccount(DemoAccount.digest(" ", " ")).matches(" ", " "))
    }

    @Test
    fun `the digest is sha256 of the salted credentials`() {
        val expected = MessageDigest.getInstance("SHA-256")
            .digest("studentbsuby-demo\nreviewer\nSecret-Pass-1".toByteArray())
            .joinToString("") { "%02x".format(it) }
        assertEquals(expected, DemoAccount.digest("Reviewer", "Secret-Pass-1"))
    }

    @Test
    fun `the release account cannot be guessed`() {
        val release = DemoAccount()
        listOf("demo", "test", "admin", "appreview", "review", "apple", "student").forEach { guess ->
            listOf(guess, "demo", "123456", "password").forEach { password ->
                assertFalse(release.matches(guess, password), "$guess / $password")
            }
        }
    }
}
