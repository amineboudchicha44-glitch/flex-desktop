package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.ThemeMode
import com.example.ui.components.ChromeOsLauncherOverlay
import com.example.ui.components.ChromeOsShelfBar
import com.example.ui.components.ControlCenterOverlay
import com.example.ui.components.DesktopWindowCanvas
import com.example.ui.i18n.LocalStrings
import com.example.ui.state.BuiltinAppId
import com.example.ui.theme.LocalFlexDeskTokens
import com.example.ui.viewmodel.FlexDeskViewModel

@Composable
fun FlexDeskDesktopScreen(
    viewModel: FlexDeskViewModel,
    onSignInWithGoogle: () -> Unit,
    onSignOut: () -> Unit,
    onLockScreen: () -> Unit,
    onRebootWorkspace: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tokens = LocalFlexDeskTokens.current
    val strings = LocalStrings.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.flushAutoSaveNow()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = uiState.isControlCenterOpen) {
        viewModel.closeControlCenter()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    event.key == Key.L &&
                    event.isCtrlPressed &&
                    event.isShiftPressed
                ) {
                    viewModel.flushAutoSaveNow()
                    onLockScreen()
                    true
                } else false
            }
    ) {
        val isExpandedScreen = maxWidth >= 700.dp

        // ChromeOS Geometric Wallpaper Canvas (adapts cleanly to Light, Dark, AMOLED, and Lite Mode)
        if (!tokens.isAmoled) {
            ChromeOsGeometricWallpaper(
                isDark = tokens.isDark,
                liteMode = uiState.settings.liteMode,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Main Desktop / Workspace Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val hasVisibleWindows = uiState.openWindows.any { !it.isMinimized }

                // Tap empty desktop wallpaper to close launcher/control center
                if ((uiState.isLauncherOpen || uiState.isControlCenterOpen) && !hasVisibleWindows) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                viewModel.closeLauncher()
                                viewModel.closeControlCenter()
                            }
                    )
                }

                // Ambient Desktop Card when all windows minimized & launcher closed
                if (!hasVisibleWindows && !uiState.isLauncherOpen) {
                    AmbientDesktopCard(
                        deviceName = uiState.hardwareProfile?.deviceName ?: "FlexDesk",
                        docCount = uiState.documents.size,
                        liteMode = uiState.settings.liteMode,
                        isGuestMode = uiState.isGuestMode,
                        onOpenLauncher = { viewModel.openLauncher() },
                        onOpenDocs = { viewModel.openAppWindow(BuiltinAppId.DOCS.id) },
                        onOpenAccounts = { viewModel.openAppWindow(BuiltinAppId.ACCOUNTS.id) },
                        onOpenControlCenter = { viewModel.toggleControlCenter() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Active Multitasking Window Canvas
                DesktopWindowCanvas(
                    uiState = uiState,
                    isExpandedScreen = isExpandedScreen,
                    onFocusWindow = { viewModel.openAppWindow(it) },
                    onMinimizeWindow = { viewModel.minimizeWindow(it) },
                    onCloseWindow = { viewModel.closeWindow(it) },
                    onSetWindowSnap = { winId, snap -> viewModel.setWindowSnap(winId, snap) },
                    windowContent = { window ->
                        when (window.appId) {
                            BuiltinAppId.DOCS.id -> FlexDocsWindowContent(
                                uiState = uiState,
                                isExpandedScreen = isExpandedScreen,
                                onSelectDoc = { viewModel.loadDocumentIntoDraft(it) },
                                onNewDoc = { viewModel.createNewDocument(it) },
                                onTitleChange = { viewModel.onDraftTitleChanged(it) },
                                onContentChange = { viewModel.onDraftContentChanged(it) },
                                onCategoryChange = { viewModel.onDraftCategoryChanged(it) },
                                onTogglePin = { viewModel.toggleDraftPinned() },
                                onDeleteDoc = { viewModel.deleteDocument(it) },
                                onFlushSave = { viewModel.flushAutoSaveNow() }
                            )
                            BuiltinAppId.NOTES.id -> QuickNotesWindowContent(
                                uiState = uiState,
                                onCreateNote = {
                                    viewModel.createNewDocument("Notes")
                                    viewModel.openAppWindow(BuiltinAppId.DOCS.id)
                                },
                                onOpenInDocs = { docId ->
                                    viewModel.openAppWindow(BuiltinAppId.DOCS.id, documentId = docId)
                                }
                            )
                            BuiltinAppId.FILES.id -> FilesAndCacheWindowContent(
                                uiState = uiState,
                                onOpenDocument = { docId ->
                                    viewModel.openAppWindow(BuiltinAppId.DOCS.id, documentId = docId)
                                },
                                onRebuildIconCache = { viewModel.clearIconCache() }
                            )
                            BuiltinAppId.CALCULATOR.id -> CalculatorWindowContent()
                            BuiltinAppId.FOCUS.id -> FocusTimerWindowContent(
                                dndEnabled = uiState.settings.dndEnabled,
                                onToggleDnd = { viewModel.toggleDnd() }
                            )
                            BuiltinAppId.ACCOUNTS.id -> AccountsAndSyncScreenContent(
                                uiState = uiState,
                                onSyncNow = { viewModel.syncDeviceHeartbeatNow() },
                                onSignInWithGoogle = onSignInWithGoogle,
                                onSignOut = onSignOut
                            )
                            BuiltinAppId.SETTINGS.id -> AppearanceAndPerformanceScreenContent(
                                uiState = uiState,
                                isExpandedScreen = isExpandedScreen,
                                onSelectThemeMode = { viewModel.updateThemeMode(it) },
                                onSelectAccent = { viewModel.updateAccentPalette(it) },
                                onToggleLiteMode = { viewModel.toggleLiteMode() },
                                onToggleReduceMotion = { viewModel.toggleReduceMotion() },
                                onToggleHighContrast = { viewModel.toggleHighContrast() },
                                onFontScaleChange = { viewModel.updateFontScale(it) },
                                onSelectLanguage = { viewModel.setAppLanguage(it) },
                                onUpdateLockSettings = { pin, reqBoot -> viewModel.updateLockSettings(pin, reqBoot) },
                                onLockScreenNow = {
                                    viewModel.flushAutoSaveNow()
                                    viewModel.closeControlCenter()
                                    onLockScreen()
                                },
                                onSetDeveloperMode = { viewModel.setDeveloperModeEnabled(it) },
                                onSetSuppressWpWarning = { viewModel.setSuppressDevBootWarning(it) },
                                onRebootWorkspace = onRebootWorkspace
                            )
                            else -> {
                                val webApp = BuiltinAppId.fromId(window.appId)
                                if (webApp?.webUrl != null) {
                                    GoogleWebAppWindowContent(
                                        app = webApp,
                                        isOnline = uiState.networkStatus.isOnline,
                                        defaultDesktopUserAgent = isExpandedScreen
                                    )
                                }
                            }
                        }
                    }
                )

                // Dim scrim when launcher or control center is open over windows
                if ((uiState.isLauncherOpen || uiState.isControlCenterOpen) && hasVisibleWindows) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.35f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                viewModel.closeLauncher()
                                viewModel.closeControlCenter()
                            }
                    )
                }

                // ChromeOS Productivity Launcher Overlay (bottom-left floating on Chromebook, sheet on phone)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = if (isExpandedScreen) 14.dp else 8.dp,
                            end = if (isExpandedScreen) 14.dp else 8.dp,
                            top = 10.dp,
                            bottom = 10.dp
                        ),
                    contentAlignment = if (isExpandedScreen) Alignment.BottomStart else Alignment.BottomCenter
                ) {
                    ChromeOsLauncherOverlay(
                        uiState = uiState,
                        iconCache = viewModel.iconCache,
                        visible = uiState.isLauncherOpen,
                        isExpandedScreen = isExpandedScreen,
                        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                        onCategorySelected = { viewModel.selectAppCategory(it) },
                        onLaunchApp = { appId -> viewModel.openAppWindow(appId) },
                        onOpenDocument = { docId ->
                            viewModel.openAppWindow(BuiltinAppId.DOCS.id, documentId = docId)
                        },
                        onNewDocument = {
                            viewModel.createNewDocument("Notes")
                            viewModel.openAppWindow(BuiltinAppId.DOCS.id)
                        },
                        onTogglePinApp = { viewModel.togglePinApp(it) },
                        onOpenAccounts = { viewModel.openAppWindow(BuiltinAppId.ACCOUNTS.id) }
                    )
                }

                // ChromeOS Control Center Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    ControlCenterOverlay(
                        uiState = uiState,
                        visible = uiState.isControlCenterOpen,
                        onDismiss = { viewModel.closeControlCenter() },
                        onCycleTheme = {
                            val next = when (uiState.settings.themeMode) {
                                ThemeMode.DARK -> ThemeMode.AMOLED
                                ThemeMode.AMOLED -> ThemeMode.LIGHT
                                ThemeMode.LIGHT -> ThemeMode.SYSTEM
                                ThemeMode.SYSTEM -> ThemeMode.DARK
                            }
                            viewModel.updateThemeMode(next)
                        },
                        onToggleLiteMode = { viewModel.toggleLiteMode() },
                        onToggleDnd = { viewModel.toggleDnd() },
                        onToggleLanguage = { viewModel.cycleLanguage() },
                        onLockScreen = {
                            viewModel.flushAutoSaveNow()
                            viewModel.closeControlCenter()
                            onLockScreen()
                        },
                        onToggleReduceMotion = { viewModel.toggleReduceMotion() },
                        onToggleHighContrast = { viewModel.toggleHighContrast() },
                        onToggleLargeTargets = { viewModel.toggleLargeTargets() },
                        onFontScaleChange = { viewModel.updateFontScale(it) },
                        onBrightnessChange = { viewModel.updateBrightness(it) },
                        onOpenAccounts = { viewModel.openAppWindow(BuiltinAppId.ACCOUNTS.id) },
                        onOpenAppearance = { viewModel.openAppWindow(BuiltinAppId.SETTINGS.id) },
                        onSignOut = onSignOut
                    )
                }

                // Status Banner Snackbar
                uiState.statusBannerMessage?.let { msg ->
                    Snackbar(
                        action = {
                            TextButton(onClick = { viewModel.dismissBanner() }) {
                                Text("OK")
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(12.dp)
                            .widthIn(max = 480.dp)
                    ) {
                        Text(msg)
                    }
                }
            }

            // Bottom ChromeOS Taskbar Shelf
            ChromeOsShelfBar(
                uiState = uiState,
                isExpandedScreen = isExpandedScreen,
                onToggleLauncher = { viewModel.toggleLauncher() },
                onToggleControlCenter = { viewModel.toggleControlCenter() },
                onToggleLanguage = { viewModel.cycleLanguage() },
                onShelfAppClick = { appId ->
                    if (BuiltinAppId.fromId(appId) != null) {
                        viewModel.focusOrToggleShelfWindow(appId)
                    } else {
                        viewModel.openAppWindow(appId)
                    }
                }
            )
        }
    }
}

