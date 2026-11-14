package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.auth.GoogleAuthManager
import com.example.data.auth.authStateFlow
import com.example.data.cache.IconCacheManager
import com.example.data.local.LocalControlState
import com.example.data.local.LocalPreferencesStore
import com.example.data.repository.FirestoreWorkspaceDataSource
import com.example.data.repository.GuestLocalWorkspaceDataSource
import com.example.data.repository.WorkspaceRepository
import com.example.data.system.DeviceTelemetryManager
import com.example.data.system.NetworkStatus
import com.example.ui.screens.DeveloperWpWarningScreen
import com.example.ui.screens.FlexDeskDesktopScreen
import com.example.ui.screens.SignInGateScreen
import com.example.ui.screens.StartupSplashScreen
import com.example.ui.theme.FlexDeskTheme
import com.example.ui.viewmodel.FlexDeskViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.system.BatteryStatus
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.ArStrings
import com.example.ui.i18n.EnStrings
import com.example.ui.i18n.LocalStrings
import com.example.ui.screens.LockScreenOverlay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private enum class BootStage {
    LOADING_PREFS,
    WP_WARNING,
    BOOT_ANIMATION,
    COMPLETE
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContext = applicationContext
        val localPrefs = LocalPreferencesStore(appContext)
        val iconCache = IconCacheManager(appContext)
        val telemetry = DeviceTelemetryManager(appContext)

        setContent {
            val localControlState by localPrefs.localStateFlow.collectAsStateWithLifecycle(
                initialValue = LocalControlState()
            )
            val networkStatus by telemetry.observeNetworkStatus().collectAsStateWithLifecycle(
                initialValue = NetworkStatus(isOnline = true, transportLabel = "Connected")
            )
            val batteryStatus by telemetry.observeBatteryStatus().collectAsStateWithLifecycle(
                initialValue = BatteryStatus(levelPercent = 100, isCharging = false)
            )

            // Apply Control Center window brightness smoothly
            LaunchedEffect(localControlState.screenBrightness) {
                val lp = window.attributes
                lp.screenBrightness = localControlState.screenBrightness
                window.attributes = lp
            }

            val isArabic = localControlState.appLanguage.isArabicResolved()
            val resolvedStrings = if (isArabic) ArStrings else EnStrings
            val resolvedLayoutDir = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

            FlexDeskTheme(
                themeMode = localControlState.themeMode,
                accentPalette = localControlState.accentPalette,
                fontScale = localControlState.fontScale,
                highContrast = localControlState.highContrast
            ) {
                CompositionLocalProvider(
                    LocalStrings provides resolvedStrings,
                    LocalLayoutDirection provides resolvedLayoutDir
                ) {
                    FlexDeskRoot(
                        localControlState = localControlState,
                        localPrefs = localPrefs,
                        iconCache = iconCache,
                        telemetry = telemetry,
                        isOnline = networkStatus.isOnline,
                        batteryPercent = batteryStatus.levelPercent
                    )
                }
            }
        }
    }
}

