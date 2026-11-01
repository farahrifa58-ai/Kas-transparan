package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AppSettings
import com.example.data.model.Transaction
import com.example.data.repository.KasRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test

class KasRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun addTransaction_authenticatedUser_success() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = KasRepository(firestore)

        val transaction = Transaction(
            userId = uid,
            type = "INCOME",
            title = "Iuran Kas Bulanan",
            amount = 500000,
            category = "Iuran Warga",
            date = "2026-11-01",
            description = "Iuran blok A",
            paymentMethod = "Tunai"
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) { repository.addTransaction(transaction) }
        assertTrue(result.isSuccess)
        val docId = result.getOrThrow()
        assertTrue(docId.isNotEmpty())

        // Verify transaction is observable
        val list = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeTransactions(uid).first()
        }
        assertTrue(list.any { it.title == "Iuran Kas Bulanan" })
    }

    @Test
    fun saveSettings_and_observeSettings_success() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = KasRepository(firestore)

        val settings = AppSettings(
            userId = uid,
            groupName = "Kas RT 01 RW 05",
            adminName = "Budi Santoso",
            initialBalance = 1000000,
            currencyCode = "IDR",
            language = "id"
        )

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveSettings(settings) }
        assertTrue(saveResult.isSuccess)

        val loaded = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeSettings(uid).first()
        }
        assertEquals("Kas RT 01 RW 05", loaded?.groupName)
    }

    @Test
    fun observeTransactions_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = KasRepository(firestore)

        var caughtException: Exception? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.observeTransactions("random_user_123").first()
            }
        } catch (e: Exception) {
            caughtException = e
        }
        assertNotNull(caughtException)
        val firestoreException = checkNotNull(caughtException?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $caughtException"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@example.com"
        const val DEFAULT_TIMEOUT_MS = 8000L
    }
}
