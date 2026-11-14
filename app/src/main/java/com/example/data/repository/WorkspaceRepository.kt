package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ConnectedDevice
import com.example.data.model.SyncedDocumentsSnapshot
import com.example.data.model.UserSettings
import com.example.data.model.WorkspaceDocument
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class WorkspaceRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        provideConfiguredFirestore(context.applicationContext)
    )

    fun observeUserSettings(userId: String): Flow<UserSettings?> {
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        UserSettings::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                logIfFirestoreError(error, OperationType.GET, docRef.path, userId)
                throw error
            }
    }

    suspend fun getUserSettings(userId: String): Result<UserSettings?> {
        val docRef = db.collection("users").document(userId)
        return try {
            val snapshot = docRef.get().await()
            val settings = if (snapshot.exists()) {
                snapshot.toObject(
                    UserSettings::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                null
            }
            Result.success(settings)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveUserSettings(userId: String, settings: UserSettings): Result<Unit> {
        val docRef = db.collection("users").document(userId)
        return try {
            val existingSnap = docRef.get().await()
            val safePinned = settings.pinnedApps.take(20)
            val clampedScale = settings.fontScale.coerceIn(0.8, 1.5)
            if (!existingSnap.exists()) {
                val payload = mutableMapOf<String, Any>(
                    "userId" to userId,
                    "themeMode" to settings.themeMode,
                    "accentPalette" to settings.accentPalette,
                    "fontScale" to clampedScale,
                    "reduceMotion" to settings.reduceMotion,
                    "highContrast" to settings.highContrast,
                    "largeTargets" to settings.largeTargets,
                    "liteMode" to settings.liteMode,
                    "pinnedApps" to safePinned,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                settings.displayName?.takeIf { it.isNotBlank() }?.let {
                    payload["displayName"] = it.take(100)
                }
                settings.email?.takeIf { it.isNotBlank() }?.let {
                    payload["email"] = it.take(160)
                }
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "themeMode" to settings.themeMode,
                    "accentPalette" to settings.accentPalette,
                    "fontScale" to clampedScale,
                    "reduceMotion" to settings.reduceMotion,
                    "highContrast" to settings.highContrast,
                    "largeTargets" to settings.largeTargets,
                    "liteMode" to settings.liteMode,
                    "pinnedApps" to safePinned,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                settings.displayName?.takeIf { it.isNotBlank() }?.let {
                    updates["displayName"] = it.take(100)
                }
                settings.email?.takeIf { it.isNotBlank() }?.let {
                    updates["email"] = it.take(160)
                }
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeDocuments(userId: String): Flow<SyncedDocumentsSnapshot> {
        val colRef = db.collection("users").document(userId).collection("documents")
        return colRef.snapshots(MetadataChanges.INCLUDE)
            .map { snapshot ->
                val docs = snapshot.toObjects(
                    WorkspaceDocument::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(
                    compareByDescending<WorkspaceDocument> { it.isPinned }
                        .thenByDescending { it.updatedAt?.seconds ?: 0L }
                )
                SyncedDocumentsSnapshot(
                    documents = docs,
                    hasPendingWrites = snapshot.metadata.hasPendingWrites(),
                    isFromCache = snapshot.metadata.isFromCache
                )
            }
            .catch { error ->
                logIfFirestoreError(error, OperationType.LIST, colRef.path, userId)
                throw error
            }
    }

    suspend fun createDocument(
        userId: String,
        title: String,
        content: String,
        category: String,
        isPinned: Boolean,
        deviceName: String,
        documentId: String? = null
    ): Result<String> {
        val colRef = db.collection("users").document(userId).collection("documents")
        val docRef = if (documentId != null) colRef.document(documentId) else colRef.document()
        val safeTitle = title.take(200)
        val safeContent = content.take(100000)
        val safeCategory = category.ifBlank { "Notes" }.take(40)
        val safeDevice = deviceName.ifBlank { "Android Device" }.take(80)
        val wordCount = countWords(safeContent).toLong()

        val payload = mapOf(
            "userId" to userId,
            "title" to safeTitle,
            "content" to safeContent,
            "category" to safeCategory,
            "isPinned" to isPinned,
            "wordCount" to wordCount,
            "lastEditedDevice" to safeDevice,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateDocument(
        userId: String,
        docId: String,
        title: String,
        content: String,
        category: String,
        isPinned: Boolean,
        deviceName: String
    ): Result<Unit> {
        require(docId.isNotBlank()) { "Document ID must not be blank" }
        val docRef = db.collection("users").document(userId).collection("documents").document(docId)
        val safeTitle = title.take(200)
        val safeContent = content.take(100000)
        val safeCategory = category.ifBlank { "Notes" }.take(40)
        val safeDevice = deviceName.ifBlank { "Android Device" }.take(80)
        val wordCount = countWords(safeContent).toLong()

        val updates = mapOf(
            "title" to safeTitle,
            "content" to safeContent,
            "category" to safeCategory,
            "isPinned" to isPinned,
            "wordCount" to wordCount,
            "lastEditedDevice" to safeDevice,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteDocument(userId: String, docId: String): Result<Unit> {
        require(docId.isNotBlank()) { "Document ID must not be blank" }
        val docRef = db.collection("users").document(userId).collection("documents").document(docId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeConnectedDevices(userId: String): Flow<List<ConnectedDevice>> {
        val colRef = db.collection("users").document(userId).collection("devices")
        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ConnectedDevice::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt?.seconds ?: 0L }
            }
            .catch { error ->
                logIfFirestoreError(error, OperationType.LIST, colRef.path, userId)
                throw error
            }
    }

    suspend fun registerOrHeartbeatDevice(
        userId: String,
        deviceId: String,
        deviceName: String,
        manufacturer: String,
        formFactor: String,
        osVersion: String,
        isLowRamDevice: Boolean
    ): Result<Unit> {
        val safeId = deviceId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128).ifBlank { "device_default" }
        val docRef = db.collection("users").document(userId).collection("devices").document(safeId)
        val safeName = deviceName.ifBlank { "Android Device" }.take(80)
        val safeMfg = manufacturer.take(60)
        val safeFactor = if (formFactor in setOf("phone", "tablet", "chromebook")) formFactor else "phone"
        val safeOs = osVersion.take(40)

        return try {
            val existingSnap = docRef.get().await()
            if (!existingSnap.exists()) {
                val payload = mapOf(
                    "userId" to userId,
                    "deviceName" to safeName,
                    "manufacturer" to safeMfg,
                    "formFactor" to safeFactor,
                    "osVersion" to safeOs,
                    "isLowRamDevice" to isLowRamDevice,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updates = mapOf(
                    "deviceName" to safeName,
                    "manufacturer" to safeMfg,
                    "formFactor" to safeFactor,
                    "osVersion" to safeOs,
                    "isLowRamDevice" to isLowRamDevice,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    private fun logIfFirestoreError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception) ?: return
        val currentUid = try {
            Firebase.auth.currentUser?.uid
        } catch (_: Exception) {
            null
        }
        if (currentUid == null || currentUid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }

    companion object {
        fun countWords(text: String): Int {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return 0
            return trimmed.split(Regex("\\s+")).size.coerceAtMost(50000)
        }

        fun provideConfiguredFirestore(context: Context): FirebaseFirestore {
            val databaseId = context.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            try {
                val cacheSettings = PersistentCacheSettings.newBuilder()
                    .setSizeBytes(50L * 1024L * 1024L)
                    .build()
                db.firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(cacheSettings)
                    .build()
            } catch (_: IllegalStateException) {
                // Firestore settings already initialized for this instance
            }
            return db
        }
    }
}
