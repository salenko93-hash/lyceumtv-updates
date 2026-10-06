package ua.edu.cunl.lyceummobile.data

import android.content.Context
import ua.edu.cunl.lyceummobile.core.WeekCycle

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("lyceummobile_settings", Context.MODE_PRIVATE)

    var selectedClass: String
        get() = prefs.getString("selectedClass", "10-А") ?: "10-А"
        set(value) = prefs.edit().putString("selectedClass", value).apply()

    var referenceMonday: String
        get() = prefs.getString("referenceMonday", WeekCycle.DEFAULT_REFERENCE)
            ?: WeekCycle.DEFAULT_REFERENCE
        set(value) = prefs.edit().putString("referenceMonday", value.trim()).apply()

    var googleSheetsUrl: String
        get() = prefs.getString("googleSheetsUrl", "") ?: ""
        set(value) = prefs.edit().putString("googleSheetsUrl", value.trim()).apply()

    var githubManifestUrl: String
        get() = prefs.getString("githubManifestUrl", "") ?: ""
        set(value) = prefs.edit().putString("githubManifestUrl", value.trim()).apply()

    var localManifestUrl: String
        get() = prefs.getString("localManifestUrl", "") ?: ""
        set(value) = prefs.edit().putString("localManifestUrl", value.trim()).apply()

    var source: String
        get() = prefs.getString("source", "GITHUB") ?: "GITHUB"
        set(value) = prefs.edit().putString("source", value).apply()

    var metronomeEnabled: Boolean
        get() = prefs.getBoolean("metronomeEnabled", true)
        set(value) = prefs.edit().putBoolean("metronomeEnabled", value).apply()

    var metronomeVolume: Float
        get() = prefs.getFloat("metronomeVolume", 0.25f).coerceIn(0f, 1f)
        set(value) = prefs.edit().putFloat("metronomeVolume", value.coerceIn(0f, 1f)).apply()

    val manifestUrl: String
        get() = if (source == "LOCAL") localManifestUrl else githubManifestUrl
}
