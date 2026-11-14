package com.example.data.repository

import android.content.Context
import com.example.data.local.LocalPreferencesStore
import com.example.data.model.ConnectedDevice
import com.example.data.model.SyncedDocumentsSnapshot
import com.example.data.model.UserSettings
import com.example.data.model.WorkspaceDocument
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

interface WorkspaceDataSource {
    val isCloudBacked: Boolean
    fun observeUserSettings(): Flow<UserSettings?>
    suspend fun saveUserSettings(settings: UserSettings): Result<Unit>
    fun observeDocuments(): Flow<SyncedDocumentsSnapshot>
    suspend fun createDocument(
        title: String,
        content: String,
        category: String,
        isPinned: Boolean,
        deviceName: String,
        documentId: String? = null
    ): Result<String>

    suspend fun updateDocument(
        docId: String,
        title: String,
        content: String,
        category: String,
        isPinned: Boolean,
        deviceName: String
    ): Result<Unit>

    suspend fun deleteDocument(docId: String): Result<Unit>
    fun observeConnectedDevices(): Flow<List<ConnectedDevice>>
    suspend fun registerOrHeartbeatDevice(
        deviceId: String,
        deviceName: String,
        manufacturer: String,
        formFactor: String,
        osVersion: String,
        isLowRamDevice: Boolean
    ): Result<Unit>
}

class FirestoreWorkspaceDataSource(
    private val repository: WorkspaceRepository,
    private val userId: String
) : WorkspaceDataSource {
    override val isCloudBacked: Boolean = true

    override fun observeUserSettings(): Flow<UserSettings?> =
        repository.observeUserSettings(userId)

    override suspend fun saveUserSettings(settings: UserSettings): Result<Unit> =
        repository.saveUserSettings(userId, settings)

    override fun observeDocuments(): Flow<SyncedDocumentsSnapshot> =
        repository.observeDocuments(userId)

    override suspend fun createDocument(
        title: String,
        content: String,
        category: String,
        isPinned: Boolean,
        deviceName: String,
        documentId: String?
    ): Result<String> = repository.createDocument(
        userId = userId,
        title = title,
        content = content,
        category = category,
        isPinned = isPinned,
        deviceName = deviceName,
        documentId = documentId
    )

    override suspend fun updateDocument(
        docId: String,
        title: String,
        content: String,
        category: String,
        isPinned: Boolean,
        deviceName: String
    ): Result<Unit> = repository.updateDocument(
        userId = userId,
        docId = docId,
        title = title,
        content = content,
        category = category,
        isPinned = isPinned,
        deviceName = deviceName
    )

    override suspend fun deleteDocument(docId: String): Result<Unit> =
        repository.deleteDocument(userId, docId)

    override fun observeConnectedDevices(): Flow<List<ConnectedDevice>> =
        repository.observeConnectedDevices(userId)

    override suspend fun registerOrHeartbeatDevice(
        deviceId: String,
        deviceName: String,
        manufacturer: String,
        formFactor: String,
        osVersion: String,
        isLowRamDevice: Boolean
    ): Result<Unit> = repository.registerOrHeartbeatDevice(
        userId = userId,
        deviceId = deviceId,
        deviceName = deviceName,
        manufacturer = manufacturer,
        formFactor = formFactor,
        osVersion = osVersion,
        isLowRamDevice = isLowRamDevice
    )
}

