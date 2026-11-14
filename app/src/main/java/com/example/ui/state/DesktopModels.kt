package com.example.ui.state

import androidx.compose.runtime.Immutable
import com.example.ui.i18n.FlexStrings

enum class BuiltinAppId(
    val id: String,
    val label: String,
    val subtitle: String,
    val category: String,
    val webUrl: String? = null
) {
    DOCS("builtin:docs", "FlexDocs", "Offline-first docs with auto-save", "Productivity"),
    NOTES("builtin:notes", "Quick Notes", "Pinned cards & fast scratchpad", "Productivity"),
    FILES("builtin:files", "Files & Cache", "Offline storage & icon cache", "System"),
    CALCULATOR("builtin:calculator", "Calculator", "Instant desk math utility", "Utilities"),
    FOCUS("builtin:focus", "Focus Timer", "Deep work timer & DND", "Productivity"),
    ACCOUNTS("builtin:accounts", "Accounts", "Google sync & connected devices", "System"),
    SETTINGS("builtin:settings", "Appearance", "Dark mode, accents & Lite mode", "System"),

    // Google Shortcut Web Apps (open inside ChromeOS-style embedded Web App window)
    GOOGLE_PHOTOS("web:google_photos", "Google Photos", "Google Photos web app", "Google Web", "https://photos.google.com"),
    GOOGLE_DRIVE("web:google_drive", "Google Drive", "Cloud files & folders web app", "Google Web", "https://drive.google.com"),
    GMAIL("web:gmail", "Gmail", "Google Mail web app", "Google Web", "https://mail.google.com"),
    GOOGLE_CALENDAR("web:google_calendar", "Calendar", "Google Calendar web app", "Google Web", "https://calendar.google.com"),
    GOOGLE_KEEP("web:google_keep", "Google Keep", "Notes & lists web app", "Google Web", "https://keep.google.com"),
    GOOGLE_DOCS_WEB("web:google_docs_web", "Google Docs", "Google Docs web editor", "Google Web", "https://docs.google.com"),
    YOUTUBE("web:youtube", "YouTube", "YouTube video web app", "Google Web", "https://www.youtube.com"),
    GOOGLE_MAPS("web:google_maps", "Google Maps", "Maps & navigation web app", "Google Web", "https://maps.google.com");

    fun localizedLabel(strings: FlexStrings): String = when (this) {
        DOCS -> strings.appDocs
        NOTES -> strings.appNotes
        FILES -> strings.appFiles
        CALCULATOR -> strings.appCalculator
        FOCUS -> strings.appFocus
        ACCOUNTS -> strings.appAccounts
        SETTINGS -> strings.appSettings
        GOOGLE_PHOTOS -> strings.appGooglePhotos
        GOOGLE_DRIVE -> strings.appGoogleDrive
        GMAIL -> strings.appGmail
        GOOGLE_CALENDAR -> strings.appGoogleCalendar
        GOOGLE_KEEP -> strings.appGoogleKeep
        GOOGLE_DOCS_WEB -> strings.appGoogleDocsWeb
        YOUTUBE -> strings.appYouTube
        GOOGLE_MAPS -> strings.appGoogleMaps
    }

    companion object {
        fun fromId(id: String): BuiltinAppId? = entries.firstOrNull { it.id == id }

        fun localizeCategory(cat: String, strings: FlexStrings): String = when (cat) {
            "All" -> strings.catAll
            "Productivity" -> strings.catProductivity
            "Google Web" -> strings.catGoogleWeb
            "Utilities" -> strings.catUtilities
            "System" -> strings.catSystem
            else -> cat
        }
    }
}

enum class WindowSnapMode {
    FULL,
    LEFT,
    RIGHT
}

@Immutable
data class DesktopWindow(
    val windowId: String,
    val appId: String,
    val title: String,
    val isMinimized: Boolean = false,
    val snapMode: WindowSnapMode = WindowSnapMode.FULL,
    val documentId: String? = null
)

enum class AutoSaveStatus(val label: String) {
    IDLE("Ready"),
    SAVING("Saving draft..."),
    SAVED_LOCAL("Saved locally • Guest Mode"),
    SAVED_OFFLINE("Saved offline • Pending sync"),
    SYNCED("Saved & synced"),
    ERROR("Save queued locally")
}

@Immutable
data class ActiveDocumentDraft(
    val docId: String = "",
    val title: String = "",
    val content: String = "",
    val category: String = "Notes",
    val isPinned: Boolean = false,
    val isNew: Boolean = true
)
