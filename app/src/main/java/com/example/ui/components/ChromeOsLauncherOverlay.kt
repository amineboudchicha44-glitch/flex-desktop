package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.cache.IconCacheManager
import com.example.data.model.WorkspaceDocument
import com.example.data.system.InstalledAndroidApp
import com.example.ui.i18n.FlexStrings
import com.example.ui.i18n.LocalStrings
import com.example.ui.state.BuiltinAppId
import com.example.ui.theme.LocalFlexDeskTokens
import com.example.ui.viewmodel.WorkspaceUiState

private sealed interface LauncherGridItem {
    val id: String
    val label: String
    val englishLabel: String
    val category: String
    val isBuiltin: Boolean
    val isWebApp: Boolean

    data class Builtin(val app: BuiltinAppId, override val label: String) : LauncherGridItem {
        override val id: String = app.id
        override val englishLabel: String = app.label
        override val category: String = app.category
        override val isBuiltin: Boolean = true
        override val isWebApp: Boolean = app.webUrl != null
    }

    data class Installed(val app: InstalledAndroidApp) : LauncherGridItem {
        override val id: String = app.packageName
        override val label: String = app.label
        override val englishLabel: String = app.label
        override val category: String = app.category
        override val isBuiltin: Boolean = false
        override val isWebApp: Boolean = false
    }
}

