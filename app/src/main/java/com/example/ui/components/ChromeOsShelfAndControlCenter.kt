package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.AddToDrive
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ThemeMode
import com.example.ui.i18n.LocalStrings
import com.example.ui.state.BuiltinAppId
import com.example.ui.theme.LocalFlexDeskTokens
import com.example.ui.viewmodel.WorkspaceUiState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChromeOsShelfBar(
    uiState: WorkspaceUiState,
    isExpandedScreen: Boolean,
    onToggleLauncher: () -> Unit,
    onToggleControlCenter: () -> Unit,
    onToggleLanguage: () -> Unit,
    onShelfAppClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalFlexDeskTokens.current
    val strings = LocalStrings.current
    val targetMinSize = if (uiState.settings.largeTargets) 54.dp else 48.dp

    var currentTime by remember { mutableStateOf(formatCurrentTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = formatCurrentTime()
            delay(15_000L)
        }
    }

    val pinnedSet = remember(uiState.settings.pinnedApps) { uiState.settings.pinnedApps.toSet() }
    val pinnedShelfApps = remember(uiState.settings.pinnedApps) { uiState.settings.pinnedApps }
    val unpinnedOpenApps = remember(uiState.openWindows, pinnedSet) {
        uiState.openWindows.map { it.appId }.distinct().filterNot { pinnedSet.contains(it) }
    }

    Surface(
        color = tokens.shelfBackground,
        tonalElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .border(width = 1.dp, color = tokens.borderColor, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: ChromeOS Circular Launcher Ring Button (in pill)
            Surface(
                onClick = onToggleLauncher,
                shape = CircleShape,
                color = if (uiState.isLauncherOpen) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    tokens.pillSurface
                },
                modifier = Modifier
                    .size(targetMinSize.coerceAtMost(46.dp))
                    .testTag("launcher_toggle_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val ringColor = if (uiState.isLauncherOpen) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                    Canvas(modifier = Modifier.size(20.dp)) {
                        drawCircle(
                            color = ringColor,
                            style = Stroke(width = 2.2.dp.toPx())
                        )
                        drawCircle(
                            color = ringColor,
                            radius = size.minDimension * 0.22f,
                            center = Offset(size.width / 2f, size.height / 2f)
                        )
                    }
                }
            }

            // Center: Pinned Circular Icons + Divider + Unpinned Running Windows
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                pinnedShelfApps.forEach { appId ->
                    ShelfCircularIcon(
                        appId = appId,
                        uiState = uiState,
                        onShelfAppClick = onShelfAppClick
                    )
                }

                if (unpinnedOpenApps.isNotEmpty() && pinnedShelfApps.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .width(1.dp)
                            .height(24.dp)
                            .background(tokens.borderColor)
                    )
                }

                unpinnedOpenApps.forEach { appId ->
                    ShelfCircularIcon(
                        appId = appId,
                        uiState = uiState,
                        onShelfAppClick = onShelfAppClick
                    )
                }
            }

            // Right: ChromeOS Status Tray Pills (Tote / Mode / Status Pill)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isExpandedScreen) {
                    // Tote / Docs quick pill like ChromeOS 103 shelf
                    Surface(
                        onClick = { onShelfAppClick(BuiltinAppId.FILES.id) },
                        shape = RoundedCornerShape(50),
                        color = tokens.pillSurface,
                        modifier = Modifier.height(38.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = "Tote & Cached Files",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${uiState.documents.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Language switcher pill ("US" / "AR") right on ChromeOS 103 shelf
                    Surface(
                        onClick = onToggleLanguage,
                        shape = RoundedCornerShape(50),
                        color = tokens.pillSurface,
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("shelf_language_pill")
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.languagePill,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Main ChromeOS Status Tray Pill (Clock, Network, Battery)
                Surface(
                    onClick = onToggleControlCenter,
                    shape = RoundedCornerShape(50),
                    color = if (uiState.isControlCenterOpen) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        tokens.pillSurface
                    },
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("control_center_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = currentTime,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Icon(
                            imageVector = if (uiState.networkStatus.isOnline) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                            contentDescription = uiState.networkStatus.transportLabel,
                            tint = if (uiState.networkStatus.isOnline) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                tokens.offlineAmber
                            },
                            modifier = Modifier.size(16.dp)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (uiState.batteryStatus.isCharging) {
                                    Icons.Rounded.BatteryChargingFull
                                } else {
                                    Icons.Rounded.BatteryStd
                                },
                                contentDescription = "Battery ${uiState.batteryStatus.levelPercent}%",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            if (isExpandedScreen) {
                                Text(
                                    text = "${uiState.batteryStatus.levelPercent}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Subtle sync status dot
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        uiState.isGuestMode -> MaterialTheme.colorScheme.primary
                                        !uiState.networkStatus.isOnline || uiState.hasPendingWrites -> tokens.offlineAmber
                                        else -> tokens.syncGreen
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShelfCircularIcon(
    appId: String,
    uiState: WorkspaceUiState,
    onShelfAppClick: (String) -> Unit
) {
    val strings = LocalStrings.current
    val window = uiState.openWindows.firstOrNull { it.appId == appId }
    val isOpen = window != null
    val isFocused = window != null && !window.isMinimized && uiState.focusedWindowId == window.windowId && !uiState.isLauncherOpen
    val appLabel = BuiltinAppId.fromId(appId)?.localizedLabel(strings)
        ?: uiState.installedApps.firstOrNull { it.packageName == appId }?.label
        ?: appId

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onShelfAppClick(appId) }
            .minimumInteractiveComponentSize()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag("shelf_app_$appId")
    ) {
        Box(
            modifier = Modifier
                .size(if (uiState.settings.largeTargets) 40.dp else 34.dp)
                .clip(CircleShape)
                .background(brushForAppId(appId)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconForAppId(appId),
                contentDescription = appLabel,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .width(if (isFocused) 16.dp else if (isOpen) 8.dp else 0.dp)
                .height(2.5.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (isFocused) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
        )
    }
}

@Composable
fun ControlCenterOverlay(
    uiState: WorkspaceUiState,
    visible: Boolean,
    onDismiss: () -> Unit,
    onCycleTheme: () -> Unit,
    onToggleLiteMode: () -> Unit,
    onToggleDnd: () -> Unit,
    onToggleLanguage: () -> Unit,
    onLockScreen: () -> Unit,
    onToggleReduceMotion: () -> Unit,
    onToggleHighContrast: () -> Unit,
    onToggleLargeTargets: () -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenAppearance: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalFlexDeskTokens.current
    val strings = LocalStrings.current
    val enterAnim = if (uiState.settings.reduceMotion) fadeIn() else slideInVertically { it / 4 } + fadeIn()
    val exitAnim = if (uiState.settings.reduceMotion) fadeOut() else slideOutVertically { it / 4 } + fadeOut()

    AnimatedVisibility(
        visible = visible,
        enter = enterAnim,
        exit = exitAnim,
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = tokens.controlCenterBackground,
            tonalElevation = 12.dp,
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .border(1.dp, tokens.borderColor, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                // Header Row: User + Accounts + Sign Out
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenAccounts() }
                            .padding(4.dp)
                            .testTag("control_center_accounts_row")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = uiState.displayName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = uiState.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = when {
                                    uiState.isGuestMode -> strings.guestModeLocal
                                    uiState.networkStatus.isOnline && !uiState.hasPendingWrites ->
                                        "${strings.syncedDevice} • ${uiState.connectedDevices.size.coerceAtLeast(1)}"
                                    else -> strings.offlineCacheActive
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    uiState.isGuestMode -> MaterialTheme.colorScheme.primary
                                    uiState.networkStatus.isOnline && !uiState.hasPendingWrites -> tokens.syncGreen
                                    else -> tokens.offlineAmber
                                }
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = onLockScreen,
                            modifier = Modifier.testTag("control_center_lock_button")
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = strings.controlCenterLock)
                        }
                        IconButton(
                            onClick = onOpenAccounts,
                            modifier = Modifier.testTag("open_accounts_button")
                        ) {
                            Icon(Icons.Rounded.ManageAccounts, contentDescription = strings.appAccounts)
                        }
                        IconButton(
                            onClick = onOpenAppearance,
                            modifier = Modifier.testTag("open_appearance_button")
                        ) {
                            Icon(Icons.Rounded.Palette, contentDescription = strings.appSettings)
                        }
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("control_center_sign_out")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = strings.lockSwitchAccount)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close Control Center")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Tiles Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val themeMode = uiState.settings.themeMode
                    QuickSettingTile(
                        icon = if (themeMode == ThemeMode.LIGHT) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        title = strings.controlCenterTheme,
                        subtitle = themeMode.label,
                        isActive = themeMode != ThemeMode.LIGHT,
                        onClick = onCycleTheme,
                        modifier = Modifier.weight(1f).testTag("cc_theme_tile")
                    )
                    QuickSettingTile(
                        icon = Icons.Rounded.Language,
                        title = strings.controlCenterLanguage,
                        subtitle = if (strings.isArabic) "العربية (RTL)" else "English (LTR)",
                        isActive = strings.isArabic,
                        onClick = onToggleLanguage,
                        modifier = Modifier.weight(1f).testTag("cc_language_tile")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickSettingTile(
                        icon = Icons.Rounded.Bolt,
                        title = strings.controlCenterLiteMode,
                        subtitle = if (uiState.settings.liteMode) "Low-RAM On" else "Balanced",
                        isActive = uiState.settings.liteMode,
                        onClick = onToggleLiteMode,
                        modifier = Modifier.weight(1f).testTag("cc_lite_tile")
                    )
                    QuickSettingTile(
                        icon = Icons.Rounded.DoNotDisturbOn,
                        title = strings.controlCenterDnd,
                        subtitle = if (uiState.settings.dndEnabled) "Muted" else "Alerts On",
                        isActive = uiState.settings.dndEnabled,
                        onClick = onToggleDnd,
                        modifier = Modifier.weight(1f).testTag("cc_dnd_tile")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Brightness slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Brightness6,
                        contentDescription = "Display Brightness",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Slider(
                        value = if (uiState.settings.screenBrightness < 0f) 0.7f else uiState.settings.screenBrightness,
                        onValueChange = onBrightnessChange,
                        valueRange = 0.15f..1.0f,
                        modifier = Modifier.weight(1f).testTag("cc_brightness_slider")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = tokens.borderColor)

                // Seamless Accessibility Controls Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccessibilityNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.controlCenterAccessibility,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.textScale, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = String.format(Locale.US, "%.2fx", uiState.settings.fontScale),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = uiState.settings.fontScale,
                    onValueChange = onFontScaleChange,
                    valueRange = 0.85f..1.4f,
                    steps = 4,
                    modifier = Modifier.fillMaxWidth().testTag("cc_font_scale_slider")
                )

                AccessibilitySwitchRow(
                    icon = Icons.Rounded.SlowMotionVideo,
                    label = strings.reduceMotion,
                    checked = uiState.settings.reduceMotion,
                    onToggle = onToggleReduceMotion,
                    tag = "cc_reduce_motion_switch"
                )
                AccessibilitySwitchRow(
                    icon = Icons.Rounded.Contrast,
                    label = strings.highContrast,
                    checked = uiState.settings.highContrast,
                    onToggle = onToggleHighContrast,
                    tag = "cc_high_contrast_switch"
                )
                AccessibilitySwitchRow(
                    icon = Icons.Rounded.TouchApp,
                    label = strings.largeTargets,
                    checked = uiState.settings.largeTargets,
                    onToggle = onToggleLargeTargets,
                    tag = "cc_large_targets_switch"
                )
            }
        }
    }
}

@Composable
private fun QuickSettingTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isActive) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        },
        modifier = modifier.height(64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AccessibilitySwitchRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle() }
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            modifier = Modifier.testTag(tag)
        )
    }
}

