package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AccentPalette
import com.example.data.model.ThemeMode
import com.example.data.model.UserSettings
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class WorkspaceRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveAndObserveUserSettings_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = WorkspaceRepository(firestore)

        val settings = UserSettings(
            userId = uid,
            displayName = "Alice",
            email = ALICE_EMAIL,
            themeMode = ThemeMode.AMOLED.key,
            accentPalette = AccentPalette.AURORA_MINT.key,
            fontScale = 1.15,
            reduceMotion = true,
            highContrast = false,
            largeTargets = true,
            liteMode = true,
            pinnedApps = listOf("builtin:docs", "builtin:notes")
        )

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveUserSettings(uid, settings)
        }
        assertTrue("Expected saveUserSettings to succeed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserSettings(uid).first { it != null }
        }
        assertNotNull(observed)
        assertEquals(ThemeMode.AMOLED.key, observed?.themeMode)
        assertEquals(true, observed?.liteMode)
    }

    @Test
    fun createUpdateDeleteDocument_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = WorkspaceRepository(firestore)
        val docId = "doc_${UUID.randomUUID().toString().replace("-", "_")}"

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createDocument(
                userId = uid,
                title = "Offline Architecture",
                content = "Auto-save buffer ready.",
                category = "Notes",
                isPinned = true,
                deviceName = "Dell Chromebook 11",
                documentId = docId
            )
        }
        assertTrue("Create failed: ${createResult.exceptionOrNull()}", createResult.isSuccess)

        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.updateDocument(
                userId = uid,
                docId = docId,
                title = "Offline Architecture v2",
                content = "Auto-save buffer synced across devices.",
                category = "Notes",
                isPinned = true,
                deviceName = "Dell Chromebook 11"
            )
        }
        assertTrue("Update failed: ${updateResult.exceptionOrNull()}", updateResult.isSuccess)

        val docs = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeDocuments(uid).first { snap ->
                snap.documents.any { it.id == docId && it.title == "Offline Architecture v2" }
            }
        }
        assertTrue(docs.documents.any { it.id == docId })

        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.deleteDocument(uid, docId)
        }
        assertTrue("Delete failed: ${deleteResult.exceptionOrNull()}", deleteResult.isSuccess)
    }

    @Test
    fun registerDevice_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = WorkspaceRepository(firestore)
        val devId = "dev_${UUID.randomUUID().toString().replace("-", "_")}"

        val res = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.registerOrHeartbeatDevice(
                userId = uid,
                deviceId = devId,
                deviceName = "Dell Chromebook 11",
                manufacturer = "Dell",
                formFactor = "chromebook",
                osVersion = "Android 14",
                isLowRamDevice = true
            )
        }
        assertTrue("Device registration failed: ${res.exceptionOrNull()}", res.isSuccess)

        val devices = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeConnectedDevices(uid).first { list -> list.any { it.id == devId } }
        }
        assertEquals("chromebook", devices.first { it.id == devId }.formFactor)
    }

    @Test
    fun crossUserAccess_documents_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = WorkspaceRepository(firestore)
        val docId = "alice_${UUID.randomUUID().toString().replace("-", "_")}"

        val createRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createDocument(
                userId = aliceUid,
                title = "Alice Private Doc",
                content = "Top secret",
                category = "Notes",
                isPinned = false,
                deviceName = "Pixel 8",
                documentId = docId
            )
        }
        assertTrue(createRes.isSuccess)

        signInTestUser(BOB_EMAIL)
        val bobRepo = WorkspaceRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeDocuments(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED for cross-user read")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    @Test
    fun unauthenticatedUser_observeDocuments_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repo = WorkspaceRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeDocuments(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED when unauthenticated")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_flexdesk@test.com"
        const val BOB_EMAIL = "bob_flexdesk@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