@Composable
private fun ChromeOsGeometricWallpaper(
    isDark: Boolean,
    liteMode: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (!liteMode) {
            Image(
                painter = painterResource(
                    id = if (isDark) R.drawable.img_wallpaper_dark else R.drawable.img_wallpaper_light
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.32f)
            )
        }

        // Authentic ChromeOS geometric shapes inspired by the ChromeOS 103+ Material You wallpaper
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val baseColor = if (isDark) Color(0xFF0E213C) else Color(0xFFBCE0FD)
            val cobalt = if (isDark) Color(0xFF12468C) else Color(0xFF1E70D4)
            val teal = if (isDark) Color(0xFF0A6078) else Color(0xFF0B7285)
            val sky = if (isDark) Color(0xFF1F6FEB) else Color(0xFF60A5FA)
            val highlight = if (isDark) Color(0xFF7DD3FC) else Color(0xFFE0F2FE)

            drawRect(color = baseColor)

            // Large rounded cobalt block on upper-left/center
            drawRoundRect(
                color = cobalt,
                topLeft = Offset(w * 0.14f, -h * 0.08f),
                size = Size(w * 0.44f, h * 0.62f),
                cornerRadius = CornerRadius(w * 0.14f, w * 0.14f)
            )

            // Right-side sky arch shape
            drawRoundRect(
                color = sky.copy(alpha = 0.72f),
                topLeft = Offset(w * 0.56f, h * 0.10f),
                size = Size(w * 0.48f, h * 0.72f),
                cornerRadius = CornerRadius(w * 0.22f, w * 0.22f)
            )

            // Bottom-right teal dome + circle accent
            drawCircle(
                color = teal,
                radius = minOf(w, h) * 0.34f,
                center = Offset(w * 0.76f, h * 0.78f)
            )

            drawCircle(
                color = baseColor,
                radius = minOf(w, h) * 0.10f,
                center = Offset(w * 0.76f, h * 0.88f)
            )

            // Crisp geometric triangle accent in center
            val triangle = Path().apply {
                moveTo(w * 0.41f, h * 0.38f)
                lineTo(w * 0.54f, h * 0.38f)
                lineTo(w * 0.52f, h * 0.53f)
                close()
            }
            drawPath(path = triangle, color = highlight)
        }
    }
}

@Composable
private fun AmbientDesktopCard(
    deviceName: String,
    docCount: Int,
    liteMode: Boolean,
    isGuestMode: Boolean,
    onOpenLauncher: () -> Unit,
    onOpenDocs: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenControlCenter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalFlexDeskTokens.current
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = tokens.launcherSheetBackground,
        tonalElevation = 8.dp,
        modifier = modifier
            .padding(24.dp)
            .widthIn(max = 460.dp)
            .fillMaxWidth()
            .border(1.dp, tokens.borderColor, RoundedCornerShape(28.dp))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "FlexDesk Desktop",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = buildString {
                    append(deviceName)
                    append(" • ")
                    append("$docCount docs")
                    if (isGuestMode) append(" • Guest Local")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (liteMode) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = tokens.syncGreen.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = tokens.syncGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Low-RAM Lite Mode Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.syncGreen
                        )
                    }
                }
            }
            val strings = LocalStrings.current
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenLauncher,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.openLauncher)
                }
                OutlinedButton(
                    onClick = onOpenDocs,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.openDocs)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenControlCenter,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.appSettings)
                }
                OutlinedButton(
                    onClick = onOpenAccounts,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.ManageAccounts, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.appAccounts)
                }
            }
        }
    }
}
