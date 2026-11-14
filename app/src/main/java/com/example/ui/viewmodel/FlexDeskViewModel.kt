package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.cache.IconCacheManager
import com.example.data.cache.IconCacheStats
import com.example.data.local.LocalControlState
import com.example.data.local.LocalPreferencesStore
import com.example.data.model.AccentPalette
import com.example.data.model.ConnectedDevice
import com.example.data.model.ThemeMode
import com.example.data.model.UserSettings
import com.example.data.model.WorkspaceDocument
import com.example.data.repository.WorkspaceDataSource
import com.example.data.system.BatteryStatus
import com.example.data.system.DeviceTelemetryManager
import com.example.data.system.HardwareProfile
import com.example.data.system.InstalledAndroidApp
import com.example.data.system.NetworkStatus
import com.example.ui.i18n.AppLanguage
import com.example.ui.state.ActiveDocumentDraft
import com.example.ui.state.AutoSaveStatus
import com.example.ui.state.BuiltinAppId
import com.example.ui.state.DesktopWindow
import com.example.ui.state.WindowSnapMode
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkspaceUiState(
    val userId: String = "",
    val isGuestMode: Boolean = false,
    val displayName: String = "",
    val email: String = "",
    val settings: LocalControlState = LocalControlState(),
    val documents: List<WorkspaceDocument> = emptyList(),
    val hasPendingWrites: Boolean = false,
    val isFromCache: Boolean = false,
    val connectedDevices: List<ConnectedDevice> = emptyList(),
    val hardwareProfile: HardwareProfile? = null,
    val networkStatus: NetworkStatus = NetworkStatus(isOnline = true, transportLabel = "Connected"),
    val batteryStatus: BatteryStatus = BatteryStatus(levelPercent = 100, isCharging = false),
    val installedApps: List<InstalledAndroidApp> = emptyList(),
    val iconCacheStats: IconCacheStats = IconCacheStats(),
    val isLauncherOpen: Boolean = true,
    val isControlCenterOpen: Boolean = false,
    val searchQuery: String = "",
    val selectedAppCategory: String = "All",
    val openWindows: List<DesktopWindow> = emptyList(),
    val focusedWindowId: String? = null,
    val activeDraft: ActiveDocumentDraft? = null,
    val autoSaveStatus: AutoSaveStatus = AutoSaveStatus.IDLE,
    val statusBannerMessage: String? = null,
    val currentDeviceId: String = ""
)

