package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewSidebar
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Minimize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.i18n.LocalStrings
import com.example.ui.state.BuiltinAppId
import com.example.ui.state.DesktopWindow
import com.example.ui.state.WindowSnapMode
import com.example.ui.theme.LocalFlexDeskTokens
import com.example.ui.viewmodel.WorkspaceUiState

@Composable
fun DesktopWindowCanvas(
    uiState: WorkspaceUiState,
    isExpandedScreen: Boolean,
    onFocusWindow: (String) -> Unit,
    onMinimizeWindow: (String) -> Unit,
    onCloseWindow: (String) -> Unit,
    onSetWindowSnap: (String, WindowSnapMode) -> Unit,
    windowContent: @Composable (DesktopWindow) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleWindows = uiState.openWindows.filter { !it.isMinimized }
    if (visibleWindows.isEmpty()) return

    val focused = visibleWindows.firstOrNull { it.windowId == uiState.focusedWindowId }
        ?: visibleWindows.last()

    BackHandler(enabled = !uiState.isLauncherOpen && !uiState.isControlCenterOpen) {
        onMinimizeWindow(focused.windowId)
    }

    if (isExpandedScreen) {
        val leftWin = visibleWindows.firstOrNull { it.snapMode == WindowSnapMode.LEFT }
        val rightWin = visibleWindows.firstOrNull { it.snapMode == WindowSnapMode.RIGHT }
        val splitActive = leftWin != null || rightWin != null

        if (splitActive && visibleWindows.size >= 2) {
            val resolvedLeft = leftWin ?: visibleWindows.first { it.windowId != rightWin?.windowId }
            val resolvedRight = rightWin ?: visibleWindows.first { it.windowId != resolvedLeft.windowId }
            Row(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WindowFrame(
                    window = resolvedLeft,
                    isFocused = resolvedLeft.windowId == focused.windowId,
                    isExpandedScreen = true,
                    onFocus = { onFocusWindow(resolvedLeft.windowId) },
                    onMinimize = { onMinimizeWindow(resolvedLeft.windowId) },
                    onClose = { onCloseWindow(resolvedLeft.windowId) },
                    onSetSnap = { snap -> onSetWindowSnap(resolvedLeft.windowId, snap) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    windowContent(resolvedLeft)
                }

                WindowFrame(
                    window = resolvedRight,
                    isFocused = resolvedRight.windowId == focused.windowId,
                    isExpandedScreen = true,
                    onFocus = { onFocusWindow(resolvedRight.windowId) },
                    onMinimize = { onMinimizeWindow(resolvedRight.windowId) },
                    onClose = { onCloseWindow(resolvedRight.windowId) },
                    onSetSnap = { snap -> onSetWindowSnap(resolvedRight.windowId, snap) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    windowContent(resolvedRight)
                }
            }
        } else {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                WindowFrame(
                    window = focused,
                    isFocused = true,
                    isExpandedScreen = true,
                    onFocus = { onFocusWindow(focused.windowId) },
                    onMinimize = { onMinimizeWindow(focused.windowId) },
                    onClose = { onCloseWindow(focused.windowId) },
                    onSetSnap = { snap -> onSetWindowSnap(focused.windowId, snap) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    windowContent(focused)
                }
            }
        }
    } else {
        // Compact / Medium layout: tab switcher strip if >1 open window + full active window
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            if (visibleWindows.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    visibleWindows.forEach { win ->
                        val strings = LocalStrings.current
                        val active = win.windowId == focused.windowId
                        val resolvedLabel = BuiltinAppId.fromId(win.appId)?.localizedLabel(strings) ?: win.title
                        Surface(
                            onClick = { onFocusWindow(win.windowId) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = iconForAppId(win.appId),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = resolvedLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            WindowFrame(
                window = focused,
                isFocused = true,
                isExpandedScreen = false,
                onFocus = { onFocusWindow(focused.windowId) },
                onMinimize = { onMinimizeWindow(focused.windowId) },
                onClose = { onCloseWindow(focused.windowId) },
                onSetSnap = { snap -> onSetWindowSnap(focused.windowId, snap) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                windowContent(focused)
            }
        }
    }
}

@Composable
private fun WindowFrame(
    window: DesktopWindow,
    isFocused: Boolean,
    isExpandedScreen: Boolean,
    onFocus: () -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    onSetSnap: (WindowSnapMode) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val tokens = LocalFlexDeskTokens.current
    val strings = LocalStrings.current
    val resolvedTitle = BuiltinAppId.fromId(window.appId)?.localizedLabel(strings) ?: window.title
    val borderColor = if (isFocused) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
    } else {
        tokens.borderColor
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isFocused) 6.dp else 2.dp,
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable { onFocus() }
            .testTag("window_frame_${window.appId}")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ChromeOS Window Titlebar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tokens.windowChromeBackground)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = iconForAppId(window.appId),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = resolvedTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isExpandedScreen) {
                        IconButton(
                            onClick = {
                                val nextSnap = when (window.snapMode) {
                                    WindowSnapMode.FULL -> WindowSnapMode.LEFT
                                    WindowSnapMode.LEFT -> WindowSnapMode.RIGHT
                                    WindowSnapMode.RIGHT -> WindowSnapMode.FULL
                                }
                                onSetSnap(nextSnap)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("window_snap_${window.appId}")
                        ) {
                            Icon(
                                imageVector = if (window.snapMode == WindowSnapMode.FULL) {
                                    Icons.AutoMirrored.Rounded.ViewSidebar
                                } else {
                                    Icons.Rounded.Fullscreen
                                },
                                contentDescription = "Snap or Maximize Window",
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onMinimize,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("window_minimize_${window.appId}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Minimize,
                            contentDescription = "Minimize Window",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("window_close_${window.appId}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Window",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                content()
            }
        }
    }
}