class GuestLocalWorkspaceDataSource(
    context: Context,
    private val localPrefs: LocalPreferencesStore
) : WorkspaceDataSource {

    override val isCloudBacked: Boolean = false

    private val docsFile = File(context.applicationContext.filesDir, "guest_workspace_docs.json")
    private val mutex = Mutex()
    private val docsFlow = MutableStateFlow<List<WorkspaceDocument>>(readDocsFromDiskSync())
    private val deviceFlow = MutableStateFlow<List<ConnectedDevice>>(emptyList())

    override fun observeUserSettings(): Flow<UserSettings?> {
        return localPrefs.localStateFlow.map { local ->
            UserSettings(
                userId = GUEST_USER_ID,
                displayName = "Guest User",
                email = null,
                themeMode = local.themeMode.key,
                accentPalette = local.accentPalette.key,
                fontScale = local.fontScale.toDouble(),
                reduceMotion = local.reduceMotion,
                highContrast = local.highContrast,
                largeTargets = local.largeTargets,
                liteMode = local.liteMode,
                pinnedApps = local.pinnedApps
            )
        }
    }

    override suspend fun saveUserSettings(settings: UserSettings): Result<Unit> {
        localPrefs.saveFromUserSettings(settings)
        return Result.success(Unit)
    }

    override fun observeDocuments(): Flow<SyncedDocumentsSnapshot> {
        return docsFlow.map { list ->
            SyncedDocumentsSnapshot(
                documents = list.sortedWith(
                    compareByDescending<WorkspaceDocument> { it.isPinned }
                        .thenByDescending { it.updatedAt?.seconds ?: 0L }
                ),
                hasPendingWrites = false,
                isFromCache = true
            )
        }
    }

    override suspend fun createDocument(
        title: String,
        content: String,
        category: String,
        isPinned: Boolean,
        deviceName: String,
        documentId: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val id = documentId ?: "guest_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val now = Timestamp.now()
            val doc = WorkspaceDocument(
                id = id,
                userId = GUEST_USER_ID,
                title = title.take(200),
                content = content.take(100000),
                category = category.ifBlank { "Notes" }.take(40),
                isPinned = isPinned,
                wordCount = WorkspaceRepository.countWords(content).toLong(),
                lastEditedDevice = deviceName.ifBlank { "Local Device" }.take(80),
                createdAt = now,
                updatedAt = now
            )
            val updated = docsFlow.value.filterNot { it.id == id } + doc
            writeDocsToDisk(updated)
            docsFlow.value = updated
            Result.success(id)
        }
    }

    override suspend fun updateDocument(
        docId: String,
        title: String,
        content: String,
        category: String,
        isPinned: Boolean,
        deviceName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val now = Timestamp.now()
            val updated = docsFlow.value.map { doc ->
                if (doc.id == docId) {
                    doc.copy(
                        title = title.take(200),
                        content = content.take(100000),
                        category = category.ifBlank { "Notes" }.take(40),
                        isPinned = isPinned,
                        wordCount = WorkspaceRepository.countWords(content).toLong(),
                        lastEditedDevice = deviceName.ifBlank { "Local Device" }.take(80),
                        updatedAt = now
                    )
                } else {
                    doc
                }
            }
            writeDocsToDisk(updated)
            docsFlow.value = updated
            Result.success(Unit)
        }
    }

    override suspend fun deleteDocument(docId: String): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val updated = docsFlow.value.filterNot { it.id == docId }
            writeDocsToDisk(updated)
            docsFlow.value = updated
            Result.success(Unit)
        }
    }

    override fun observeConnectedDevices(): Flow<List<ConnectedDevice>> = deviceFlow

    override suspend fun registerOrHeartbeatDevice(
        deviceId: String,
        deviceName: String,
        manufacturer: String,
        formFactor: String,
        osVersion: String,
        isLowRamDevice: Boolean
    ): Result<Unit> {
        val now = Timestamp.now()
        deviceFlow.value = listOf(
            ConnectedDevice(
                id = deviceId,
                userId = GUEST_USER_ID,
                deviceName = "$deviceName (Guest Local)",
                manufacturer = manufacturer,
                formFactor = formFactor,
                osVersion = osVersion,
                isLowRamDevice = isLowRamDevice,
                createdAt = now,
                updatedAt = now
            )
        )
        return Result.success(Unit)
    }

    private fun readDocsFromDiskSync(): List<WorkspaceDocument> {
        if (!docsFile.exists()) return emptyList()
        return try {
            val arr = JSONArray(docsFile.readText())
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val createdSec = obj.optLong("createdAtSec", 0L)
                    val updatedSec = obj.optLong("updatedAtSec", createdSec)
                    add(
                        WorkspaceDocument(
                            id = obj.getString("id"),
                            userId = GUEST_USER_ID,
                            title = obj.optString("title", ""),
                            content = obj.optString("content", ""),
                            category = obj.optString("category", "Notes"),
                            isPinned = obj.optBoolean("isPinned", false),
                            wordCount = obj.optLong("wordCount", 0L),
                            lastEditedDevice = obj.optString("lastEditedDevice", "Local Device"),
                            createdAt = Timestamp(createdSec, 0),
                            updatedAt = Timestamp(updatedSec, 0)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writeDocsToDisk(docs: List<WorkspaceDocument>) {
        val arr = JSONArray()
        docs.forEach { doc ->
            arr.put(
                JSONObject().apply {
                    put("id", doc.id)
                    put("title", doc.title)
                    put("content", doc.content)
                    put("category", doc.category)
                    put("isPinned", doc.isPinned)
                    put("wordCount", doc.wordCount)
                    put("lastEditedDevice", doc.lastEditedDevice)
                    put("createdAtSec", doc.createdAt?.seconds ?: 0L)
                    put("updatedAtSec", doc.updatedAt?.seconds ?: 0L)
                }
            )
        }
        docsFile.writeText(arr.toString())
    }

    companion object {
        const val GUEST_USER_ID = "guest_local"
    }
}