@Composable
private fun FlexDeskRoot(
    localControlState: LocalControlState,
    localPrefs: LocalPreferencesStore,
    iconCache: IconCacheManager,
    telemetry: DeviceTelemetryManager,
    isOnline: Boolean,
    batteryPercent: Int
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val hardwareProfile = remember { telemetry.readHardwareProfile() }

    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    var bootStage by remember { mutableStateOf(BootStage.LOADING_PREFS) }
    var isGuestMode by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var warmUpLabel by remember { mutableStateOf("Warming persistent icon cache...") }

    val toggleLanguageAction: () -> Unit = {
        scope.launch {
            val next = if (localControlState.appLanguage.isArabicResolved()) {
                AppLanguage.ENGLISH
            } else {
                AppLanguage.ARABIC
            }
            localPrefs.setAppLanguage(next)
        }
    }

    val triggerReboot: () -> Unit = {
        scope.launch {
            val latestPrefs = localPrefs.localStateFlow.first()
            isLocked = latestPrefs.requireLockOnBoot
            bootStage = if (latestPrefs.developerModeEnabled && !latestPrefs.suppressDevBootWarning) {
                BootStage.WP_WARNING
            } else {
                BootStage.BOOT_ANIMATION
            }
        }
    }

    // If Firebase Auth signs in while in Guest Mode, transition cleanly into the signed-in account
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            isGuestMode = false
        }
    }

    // On cold start, read DataStore prefs first so Developer Mode WP warning & Lock Screen trigger deterministically
    LaunchedEffect(Unit) {
        val initialPrefs = localPrefs.localStateFlow.first()
        isLocked = initialPrefs.requireLockOnBoot
        bootStage = if (initialPrefs.developerModeEnabled && !initialPrefs.suppressDevBootWarning) {
            BootStage.WP_WARNING
        } else {
            BootStage.BOOT_ANIMATION
        }
        iconCache.configureMemoryBudget(initialPrefs.liteMode || hardwareProfile.isLowRamDevice)
        val apps = telemetry.loadInstalledLauncherApps()
        iconCache.warmCacheFromDisk(apps.map { it.packageName })
        warmUpLabel = "Cached ${apps.size} launcher icons • Checking account..."
        GoogleAuthManager.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { warmUpLabel = "Account ready" },
            onUnauthenticated = { warmUpLabel = "Ready for Google or Guest session" },
            scope = scope
        )
    }

    when (bootStage) {
        BootStage.LOADING_PREFS -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
            return
        }
        BootStage.WP_WARNING -> {
            DeveloperWpWarningScreen(
                hardwareProfile = hardwareProfile,
                onReenableWpWithSpace = {
                    scope.launch {
                        localPrefs.setDeveloperModeEnabled(false)
                        bootStage = BootStage.BOOT_ANIMATION
                    }
                },
                onContinueDevAndSuppressWarning = {
                    scope.launch {
                        localPrefs.setSuppressDevBootWarning(true)
                        bootStage = BootStage.BOOT_ANIMATION
                    }
                }
            )
            return
        }
        BootStage.BOOT_ANIMATION -> {
            StartupSplashScreen(
                hardwareProfile = hardwareProfile,
                reduceMotion = localControlState.reduceMotion || localControlState.liteMode,
                developerModeEnabled = localControlState.developerModeEnabled,
                warmUpProgressLabel = warmUpLabel,
                onContinueNow = { bootStage = BootStage.COMPLETE }
            )
            return
        }
        BootStage.COMPLETE -> Unit
    }

    val user = currentUser
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            user != null -> {
                val uid = user.uid
                key(uid) {
                    val viewModel: FlexDeskViewModel = viewModel(
                        key = uid,
                        factory = viewModelFactory {
                            initializer {
                                val app = checkNotNull(this[APPLICATION_KEY])
                                val repo = WorkspaceRepository(app.applicationContext)
                                FlexDeskViewModel(
                                    dataSource = FirestoreWorkspaceDataSource(repo, uid),
                                    localPrefs = localPrefs,
                                    iconCache = iconCache,
                                    telemetry = telemetry,
                                    currentUserId = uid,
                                    isGuestMode = false,
                                    userDisplayName = user.displayName ?: "FlexDesk User",
                                    userEmail = user.email ?: ""
                                )
                            }
                        }
                    )

                    FlexDeskDesktopScreen(
                        viewModel = viewModel,
                        onSignInWithGoogle = {},
                        onSignOut = {
                            viewModel.flushAutoSaveNow()
                            isLocked = false
                            GoogleAuthManager.signOut(
                                credentialManager = credentialManager,
                                scope = scope
                            )
                        },
                        onLockScreen = { isLocked = true },
                        onRebootWorkspace = {
                            viewModel.flushAutoSaveNow()
                            triggerReboot()
                        }
                    )
                }

                if (isLocked) {
                    LockScreenOverlay(
                        displayName = user.displayName ?: "FlexDesk User",
                        subtitle = user.email ?: hardwareProfile.deviceName,
                        expectedPin = localControlState.lockPin,
                        isOnline = isOnline,
                        batteryPercent = batteryPercent,
                        onToggleLanguage = toggleLanguageAction,
                        onUnlock = { isLocked = false },
                        onSwitchUser = {
                            isLocked = false
                            GoogleAuthManager.signOut(
                                credentialManager = credentialManager,
                                scope = scope
                            )
                        }
                    )
                }
            }

            isGuestMode -> {
                key("guest_session") {
                    val viewModel: FlexDeskViewModel = viewModel(
                        key = "guest_session",
                        factory = viewModelFactory {
                            initializer {
                                val app = checkNotNull(this[APPLICATION_KEY])
                                FlexDeskViewModel(
                                    dataSource = GuestLocalWorkspaceDataSource(app.applicationContext, localPrefs),
                                    localPrefs = localPrefs,
                                    iconCache = iconCache,
                                    telemetry = telemetry,
                                    currentUserId = GuestLocalWorkspaceDataSource.GUEST_USER_ID,
                                    isGuestMode = true,
                                    userDisplayName = if (localControlState.appLanguage.isArabicResolved()) "مستخدم ضيف" else "Guest User",
                                    userEmail = ""
                                )
                            }
                        }
                    )

                    FlexDeskDesktopScreen(
                        viewModel = viewModel,
                        onSignInWithGoogle = {
                            GoogleAuthManager.onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = { isGuestMode = false },
                                onAuthError = { },
                                onAuthCancelled = { },
                                scope = scope
                            )
                        },
                        onSignOut = {
                            viewModel.flushAutoSaveNow()
                            isLocked = false
                            isGuestMode = false
                        },
                        onLockScreen = { isLocked = true },
                        onRebootWorkspace = {
                            viewModel.flushAutoSaveNow()
                            triggerReboot()
                        }
                    )
                }

                if (isLocked) {
                    LockScreenOverlay(
                        displayName = if (localControlState.appLanguage.isArabicResolved()) "مستخدم ضيف" else "Guest User",
                        subtitle = hardwareProfile.deviceName,
                        expectedPin = localControlState.lockPin,
                        isOnline = isOnline,
                        batteryPercent = batteryPercent,
                        onToggleLanguage = toggleLanguageAction,
                        onUnlock = { isLocked = false },
                        onSwitchUser = {
                            isLocked = false
                            isGuestMode = false
                        }
                    )
                }
            }

            else -> {
                SignInGateScreen(
                    hardwareProfile = hardwareProfile,
                    isOnline = isOnline,
                    onToggleLanguage = toggleLanguageAction,
                    onAuthSuccess = { isGuestMode = false },
                    onContinueAsGuest = { isGuestMode = true }
                )
            }
        }
    }
}