fun iconForAppId(appId: String): ImageVector {
    return when (appId) {
        BuiltinAppId.DOCS.id -> Icons.Rounded.Description
        BuiltinAppId.NOTES.id -> Icons.Rounded.EditNote
        BuiltinAppId.FILES.id -> Icons.Rounded.Folder
        BuiltinAppId.CALCULATOR.id -> Icons.Rounded.Calculate
        BuiltinAppId.FOCUS.id -> Icons.Rounded.Timer
        BuiltinAppId.ACCOUNTS.id -> Icons.Rounded.ManageAccounts
        BuiltinAppId.SETTINGS.id -> Icons.Rounded.Palette
        BuiltinAppId.GOOGLE_PHOTOS.id -> Icons.Rounded.PhotoLibrary
        BuiltinAppId.GOOGLE_DRIVE.id -> Icons.Rounded.AddToDrive
        BuiltinAppId.GMAIL.id -> Icons.Rounded.Mail
        BuiltinAppId.GOOGLE_CALENDAR.id -> Icons.Rounded.CalendarMonth
        BuiltinAppId.GOOGLE_KEEP.id -> Icons.Rounded.Lightbulb
        BuiltinAppId.GOOGLE_DOCS_WEB.id -> Icons.Rounded.Article
        BuiltinAppId.YOUTUBE.id -> Icons.Rounded.PlayCircle
        BuiltinAppId.GOOGLE_MAPS.id -> Icons.Rounded.Map
        else -> Icons.Rounded.CloudSync
    }
}

private fun formatCurrentTime(): String {
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
}