class FlexDeskViewModel(
    private val dataSource: WorkspaceDataSource,
    private val localPrefs: LocalPreferencesStore,
    val iconCache: IconCacheManager,
    private val telemetry: DeviceTelemetryManager,
    private val currentUserId: String,
    private val isGuestMode: Boolean,
    private val userDisplayName: String,
    private val userEmail: String
) : ViewModel() {

    private val _chromeState = MutableStateFlow(
        WorkspaceUiState(
            userId = currentUserId,
            isGuestMode = isGuestMode,
            displayName = userDisplayName.ifBlank { if (isGuestMode) "Guest User" else "FlexDesk User" },
            email = userEmail,
            hardwareProfile = telemetry.readHardwareProfile()
        )
    )

    private val _documents = MutableStateFlow<List<WorkspaceDocument>>(emptyList())
    private val _syncMeta = MutableStateFlow(false to false)
    private val _devices = MutableStateFlow<List<ConnectedDevice>>(emptyList())
    private val _installedApps = MutableStateFlow<List<InstalledAndroidApp>>(emptyList())

    val uiState: StateFlow<WorkspaceUiState> = combine(
        _chromeState,
        localPrefs.localStateFlow,
        _documents,
        _devices,
        combine(
            _syncMeta,
            telemetry.observeNetworkStatus(),
            telemetry.observeBatteryStatus(),
            iconCache.stats,
            _installedApps
        ) { syncMeta, net, bat, cacheStats, apps ->
            Tuple5(syncMeta, net, bat, cacheStats, apps)
        }
    ) { chrome, localSettings, docs, devices, systemTuple ->
        chrome.copy(
            settings = localSettings,
            documents = docs,
            hasPendingWrites = systemTuple.first.first,
            isFromCache = systemTuple.first.second,
            connectedDevices = devices,
            networkStatus = systemTuple.second,
            batteryStatus = systemTuple.third,
            iconCacheStats = systemTuple.fourth,
            installedApps = systemTuple.fifth
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = _chromeState.value
    )

    private var autoSaveJob: Job? = null
    private var lastAppliedRemoteSettings: UserSettings? = null

    init {
        bootstrapDeviceAndApps()
        observeRemoteUserSettings()
        observeRemoteDocuments()
        observeRemoteDevices()
    }

    private fun bootstrapDeviceAndApps() {
        viewModelScope.launch {
            val hw = telemetry.readHardwareProfile()
            val devId = localPrefs.getOrCreateDeviceId()
            _chromeState.update {
                it.copy(
                    hardwareProfile = hw,
                    currentDeviceId = devId
                )
            }
            // Auto-enable Lite mode suggestion if device reports low RAM on first setup
            val currentLocal = localPrefs.localStateFlow.first()
            iconCache.configureMemoryBudget(currentLocal.liteMode || hw.isLowRamDevice)

            dataSource.registerOrHeartbeatDevice(
                deviceId = devId,
                deviceName = hw.deviceName,
                manufacturer = hw.manufacturer,
                formFactor = hw.formFactor,
                osVersion = hw.osVersion,
                isLowRamDevice = hw.isLowRamDevice
            )

            val apps = telemetry.loadInstalledLauncherApps()
            _installedApps.value = apps
            iconCache.warmCacheFromDisk(apps.map { it.packageName })
        }
    }

    private fun observeRemoteUserSettings() {
        viewModelScope.launch {
            dataSource.observeUserSettings()
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    _chromeState.update {
                        it.copy(statusBannerMessage = fsErr?.message ?: "Using offline theme preferences")
                    }
                }
                .collectLatest { remote ->
                    if (remote != null) {
                        lastAppliedRemoteSettings = remote
                        if (dataSource.isCloudBacked) {
                            localPrefs.saveFromUserSettings(remote)
                        }
                        iconCache.configureMemoryBudget(remote.liteMode)
                    } else if (dataSource.isCloudBacked) {
                        val local = localPrefs.localStateFlow.first()
                        val hw = _chromeState.value.hardwareProfile
                        val seed = UserSettings(
                            userId = currentUserId,
                            displayName = userDisplayName.ifBlank { null },
                            email = userEmail.ifBlank { null },
                            themeMode = local.themeMode.key,
                            accentPalette = local.accentPalette.key,
                            fontScale = local.fontScale.toDouble(),
                            reduceMotion = local.reduceMotion || (hw?.isLowRamDevice == true),
                            highContrast = local.highContrast,
                            largeTargets = local.largeTargets,
                            liteMode = local.liteMode || (hw?.isLowRamDevice == true),
                            pinnedApps = local.pinnedApps
                        )
                        dataSource.saveUserSettings(seed)
                    }
                }
        }
    }

    private fun observeRemoteDocuments() {
        viewModelScope.launch {
            dataSource.observeDocuments()
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    _chromeState.update {
                        it.copy(statusBannerMessage = fsErr?.message ?: "Offline mode active")
                    }
                }
                .collectLatest { snap ->
                    _documents.value = snap.documents
                    _syncMeta.value = snap.hasPendingWrites to snap.isFromCache
                    if (snap.documents.isEmpty() && _chromeState.value.activeDraft == null) {
                        seedWelcomeDocumentIfEmpty()
                    }
                }
        }
    }

    private var seededOnce = false
    private fun seedWelcomeDocumentIfEmpty() {
        if (seededOnce) return
        seededOnce = true
        viewModelScope.launch {
            val hwName = _chromeState.value.hardwareProfile?.deviceName ?: "Android"
            val introContent = if (isGuestMode) {
                "You are using FlexDesk in Guest Mode. Your documents auto-save locally on this device.\n\n" +
                    "• Sign in with Google anytime from Accounts to enable cloud sync across Chromebook and Android.\n" +
                    "• Try snapping two windows side-by-side on wide screens or customizing your Dark Mode theme."
            } else {
                "FlexDesk keeps your documents, theme, and shelf pins in sync across your Chromebook and Android devices.\n\n" +
                    "• Real-time Auto-Save: Changes save locally within 600ms even offline.\n" +
                    "• Window Multitasking: Snap two workspace apps side-by-side on Chromebook or tablet.\n" +
                    "• Lite Performance Mode: Optimized animations and persistent PNG icon caching for low-RAM devices."
            }
            dataSource.createDocument(
                title = if (isGuestMode) "Welcome to Guest Workspace" else "Welcome to FlexDesk Workspace",
                content = introContent,
                category = "Notes",
                isPinned = true,
                deviceName = hwName
            )
        }
    }

    private fun observeRemoteDevices() {
        viewModelScope.launch {
            dataSource.observeConnectedDevices()
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                }
                .collectLatest { list ->
                    _devices.value = list
                }
        }
    }

    fun toggleLauncher() {
        _chromeState.update {
            it.copy(
                isLauncherOpen = !it.isLauncherOpen,
                isControlCenterOpen = false
            )
        }
    }

    fun closeLauncher() {
        _chromeState.update { it.copy(isLauncherOpen = false) }
    }

    fun openLauncher() {
        _chromeState.update { it.copy(isLauncherOpen = true, isControlCenterOpen = false) }
    }

    fun toggleControlCenter() {
        _chromeState.update {
            it.copy(
                isControlCenterOpen = !it.isControlCenterOpen,
                isLauncherOpen = false
            )
        }
    }

    fun closeControlCenter() {
        _chromeState.update { it.copy(isControlCenterOpen = false) }
    }

    fun updateSearchQuery(query: String) {
        _chromeState.update { it.copy(searchQuery = query) }
    }

    fun selectAppCategory(category: String) {
        _chromeState.update { it.copy(selectedAppCategory = category) }
    }

    fun openAppWindow(appId: String, customTitle: String? = null, documentId: String? = null) {
        val builtin = BuiltinAppId.fromId(appId)
        if (builtin == null) {
            // Launch external Android app
            val launched = telemetry.launchInstalledApp(appId)
            if (!launched) {
                _chromeState.update { it.copy(statusBannerMessage = "Unable to launch app: $appId") }
            }
            return
        }

        val targetWindowId = appId
        val resolvedTitle = customTitle ?: builtin.label

        if (appId == BuiltinAppId.DOCS.id && documentId != null) {
            loadDocumentIntoDraft(documentId)
        } else if (appId == BuiltinAppId.DOCS.id && _chromeState.value.activeDraft == null) {
            val firstDoc = _documents.value.firstOrNull()
            if (firstDoc != null) {
                loadDocumentIntoDraft(firstDoc.id)
            } else {
                createNewDocument("Notes")
            }
        }

        _chromeState.update { state ->
            val existing = state.openWindows.firstOrNull { it.windowId == targetWindowId }
            val updatedList = if (existing != null) {
                state.openWindows.map {
                    if (it.windowId == targetWindowId) {
                        it.copy(isMinimized = false, title = resolvedTitle, documentId = documentId ?: it.documentId)
                    } else it
                }
            } else {
                state.openWindows + DesktopWindow(
                    windowId = targetWindowId,
                    appId = appId,
                    title = resolvedTitle,
                    isMinimized = false,
                    snapMode = WindowSnapMode.FULL,
                    documentId = documentId
                )
            }
            state.copy(
                openWindows = updatedList,
                focusedWindowId = targetWindowId,
                isLauncherOpen = false,
                isControlCenterOpen = false
            )
        }
    }

    fun focusOrToggleShelfWindow(appId: String) {
        val existing = _chromeState.value.openWindows.firstOrNull { it.appId == appId }
        if (existing == null) {
            openAppWindow(appId)
        } else if (existing.isMinimized || _chromeState.value.focusedWindowId != existing.windowId || _chromeState.value.isLauncherOpen) {
            _chromeState.update { state ->
                state.copy(
                    openWindows = state.openWindows.map {
                        if (it.windowId == existing.windowId) it.copy(isMinimized = false) else it
                    },
                    focusedWindowId = existing.windowId,
                    isLauncherOpen = false,
                    isControlCenterOpen = false
                )
            }
        } else {
            minimizeWindow(existing.windowId)
        }
    }

    fun minimizeWindow(windowId: String) {
        flushAutoSaveNow()
        _chromeState.update { state ->
            val updated = state.openWindows.map {
                if (it.windowId == windowId) it.copy(isMinimized = true) else it
            }
            val nextFocus = updated.lastOrNull { !it.isMinimized }?.windowId
            state.copy(
                openWindows = updated,
                focusedWindowId = nextFocus
            )
        }
    }

    fun closeWindow(windowId: String) {
        flushAutoSaveNow()
        _chromeState.update { state ->
            val updated = state.openWindows.filterNot { it.windowId == windowId }
            val nextFocus = updated.lastOrNull { !it.isMinimized }?.windowId
            state.copy(
                openWindows = updated,
                focusedWindowId = nextFocus
            )
        }
    }

    fun setWindowSnap(windowId: String, snapMode: WindowSnapMode) {
        _chromeState.update { state ->
            val updated = state.openWindows.map {
                when {
                    it.windowId == windowId -> it.copy(snapMode = snapMode, isMinimized = false)
                    snapMode != WindowSnapMode.FULL && it.snapMode == snapMode -> it.copy(
                        snapMode = if (snapMode == WindowSnapMode.LEFT) WindowSnapMode.RIGHT else WindowSnapMode.LEFT
                    )
                    else -> it
                }
            }
            state.copy(openWindows = updated, focusedWindowId = windowId)
        }
    }

    fun togglePinApp(appId: String) {
        viewModelScope.launch {
            val current = localPrefs.localStateFlow.first()
            val updatedPins = if (current.pinnedApps.contains(appId)) {
                current.pinnedApps.filterNot { it == appId }
            } else {
                (current.pinnedApps + appId).take(20)
            }
            persistSettingsChange { it.copy(pinnedApps = updatedPins) }
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            persistSettingsChange { it.copy(themeMode = mode.key) }
        }
    }

    fun updateAccentPalette(palette: AccentPalette) {
        viewModelScope.launch {
            persistSettingsChange { it.copy(accentPalette = palette.key) }
        }
    }

    fun updateFontScale(scale: Float) {
        viewModelScope.launch {
            persistSettingsChange { it.copy(fontScale = scale.toDouble().coerceIn(0.85, 1.4)) }
        }
    }

    fun toggleReduceMotion() {
        viewModelScope.launch {
            val current = localPrefs.localStateFlow.first()
            persistSettingsChange { it.copy(reduceMotion = !current.reduceMotion) }
        }
    }

    fun toggleHighContrast() {
        viewModelScope.launch {
            val current = localPrefs.localStateFlow.first()
            persistSettingsChange { it.copy(highContrast = !current.highContrast) }
        }
    }

    fun toggleLargeTargets() {
        viewModelScope.launch {
            val current = localPrefs.localStateFlow.first()
            persistSettingsChange { it.copy(largeTargets = !current.largeTargets) }
        }
    }

    fun toggleLiteMode() {
        viewModelScope.launch {
            val current = localPrefs.localStateFlow.first()
            val nextLite = !current.liteMode
            iconCache.configureMemoryBudget(nextLite)
            persistSettingsChange {
                it.copy(
                    liteMode = nextLite,
                    reduceMotion = if (nextLite) true else it.reduceMotion
                )
            }
        }
    }

    fun toggleDnd() {
        viewModelScope.launch {
            val current = localPrefs.localStateFlow.first()
            localPrefs.updateLocalDeviceControls(!current.dndEnabled, current.screenBrightness)
        }
    }

    fun updateBrightness(brightness: Float) {
        viewModelScope.launch {
            val current = localPrefs.localStateFlow.first()
            localPrefs.updateLocalDeviceControls(current.dndEnabled, brightness)
        }
    }

    fun setDeveloperModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            localPrefs.setDeveloperModeEnabled(enabled)
            _chromeState.update {
                it.copy(
                    statusBannerMessage = if (enabled) {
                        "Developer Mode enabled • WP is OFF (warning armed for boot)"
                    } else {
                        "Developer Mode disabled • Write Protection (WP) re-enabled"
                    }
                )
            }
        }
    }

    fun setSuppressDevBootWarning(suppress: Boolean) {
        viewModelScope.launch {
            localPrefs.setSuppressDevBootWarning(suppress)
        }
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            localPrefs.setAppLanguage(language)
        }
    }

    fun cycleLanguage() {
        viewModelScope.launch {
            val current = localPrefs.localStateFlow.first()
            val next = if (current.appLanguage.isArabicResolved()) AppLanguage.ENGLISH else AppLanguage.ARABIC
            localPrefs.setAppLanguage(next)
        }
    }

    fun updateLockSettings(pin: String, requireOnBoot: Boolean) {
        viewModelScope.launch {
            localPrefs.setLockSettings(pin, requireOnBoot)
            _chromeState.update {
                it.copy(
                    statusBannerMessage = if (pin.isNotBlank()) "Lock PIN updated" else "Lock screen settings updated"
                )
            }
        }
    }

    private suspend fun persistSettingsChange(transform: (UserSettings) -> UserSettings) {
        val currentLocal = localPrefs.localStateFlow.first()
        val base = lastAppliedRemoteSettings ?: UserSettings(
            userId = currentUserId,
            displayName = userDisplayName.ifBlank { null },
            email = userEmail.ifBlank { null },
            themeMode = currentLocal.themeMode.key,
            accentPalette = currentLocal.accentPalette.key,
            fontScale = currentLocal.fontScale.toDouble(),
            reduceMotion = currentLocal.reduceMotion,
            highContrast = currentLocal.highContrast,
            largeTargets = currentLocal.largeTargets,
            liteMode = currentLocal.liteMode,
            pinnedApps = currentLocal.pinnedApps
        )
        val updated = transform(base)
        lastAppliedRemoteSettings = updated
        localPrefs.saveFromUserSettings(updated)
        dataSource.saveUserSettings(updated)
    }

    // --- Document Auto-Save & Offline Productivity ---

    fun createNewDocument(category: String = "Notes") {
        flushAutoSaveNow()
        val hwName = _chromeState.value.hardwareProfile?.deviceName ?: "Android"
        viewModelScope.launch {
            _chromeState.update { it.copy(autoSaveStatus = AutoSaveStatus.SAVING) }
            val result = dataSource.createDocument(
                title = "Untitled Note",
                content = "",
                category = category,
                isPinned = false,
                deviceName = hwName
            )
            result.onSuccess { newId ->
                _chromeState.update {
                    it.copy(
                        activeDraft = ActiveDocumentDraft(
                            docId = newId,
                            title = "Untitled Note",
                            content = "",
                            category = category,
                            isPinned = false,
                            isNew = false
                        ),
                        autoSaveStatus = when {
                            isGuestMode -> AutoSaveStatus.SAVED_LOCAL
                            it.networkStatus.isOnline -> AutoSaveStatus.SYNCED
                            else -> AutoSaveStatus.SAVED_OFFLINE
                        }
                    )
                }
            }.onFailure {
                _chromeState.update { it.copy(autoSaveStatus = AutoSaveStatus.ERROR) }
            }
        }
    }

    fun loadDocumentIntoDraft(docId: String) {
        if (_chromeState.value.activeDraft?.docId == docId) return
        flushAutoSaveNow()
        val doc = _documents.value.firstOrNull { it.id == docId } ?: return
        _chromeState.update {
            it.copy(
                activeDraft = ActiveDocumentDraft(
                    docId = doc.id,
                    title = doc.title,
                    content = doc.content,
                    category = doc.category,
                    isPinned = doc.isPinned,
                    isNew = false
                ),
                autoSaveStatus = when {
                    isGuestMode -> AutoSaveStatus.SAVED_LOCAL
                    it.hasPendingWrites || !it.networkStatus.isOnline -> AutoSaveStatus.SAVED_OFFLINE
                    else -> AutoSaveStatus.SYNCED
                }
            )
        }
    }

    fun onDraftTitleChanged(newTitle: String) {
        val current = _chromeState.value.activeDraft ?: return
        _chromeState.update {
            it.copy(
                activeDraft = current.copy(title = newTitle),
                autoSaveStatus = AutoSaveStatus.SAVING
            )
        }
        scheduleDebouncedAutoSave()
    }

    fun onDraftContentChanged(newContent: String) {
        val current = _chromeState.value.activeDraft ?: return
        _chromeState.update {
            it.copy(
                activeDraft = current.copy(content = newContent),
                autoSaveStatus = AutoSaveStatus.SAVING
            )
        }
        scheduleDebouncedAutoSave()
    }

    fun onDraftCategoryChanged(category: String) {
        val current = _chromeState.value.activeDraft ?: return
        _chromeState.update {
            it.copy(
                activeDraft = current.copy(category = category),
                autoSaveStatus = AutoSaveStatus.SAVING
            )
        }
        flushAutoSaveNow()
    }

    fun toggleDraftPinned() {
        val current = _chromeState.value.activeDraft ?: return
        _chromeState.update {
            it.copy(
                activeDraft = current.copy(isPinned = !current.isPinned),
                autoSaveStatus = AutoSaveStatus.SAVING
            )
        }
        flushAutoSaveNow()
    }

    fun deleteDocument(docId: String) {
        autoSaveJob?.cancel()
        viewModelScope.launch {
            dataSource.deleteDocument(docId)
            val currentDraft = _chromeState.value.activeDraft
            if (currentDraft?.docId == docId) {
                val next = _documents.value.firstOrNull { it.id != docId }
                _chromeState.update {
                    it.copy(
                        activeDraft = next?.let { doc ->
                            ActiveDocumentDraft(
                                docId = doc.id,
                                title = doc.title,
                                content = doc.content,
                                category = doc.category,
                                isPinned = doc.isPinned,
                                isNew = false
                            )
                        },
                        autoSaveStatus = AutoSaveStatus.IDLE
                    )
                }
            }
        }
    }

    private fun scheduleDebouncedAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(AUTO_SAVE_DEBOUNCE_MS)
            commitActiveDraft()
        }
    }

    fun flushAutoSaveNow() {
        if (_chromeState.value.autoSaveStatus == AutoSaveStatus.SAVING) {
            autoSaveJob?.cancel()
            viewModelScope.launch {
                commitActiveDraft()
            }
        }
    }

    private suspend fun commitActiveDraft() {
        val draft = _chromeState.value.activeDraft ?: return
        if (draft.docId.isBlank()) return
        val hwName = _chromeState.value.hardwareProfile?.deviceName ?: "Android"
        val res = dataSource.updateDocument(
            docId = draft.docId,
            title = draft.title.ifBlank { "Untitled Note" },
            content = draft.content,
            category = draft.category,
            isPinned = draft.isPinned,
            deviceName = hwName
        )
        _chromeState.update { state ->
            state.copy(
                autoSaveStatus = when {
                    res.isFailure -> AutoSaveStatus.ERROR
                    isGuestMode -> AutoSaveStatus.SAVED_LOCAL
                    !state.networkStatus.isOnline || state.hasPendingWrites -> AutoSaveStatus.SAVED_OFFLINE
                    else -> AutoSaveStatus.SYNCED
                }
            )
        }
    }

    fun clearIconCache() {
        viewModelScope.launch {
            iconCache.clearCache()
            iconCache.warmCacheFromDisk(_installedApps.value.map { it.packageName })
            _chromeState.update { it.copy(statusBannerMessage = "Icon cache rebuilt cleanly") }
        }
    }

    fun syncDeviceHeartbeatNow() {
        viewModelScope.launch {
            val hw = telemetry.readHardwareProfile()
            val devId = _chromeState.value.currentDeviceId.ifBlank { localPrefs.getOrCreateDeviceId() }
            val res = dataSource.registerOrHeartbeatDevice(
                deviceId = devId,
                deviceName = hw.deviceName,
                manufacturer = hw.manufacturer,
                formFactor = hw.formFactor,
                osVersion = hw.osVersion,
                isLowRamDevice = hw.isLowRamDevice
            )
            _chromeState.update {
                it.copy(
                    hardwareProfile = hw,
                    statusBannerMessage = when {
                        isGuestMode -> "Guest workspace saved locally on ${hw.deviceName}"
                        res.isSuccess -> "Device telemetry & workspace synced"
                        else -> "Queued locally — will sync when online"
                    }
                )
            }
        }
    }

    fun dismissBanner() {
        _chromeState.update { it.copy(statusBannerMessage = null) }
    }

    private data class Tuple5<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E
    )

    private companion object {
        const val AUTO_SAVE_DEBOUNCE_MS = 600L
    }
}
