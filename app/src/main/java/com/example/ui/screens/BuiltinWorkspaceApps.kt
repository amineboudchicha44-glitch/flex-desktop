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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.WorkspaceDocument
import com.example.data.repository.WorkspaceRepository
import com.example.ui.state.AutoSaveStatus
import com.example.ui.theme.LocalFlexDeskTokens
import com.example.ui.viewmodel.WorkspaceUiState

@Composable
fun FlexDocsWindowContent(
    uiState: WorkspaceUiState,
    isExpandedScreen: Boolean,
    onSelectDoc: (String) -> Unit,
    onNewDoc: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onTogglePin: () -> Unit,
    onDeleteDoc: (String) -> Unit,
    onFlushSave: () -> Unit
) {
    val tokens = LocalFlexDeskTokens.current
    val draft = uiState.activeDraft

    if (isExpandedScreen) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Document Sidebar
            Column(
                modifier = Modifier
                    .width(240.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(12.dp)
            ) {
                Button(
                    onClick = { onNewDoc("Notes") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("docs_new_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Document")
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.documents, key = { it.id }) { doc ->
                        DocSidebarItem(
                            doc = doc,
                            isSelected = doc.id == draft?.docId,
                            onClick = { onSelectDoc(doc.id) }
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight(),
                color = tokens.borderColor
            )

            // Right Editor Pane
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                DocumentEditorPane(
                    uiState = uiState,
                    onTitleChange = onTitleChange,
                    onContentChange = onContentChange,
                    onCategoryChange = onCategoryChange,
                    onTogglePin = onTogglePin,
                    onDeleteDoc = onDeleteDoc,
                    onFlushSave = onFlushSave,
                    onNewDoc = { onNewDoc("Notes") }
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            // Compact top doc selector strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { onNewDoc("Notes") },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.testTag("docs_new_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "New Document", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New", style = MaterialTheme.typography.labelLarge)
                    }
                }

                uiState.documents.forEach { doc ->
                    FilterChip(
                        selected = doc.id == draft?.docId,
                        onClick = { onSelectDoc(doc.id) },
                        label = {
                            Text(
                                text = doc.title.ifBlank { "Untitled" },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            HorizontalDivider(color = tokens.borderColor)

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                DocumentEditorPane(
                    uiState = uiState,
                    onTitleChange = onTitleChange,
                    onContentChange = onContentChange,
                    onCategoryChange = onCategoryChange,
                    onTogglePin = onTogglePin,
                    onDeleteDoc = onDeleteDoc,
                    onFlushSave = onFlushSave,
                    onNewDoc = { onNewDoc("Notes") }
                )
            }
        }
    }
}

@Composable
private fun DocSidebarItem(
    doc: WorkspaceDocument,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (doc.isPinned) {
                    Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = "Pinned",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = doc.title.ifBlank { "Untitled Note" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${doc.category} • ${doc.wordCount} words",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DocumentEditorPane(
    uiState: WorkspaceUiState,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onTogglePin: () -> Unit,
    onDeleteDoc: (String) -> Unit,
    onFlushSave: () -> Unit,
    onNewDoc: () -> Unit
) {
    val tokens = LocalFlexDeskTokens.current
    val draft = uiState.activeDraft

    if (draft == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("No document open", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Create an offline-first document with instant auto-save and cross-device sync.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNewDoc) {
                Text("Create Document")
            }
        }
        return
    }

    val statusColor = when (uiState.autoSaveStatus) {
        AutoSaveStatus.SAVING -> MaterialTheme.colorScheme.primary
        AutoSaveStatus.SAVED_LOCAL -> MaterialTheme.colorScheme.primary
        AutoSaveStatus.SAVED_OFFLINE, AutoSaveStatus.ERROR -> tokens.offlineAmber
        AutoSaveStatus.SYNCED -> tokens.syncGreen
        AutoSaveStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Auto-save status & toolbar row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                onClick = onFlushSave,
                shape = RoundedCornerShape(50),
                color = statusColor.copy(alpha = 0.14f),
                modifier = Modifier.testTag("autosave_status_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (uiState.autoSaveStatus) {
                            AutoSaveStatus.SYNCED -> Icons.Rounded.CloudDone
                            AutoSaveStatus.SAVING -> Icons.Rounded.Save
                            else -> Icons.Rounded.CloudQueue
                        },
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = uiState.autoSaveStatus.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${WorkspaceRepository.countWords(draft.content)} words",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier.testTag("doc_pin_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = if (draft.isPinned) "Unpin Document" else "Pin Document",
                        tint = if (draft.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { onDeleteDoc(draft.docId) },
                    modifier = Modifier.testTag("doc_delete_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete Document",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Category tags
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Notes", "Work", "Tasks", "Scratchpad").forEach { cat ->
                FilterChip(
                    selected = draft.category == cat,
                    onClick = { onCategoryChange(cat) },
                    label = { Text(cat) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Title input
        BasicTextField(
            value = draft.title,
            onValueChange = onTitleChange,
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = true,
            decorationBox = { inner ->
                if (draft.title.isEmpty()) {
                    Text(
                        text = "Document title...",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                inner()
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("doc_title_input")
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = tokens.borderColor)

        // Content editor
        BasicTextField(
            value = draft.content,
            onValueChange = onContentChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                Box(modifier = Modifier.fillMaxSize()) {
                    if (draft.content.isEmpty()) {
                        Text(
                            text = "Start typing... Auto-save writes safely offline every 600ms and syncs across all your devices.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("doc_content_input")
        )
    }
}

@Composable
fun QuickNotesWindowContent(
    uiState: WorkspaceUiState,
    onCreateNote: () -> Unit,
    onOpenInDocs: (String) -> Unit
) {
    val tokens = LocalFlexDeskTokens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Quick Notes Board", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Offline-cached notes synced across Chromebook & Android",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onCreateNote,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("quick_notes_add_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Note")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 190.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(uiState.documents, key = { it.id }) { doc ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, tokens.borderColor, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenInDocs(doc.id) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = doc.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (doc.isPinned) {
                                Icon(
                                    imageVector = Icons.Rounded.PushPin,
                                    contentDescription = "Pinned",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = doc.title.ifBlank { "Untitled Note" },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = doc.content.ifBlank { "Tap to start writing..." },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Edited on ${doc.lastEditedDevice}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilesAndCacheWindowContent(
    uiState: WorkspaceUiState,
    onOpenDocument: (String) -> Unit,
    onRebuildIconCache: () -> Unit
) {
    val tokens = LocalFlexDeskTokens.current
    val hw = uiState.hardwareProfile
    val stats = uiState.iconCacheStats
    val usedRamMb = ((hw?.totalRamMb ?: 4096L) - (hw?.availRamMb ?: 2048L)).coerceAtLeast(0L)
    val totalRamMb = (hw?.totalRamMb ?: 4096L).coerceAtLeast(1L)
    val ramRatio = (usedRamMb.toFloat() / totalRamMb.toFloat()).coerceIn(0f, 1f)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("Files, Memory & Persistent Icon Cache", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Optimized for fast startup and smooth execution on low-RAM hardware like Dell Chromebook 11.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

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
                        Icon(Icons.Rounded.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hardware Memory Budget", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { ramRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$usedRamMb MB used / $totalRamMb MB total • Low-RAM Profile: ${if (hw?.isLowRamDevice == true) "Yes" else "Standard"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Persistent Icon Cache", style = MaterialTheme.typography.titleMedium)
                        }
                        OutlinedButton(
                            onClick = onRebuildIconCache,
                            modifier = Modifier.testTag("rebuild_icon_cache_button")
                        ) {
                            Icon(Icons.Rounded.Cached, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rebuild")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Disk PNG Icons: ${stats.diskCachedCount} (${stats.diskSizeKb} KB) • LruCache Budget: ${stats.memoryBudgetKb} KB",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Warm-up Time: ${stats.warmUpMs} ms • Memory Hits: ${stats.memoryHits} • Disk Hits: ${stats.diskHits}",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.syncGreen
                    )
                }
            }
        }

        item {
            Text(
                text = "Offline Synced Documents (${uiState.documents.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        items(uiState.documents, key = { it.id }) { doc ->
            Surface(
                onClick = { onOpenDocument(doc.id) },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Rounded.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(doc.title.ifBlank { "Untitled Note" }, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${doc.category} • ${doc.wordCount} words • ${doc.lastEditedDevice}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = if (uiState.hasPendingWrites || !uiState.networkStatus.isOnline) "Cached" else "Synced",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.hasPendingWrites || !uiState.networkStatus.isOnline) tokens.offlineAmber else tokens.syncGreen
                    )
                }
            }
        }
    }
}
