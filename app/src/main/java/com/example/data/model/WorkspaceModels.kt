package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ThemeMode(val key: String, val label: String) {
    SYSTEM("system", "System"),
    DARK("dark", "Dark"),
    LIGHT("light", "Light"),
    AMOLED("amoled", "AMOLED Pitch");

    companion object {
        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.key == key } ?: DARK
    }
}

enum class AccentPalette(val key: String, val label: String) {
    CHROME_BLUE("chrome_blue", "Chrome Blue"),
    AURORA_MINT("aurora_mint", "Aurora Mint"),
    AMBER_DUSK("amber_dusk", "Amber Dusk"),
    ROSE_QUARTZ("rose_quartz", "Rose Quartz"),
    SLATE_MONO("slate_mono", "Slate Mono");

    companion object {
        fun fromKey(key: String?): AccentPalette =
            entries.firstOrNull { it.key == key } ?: CHROME_BLUE
    }
}

@IgnoreExtraProperties
data class UserSettings(
    val userId: String = "",
    val displayName: String? = null,
    val email: String? = null,
    val themeMode: String = ThemeMode.DARK.key,
    val accentPalette: String = AccentPalette.CHROME_BLUE.key,
    val fontScale: Double = 1.0,
    val reduceMotion: Boolean = false,
    val highContrast: Boolean = false,
    val largeTargets: Boolean = false,
    val liteMode: Boolean = false,
    val pinnedApps: List<String> = listOf("builtin:docs", "builtin:notes", "builtin:files", "builtin:calculator", "builtin:focus"),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WorkspaceDocument(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val category: String = "Notes",
    val isPinned: Boolean = false,
    val wordCount: Long = 0L,
    val lastEditedDevice: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ConnectedDevice(
    @DocumentId val id: String = "",
    val userId: String = "",
    val deviceName: String = "",
    val manufacturer: String = "",
    val formFactor: String = "phone",
    val osVersion: String = "",
    val isLowRamDevice: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class SyncedDocumentsSnapshot(
    val documents: List<WorkspaceDocument> = emptyList(),
    val hasPendingWrites: Boolean = false,
    val isFromCache: Boolean = false
)