@Composable
fun ChromeOsLauncherOverlay(
    uiState: WorkspaceUiState,
    iconCache: IconCacheManager,
    visible: Boolean,
    isExpandedScreen: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onLaunchApp: (String) -> Unit,
    onOpenDocument: (String) -> Unit,
    onNewDocument: () -> Unit,
    onTogglePinApp: (String) -> Unit,
    onOpenAccounts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalFlexDeskTokens.current
    val strings = LocalStrings.current
    val enterAnim = if (uiState.settings.reduceMotion) fadeIn() else slideInVertically { it / 5 } + fadeIn()
    val exitAnim = if (uiState.settings.reduceMotion) fadeOut() else slideOutVertically { it / 5 } + fadeOut()
    var editPinsMode by remember { mutableStateOf(false) }

    val allApps = remember(uiState.installedApps, strings) {
        BuiltinAppId.entries.map { LauncherGridItem.Builtin(it, it.localizedLabel(strings)) } +
            uiState.installedApps.map { LauncherGridItem.Installed(it) }
    }

    val categories = remember(allApps) {
        listOf("All") + allApps.map { it.category }.distinct()
    }

    val query = uiState.searchQuery.trim()
    val filteredApps = remember(allApps, query, uiState.selectedAppCategory) {
        allApps.filter { item ->
            val matchesCat = uiState.selectedAppCategory == "All" || item.category == uiState.selectedAppCategory
            val matchesQuery = query.isEmpty() ||
                item.label.contains(query, ignoreCase = true) ||
                item.englishLabel.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    val topQuickApps = remember(allApps) {
        val preferredOrder = listOf(
            BuiltinAppId.DOCS.id,
            BuiltinAppId.GOOGLE_PHOTOS.id,
            BuiltinAppId.GOOGLE_DRIVE.id,
            BuiltinAppId.GMAIL.id,
            BuiltinAppId.YOUTUBE.id
        )
        preferredOrder.mapNotNull { id -> allApps.firstOrNull { it.id == id } }
    }
    val filteredDocs = remember(uiState.documents, query) {
        if (query.isEmpty()) {
            uiState.documents.take(5)
        } else {
            uiState.documents.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.content.contains(query, ignoreCase = true)
            }.take(5)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = enterAnim,
        exit = exitAnim,
        modifier = modifier
    ) {
        // ChromeOS Productivity Launcher floating card (bottom-left on Chromebook, bottom sheet on phone)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = tokens.launcherSheetBackground,
            tonalElevation = 12.dp,
            modifier = Modifier
                .then(
                    if (isExpandedScreen) {
                        Modifier
                            .widthIn(min = 420.dp, max = 480.dp)
                            .heightIn(max = 580.dp)
                            .fillMaxHeight(0.88f)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.86f)
                    }
                )
                .border(1.dp, tokens.borderColor, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Top ChromeOS Search Pill + Account Avatar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = {
                            Text(
                                text = strings.searchPlaceholder,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear Search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(50),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = tokens.pillSurface,
                            unfocusedContainerColor = tokens.pillSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("launcher_search_input")
                    )

                    Surface(
                        onClick = onOpenAccounts,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("launcher_account_avatar")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = uiState.displayName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Productivity Launcher Content
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (isExpandedScreen) 5 else 4),
                    contentPadding = PaddingValues(bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Top Quick Launch Row + Continue Card + Category Filters
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Top 5 Suggested / Recent Circular Apps Row (like ChromeOS launcher top bar)
                            if (query.isEmpty() && uiState.selectedAppCategory == "All") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    topQuickApps.take(if (isExpandedScreen) 5 else 4).forEach { item ->
                                        CircularAppShortcut(
                                            item = item,
                                            iconCache = iconCache,
                                            isPinned = uiState.settings.pinnedApps.contains(item.id),
                                            showPinBadge = editPinsMode,
                                            largeTargets = uiState.settings.largeTargets,
                                            onClick = {
                                                if (editPinsMode) onTogglePinApp(item.id) else onLaunchApp(item.id)
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // ChromeOS "Continue" Card (Synced / Local Auto-Saved Docs)
                            ContinueSpotlightCard(
                                docs = filteredDocs,
                                strings = strings,
                                onOpenDocument = onOpenDocument,
                                onNewDocument = onNewDocument
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = tokens.borderColor)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Category filter chips + Pin edit toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = editPinsMode,
                                    onClick = { editPinsMode = !editPinsMode },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.PushPin,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    label = { Text(if (editPinsMode) strings.donePinning else strings.pinToShelf) }
                                )

                                if (uiState.settings.liteMode) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = tokens.syncGreen.copy(alpha = 0.16f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Bolt,
                                                contentDescription = null,
                                                tint = tokens.syncGreen,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Lite",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = tokens.syncGreen
                                            )
                                        }
                                    }
                                }

                                categories.forEach { cat ->
                                    FilterChip(
                                        selected = uiState.selectedAppCategory == cat,
                                        onClick = { onCategorySelected(cat) },
                                        label = { Text(BuiltinAppId.localizeCategory(cat, strings)) }
                                    )
                                }
                            }
                        }
                    }

                    // Clean Circular Icon App Grid
                    items(filteredApps, key = { it.id }) { item ->
                        val isPinned = uiState.settings.pinnedApps.contains(item.id)
                        CircularAppShortcut(
                            item = item,
                            iconCache = iconCache,
                            isPinned = isPinned,
                            showPinBadge = editPinsMode,
                            largeTargets = uiState.settings.largeTargets,
                            onClick = {
                                if (editPinsMode) onTogglePinApp(item.id) else onLaunchApp(item.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueSpotlightCard(
    docs: List<WorkspaceDocument>,
    strings: FlexStrings,
    onOpenDocument: (String) -> Unit,
    onNewDocument: () -> Unit
) {
    val tokens = LocalFlexDeskTokens.current
    val primaryDoc = docs.firstOrNull()

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = tokens.pillSurface.copy(alpha = 0.88f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, tokens.borderColor, RoundedCornerShape(16.dp))
    ) {
        if (primaryDoc == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNewDocument() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Description,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = strings.startNewDocTitle,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = strings.startNewDocSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(Icons.Rounded.Add, contentDescription = strings.newDoc)
            }
        } else {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onOpenDocument(primaryDoc.id) }
                            .padding(4.dp)
                            .testTag("continue_doc_${primaryDoc.id}")
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF4285F4),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Description,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = primaryDoc.title.ifBlank { "Untitled Note" },
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${strings.continueLabel} • ${primaryDoc.wordCount}w • ${primaryDoc.lastEditedDevice}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    TextButton(
                        onClick = onNewDocument,
                        modifier = Modifier.testTag("launcher_new_doc_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.newDoc)
                    }
                }

                if (docs.size > 1) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        docs.drop(1).forEach { doc ->
                            Surface(
                                onClick = { onOpenDocument(doc.id) },
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                                modifier = Modifier.testTag("continue_doc_${doc.id}")
                            ) {
                                Text(
                                    text = doc.title.ifBlank { "Untitled" },
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
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
private fun CircularAppShortcut(
    item: LauncherGridItem,
    iconCache: IconCacheManager,
    isPinned: Boolean,
    showPinBadge: Boolean,
    largeTargets: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cachedIcon: ImageBitmap? by produceState<ImageBitmap?>(initialValue = null, key1 = item.id) {
        value = if (!item.isBuiltin) iconCache.getOrLoadIcon(item.id) else null
    }

    val iconSize = if (largeTargets) 52.dp else 44.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .testTag("launcher_app_${item.id}")
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (cachedIcon != null) {
                Image(
                    bitmap = cachedIcon!!,
                    contentDescription = item.label,
                    modifier = Modifier
                        .size(iconSize)
                        .clip(CircleShape)
                )
            } else {
                val badgeBrush = brushForAppId(item.id)
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .clip(CircleShape)
                        .background(badgeBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.isBuiltin) iconForAppId(item.id) else Icons.Rounded.Apps,
                        contentDescription = item.label,
                        tint = Color.White,
                        modifier = Modifier.size(if (largeTargets) 26.dp else 22.dp)
                    )
                }
            }

            if (showPinBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPinned) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = if (isPinned) "Pinned" else "Pin",
                        tint = if (isPinned) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

fun brushForAppId(appId: String): Brush {
    return when (appId) {
        BuiltinAppId.DOCS.id -> Brush.linearGradient(listOf(Color(0xFF4285F4), Color(0xFF1A73E8)))
        BuiltinAppId.NOTES.id -> Brush.linearGradient(listOf(Color(0xFFFABB05), Color(0xFFF29900)))
        BuiltinAppId.FILES.id -> Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF0EA5E9)))
        BuiltinAppId.CALCULATOR.id -> Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))
        BuiltinAppId.FOCUS.id -> Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFEC4899)))
        BuiltinAppId.ACCOUNTS.id -> Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)))
        BuiltinAppId.SETTINGS.id -> Brush.linearGradient(listOf(Color(0xFF64748B), Color(0xFF334155)))
        BuiltinAppId.GOOGLE_PHOTOS.id -> Brush.linearGradient(listOf(Color(0xFFEA4335), Color(0xFFFBBC05), Color(0xFF34A853)))
        BuiltinAppId.GOOGLE_DRIVE.id -> Brush.linearGradient(listOf(Color(0xFF0F9D58), Color(0xFF4285F4)))
        BuiltinAppId.GMAIL.id -> Brush.linearGradient(listOf(Color(0xFFEA4335), Color(0xFFC5221F)))
        BuiltinAppId.GOOGLE_CALENDAR.id -> Brush.linearGradient(listOf(Color(0xFF1A73E8), Color(0xFF34A853)))
        BuiltinAppId.GOOGLE_KEEP.id -> Brush.linearGradient(listOf(Color(0xFFFBBC04), Color(0xFFE37400)))
        BuiltinAppId.GOOGLE_DOCS_WEB.id -> Brush.linearGradient(listOf(Color(0xFF1967D2), Color(0xFF4285F4)))
        BuiltinAppId.YOUTUBE.id -> Brush.linearGradient(listOf(Color(0xFFFF0000), Color(0xFFB91C1C)))
        BuiltinAppId.GOOGLE_MAPS.id -> Brush.linearGradient(listOf(Color(0xFF34A853), Color(0xFF1A73E8)))
        else -> Brush.linearGradient(listOf(Color(0xFF0EA5E9), Color(0xFF2563EB)))
    }
}
