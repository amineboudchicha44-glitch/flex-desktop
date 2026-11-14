package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LaptopChromebook
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.TabletAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.AccentPalette
import com.example.data.model.ThemeMode
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.AmberDuskPrimaryDark
import com.example.ui.theme.AuroraMintPrimaryDark
import com.example.ui.theme.ChromeBluePrimaryDark
import com.example.ui.theme.LocalFlexDeskTokens
import com.example.ui.theme.RoseQuartzPrimaryDark
import com.example.ui.theme.SlateMonoPrimaryDark
import com.example.ui.viewmodel.WorkspaceUiState
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun AccountsAndSyncScreenContent(
    uiState: WorkspaceUiState,
    onSyncNow: () -> Unit,
    onSignInWithGoogle: () -> Unit,
    onSignOut: () -> Unit
) {
    val tokens = LocalFlexDeskTokens.current

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("accounts_screen_root")
    ) {
        item {
            Text(
                text = if (uiState.isGuestMode) "Guest Session & Account Upgrade" else "Accounts & Connected Devices",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = if (uiState.isGuestMode) {
                    "You are browsing in local Guest Mode. Sign in with Google anytime to enable cross-device sync."
                } else {
                    "Your Google account synchronizes launcher pins, theme, and documents seamlessly across Chromebook and Android."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Google Account Profile Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, tokens.borderColor, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = uiState.displayName.take(1).uppercase(),
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = uiState.displayName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (uiState.email.isNotBlank()) {
                                    Text(
                                        text = uiState.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = if (uiState.isGuestMode) "Guest Account (Local Only)" else "UID: ${uiState.userId.take(10)}...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = when {
                                uiState.isGuestMode -> MaterialTheme.colorScheme.primaryContainer
                                uiState.networkStatus.isOnline && !uiState.hasPendingWrites -> tokens.syncGreen.copy(alpha = 0.16f)
                                else -> tokens.offlineAmber.copy(alpha = 0.16f)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (uiState.networkStatus.isOnline && !uiState.isGuestMode) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                                    contentDescription = null,
                                    tint = when {
                                        uiState.isGuestMode -> MaterialTheme.colorScheme.primary
                                        uiState.networkStatus.isOnline && !uiState.hasPendingWrites -> tokens.syncGreen
                                        else -> tokens.offlineAmber
                                    },
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when {
                                        uiState.isGuestMode -> "Local"
                                        !uiState.networkStatus.isOnline -> "Offline"
                                        uiState.hasPendingWrites -> "Syncing"
                                        else -> "Synced"
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (uiState.isGuestMode) {
                            Button(
                                onClick = onSignInWithGoogle,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("guest_upgrade_google_signin_button")
                            ) {
                                Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign in with Google")
                            }
                        } else {
                            Button(
                                onClick = onSyncNow,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("accounts_sync_now_button")
                            ) {
                                Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Workspace")
                            }
                        }

                        OutlinedButton(
                            onClick = onSignOut,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("accounts_sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (uiState.isGuestMode) "Exit Guest" else "Sign Out")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Connected Devices (${uiState.connectedDevices.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (uiState.connectedDevices.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Registering ${uiState.hardwareProfile?.deviceName ?: "this device"} to your cloud device list...",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(uiState.connectedDevices, key = { it.id }) { dev ->
                val isCurrent = dev.id == uiState.currentDeviceId
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isCurrent) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, tokens.borderColor, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = when (dev.formFactor) {
                                    "chromebook" -> Icons.Rounded.LaptopChromebook
                                    "tablet" -> Icons.Rounded.TabletAndroid
                                    else -> Icons.Rounded.PhoneAndroid
                                },
                                contentDescription = dev.formFactor,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(dev.deviceName, style = MaterialTheme.typography.titleSmall)
                                    if (isCurrent) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = "This device",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${dev.formFactor.replaceFirstChar { it.uppercase() }} • ${dev.osVersion} • ${if (dev.isLowRamDevice) "Low-RAM Profile" else "Standard RAM"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppearanceAndPerformanceScreenContent(
    uiState: WorkspaceUiState,
    isExpandedScreen: Boolean,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectAccent: (AccentPalette) -> Unit,
    onToggleLiteMode: () -> Unit,
    onToggleReduceMotion: () -> Unit,
    onToggleHighContrast: () -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onUpdateLockSettings: (String, Boolean) -> Unit,
    onLockScreenNow: () -> Unit,
    onSetDeveloperMode: (Boolean) -> Unit,
    onSetSuppressWpWarning: (Boolean) -> Unit,
    onRebootWorkspace: () -> Unit
) {
    val tokens = LocalFlexDeskTokens.current
    val strings = LocalStrings.current
    var selectedSection by remember { mutableIntStateOf(0) }
    var pinInput by remember(uiState.settings.lockPin) { mutableStateOf(uiState.settings.lockPin) }

    val settingsList: @Composable (Modifier) -> Unit = { listModifier ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = listModifier.testTag("appearance_screen_root")
        ) {
            item {
                Text("Personalization & Dark Mode", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Synced across your devices and persisted locally for instant offline startup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isExpandedScreen || selectedSection == 0) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, tokens.borderColor, RoundedCornerShape(18.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Theme Mode", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThemeMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = uiState.settings.themeMode == mode,
                                        onClick = { onSelectThemeMode(mode) },
                                        label = { Text(mode.label) },
                                        modifier = Modifier.testTag("theme_mode_${mode.key}")
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Accent Palette", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AccentPalette.entries.forEach { palette ->
                                    val swatchColor = when (palette) {
                                        AccentPalette.CHROME_BLUE -> ChromeBluePrimaryDark
                                        AccentPalette.AURORA_MINT -> AuroraMintPrimaryDark
                                        AccentPalette.AMBER_DUSK -> AmberDuskPrimaryDark
                                        AccentPalette.ROSE_QUARTZ -> RoseQuartzPrimaryDark
                                        AccentPalette.SLATE_MONO -> SlateMonoPrimaryDark
                                    }
                                    val selected = uiState.settings.accentPalette == palette
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { onSelectAccent(palette) }
                                            .padding(vertical = 6.dp)
                                            .testTag("accent_palette_${palette.key}")
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(swatchColor)
                                                .border(
                                                    width = if (selected) 3.dp else 1.dp,
                                                    color = if (selected) MaterialTheme.colorScheme.onSurface else tokens.borderColor,
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (selected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = "Selected",
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = palette.label.split(" ").first(),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!isExpandedScreen || (selectedSection == 1 || selectedSection == 2)) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, tokens.borderColor, RoundedCornerShape(18.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.Bolt, contentDescription = null, tint = tokens.syncGreen)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Low-End Hardware Lite Mode", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "Reduces animation overhead & caps icon cache heap for Dell Chromebook 11 / 4GB RAM",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = uiState.settings.liteMode,
                                    onCheckedChange = { onToggleLiteMode() },
                                    modifier = Modifier.testTag("settings_lite_mode_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Reduce Motion", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = uiState.settings.reduceMotion,
                                    onCheckedChange = { onToggleReduceMotion() }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("High Contrast Outlines", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = uiState.settings.highContrast,
                                    onCheckedChange = { onToggleHighContrast() }
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Font Scale: ${String.format(Locale.US, "%.2fx", uiState.settings.fontScale)}",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Slider(
                                value = uiState.settings.fontScale,
                                onValueChange = onFontScaleChange,
                                valueRange = 0.85f..1.4f,
                                steps = 4
                            )
                        }
                    }
                }
            }

            if (!isExpandedScreen || selectedSection == 3) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, tokens.borderColor, RoundedCornerShape(18.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(strings.languageSectionTitle, style = MaterialTheme.typography.titleSmall)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppLanguage.entries.forEach { lang ->
                                    FilterChip(
                                        selected = uiState.settings.appLanguage == lang,
                                        onClick = { onSelectLanguage(lang) },
                                        label = { Text(if (strings.isArabic) lang.arabicLabel else lang.englishLabel) },
                                        modifier = Modifier.testTag("settings_lang_${lang.key}")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(strings.lockSecurityTitle, style = MaterialTheme.typography.titleSmall)
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings.lockScreenOnBoot, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Switch(
                                    checked = uiState.settings.requireLockOnBoot,
                                    onCheckedChange = { onUpdateLockSettings(uiState.settings.lockPin, it) },
                                    modifier = Modifier.testTag("settings_lock_on_boot_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = { v -> pinInput = v.filter { it.isDigit() }.take(8) },
                                label = { Text(strings.lockPinLabel) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().testTag("settings_lock_pin_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onUpdateLockSettings(pinInput, uiState.settings.requireLockOnBoot) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("settings_save_pin_button")
                                ) {
                                    Text(strings.savePinButton)
                                }
                                if (uiState.settings.lockPin.isNotBlank() || pinInput.isNotBlank()) {
                                    TextButton(
                                        onClick = {
                                            pinInput = ""
                                            onUpdateLockSettings("", uiState.settings.requireLockOnBoot)
                                        },
                                        modifier = Modifier.testTag("settings_clear_pin_button")
                                    ) {
                                        Text(strings.clearPinButton)
                                    }
                                }
                                OutlinedButton(
                                    onClick = onLockScreenNow,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("settings_lock_now_button")
                                ) {
                                    Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.lockNowButton)
                                }
                            }
                        }
                    }
                }
            }

            if (!isExpandedScreen || selectedSection == 4) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (uiState.settings.developerModeEnabled) tokens.offlineAmber.copy(alpha = 0.65f) else tokens.borderColor,
                                shape = RoundedCornerShape(18.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = Icons.Rounded.Code,
                                        contentDescription = null,
                                        tint = if (uiState.settings.developerModeEnabled) tokens.offlineAmber else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(strings.developerModeTitle, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = strings.developerModeDesc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = uiState.settings.developerModeEnabled,
                                    onCheckedChange = { onSetDeveloperMode(it) },
                                    modifier = Modifier.testTag("settings_dev_mode_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (uiState.settings.developerModeEnabled) {
                                    tokens.offlineAmber.copy(alpha = 0.16f)
                                } else {
                                    tokens.syncGreen.copy(alpha = 0.15f)
                                }
                            ) {
                                Text(
                                    text = if (uiState.settings.developerModeEnabled) {
                                        "Write Protection (WP): OFF"
                                    } else {
                                        "Write Protection (WP): ENFORCED"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (uiState.settings.developerModeEnabled) tokens.offlineAmber else tokens.syncGreen,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }

                            if (uiState.settings.developerModeEnabled) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(strings.skipWpBootWarning, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = "Set automatically via Ctrl + D or waiting 10s at boot warning",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = uiState.settings.suppressDevBootWarning,
                                        onCheckedChange = { onSetSuppressWpWarning(it) },
                                        modifier = Modifier.testTag("settings_suppress_wp_switch")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = onRebootWorkspace,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("settings_reboot_preview_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.RestartAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.rebootPreviewButton)
                            }
                        }
                    }
                }
            }
        }
    }

    if (isExpandedScreen) {
        // Authentic ChromeOS Settings two-pane layout
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .width(210.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = strings.settingsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
                val navItems = listOf(
                    Triple(0, Icons.Rounded.Palette, strings.tabPersonalization),
                    Triple(1, Icons.Rounded.Bolt, strings.tabPerformance),
                    Triple(2, Icons.Rounded.AccessibilityNew, strings.tabAccessibility),
                    Triple(3, Icons.Rounded.Lock, strings.tabSecurityLock),
                    Triple(4, Icons.Rounded.Code, strings.tabDevelopers)
                )
                navItems.forEach { (idx, icon, label) ->
                    val selected = selectedSection == idx
                    Surface(
                        onClick = { selectedSection = idx },
                        shape = RoundedCornerShape(50),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            settingsList(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    } else {
        settingsList(Modifier.fillMaxSize())
    }
}

@Composable
fun CalculatorWindowContent() {
    var expression by remember { mutableStateOf("0") }
    var firstOperand by remember { mutableStateOf<Double?>(null) }
    var pendingOp by remember { mutableStateOf<String?>(null) }
    var resetNext by remember { mutableStateOf(false) }

    fun pressDigit(d: String) {
        if (resetNext || expression == "0") {
            expression = if (d == ".") "0." else d
            resetNext = false
        } else {
            if (d == "." && expression.contains(".")) return
            expression += d
        }
    }

    fun compute(a: Double, b: Double, op: String): Double = when (op) {
        "+" -> a + b
        "-" -> a - b
        "×" -> a * b
        "÷" -> if (b != 0.0) a / b else Double.NaN
        else -> b
    }

    fun formatNum(v: Double): String {
        if (v.isNaN() || v.isInfinite()) return "Error"
        return if (v % 1.0 == 0.0) v.toLong().toString() else String.format(Locale.US, "%.4f", v).trimEnd('0').trimEnd('.')
    }

    fun pressOp(op: String) {
        val current = expression.toDoubleOrNull() ?: return
        if (firstOperand != null && pendingOp != null && !resetNext) {
            val res = compute(firstOperand!!, current, pendingOp!!)
            expression = formatNum(res)
            firstOperand = res
        } else {
            firstOperand = current
        }
        pendingOp = op
        resetNext = true
    }

    fun pressEquals() {
        val current = expression.toDoubleOrNull() ?: return
        if (firstOperand != null && pendingOp != null) {
            val res = compute(firstOperand!!, current, pendingOp!!)
            expression = formatNum(res)
            firstOperand = null
            pendingOp = null
            resetNext = true
        }
    }

    val rows = listOf(
        listOf("C", "±", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "⌫", "=")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = if (firstOperand != null && pendingOp != null) "${formatNum(firstOperand!!)} $pendingOp" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = expression,
                    style = MaterialTheme.typography.displaySmall,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { key ->
                        val isAction = key in setOf("÷", "×", "-", "+", "=")
                        Surface(
                            onClick = {
                                when (key) {
                                    "C" -> {
                                        expression = "0"
                                        firstOperand = null
                                        pendingOp = null
                                        resetNext = false
                                    }
                                    "±" -> {
                                        val v = expression.toDoubleOrNull() ?: 0.0
                                        expression = formatNum(-v)
                                    }
                                    "%" -> {
                                        val v = expression.toDoubleOrNull() ?: 0.0
                                        expression = formatNum(v / 100.0)
                                    }
                                    "⌫" -> {
                                        expression = if (expression.length > 1) expression.dropLast(1) else "0"
                                    }
                                    "÷", "×", "-", "+" -> pressOp(key)
                                    "=" -> pressEquals()
                                    else -> pressDigit(key)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isAction) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FocusTimerWindowContent(
    dndEnabled: Boolean,
    onToggleDnd: () -> Unit
) {
    var totalSeconds by remember { mutableIntStateOf(25 * 60) }
    var remainingSeconds by remember { mutableIntStateOf(25 * 60) }
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        while (isRunning && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
        }
        if (remainingSeconds == 0) isRunning = false
    }

    val mins = remainingSeconds / 60
    val secs = remainingSeconds % 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Focus Session", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Runs completely offline with zero background overhead",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(25 to "Pomodoro 25m", 15 to "Sprint 15m", 5 to "Break 5m").forEach { (m, label) ->
                FilterChip(
                    selected = totalSeconds == m * 60,
                    onClick = {
                        isRunning = false
                        totalSeconds = m * 60
                        remainingSeconds = m * 60
                    },
                    label = { Text(label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = String.format(Locale.US, "%02d:%02d", mins, secs),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { isRunning = !isRunning },
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isRunning) "Pause" else "Start")
            }

            OutlinedButton(
                onClick = {
                    isRunning = false
                    remainingSeconds = totalSeconds
                },
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Enable Do Not Disturb during focus", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = dndEnabled, onCheckedChange = { onToggleDnd() })
        }
    }
}
