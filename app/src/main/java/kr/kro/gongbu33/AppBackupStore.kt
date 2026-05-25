package kr.kro.gongbu33

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object AppBackupStore {
    fun exportJson(
        context: Context,
        repository: StudySessionRepository,
        themeSettings: ThemeSettings
    ): String {
        val sessions = JSONArray()
        repository.readSessions().forEach { session ->
            sessions.put(session.toJson())
        }

        return JSONObject()
            .put("schemaVersion", 1)
            .put("packageName", context.packageName)
            .put("exportedAtMillis", System.currentTimeMillis())
            .put(
                "themeSettings",
                JSONObject()
                    .put("presetId", themeSettings.presetId)
                    .put("backgroundArgb", themeSettings.backgroundArgb)
                    .put("textArgb", themeSettings.textArgb)
            )
            .put("studySessions", sessions)
            .toString(2)
    }

    fun importJson(
        context: Context,
        repository: StudySessionRepository,
        json: String
    ): ThemeSettings {
        val root = JSONObject(json)
        val sessionsArray = root.optJSONArray("studySessions")
            ?: root.optJSONArray("sessions")
            ?: JSONArray()
        val sessions = buildList {
            for (index in 0 until sessionsArray.length()) {
                val session = StudySessionRepository.fromJson(sessionsArray.getJSONObject(index))
                if (session.date.isNotBlank() && session.subject.isNotBlank() && session.durationMillis > 0L) {
                    add(session)
                }
            }
        }

        val currentTheme = ThemeSettingsStore.read(context)
        val themeJson = root.optJSONObject("themeSettings")
        val themeSettings = if (themeJson != null) {
            ThemeSettings(
                presetId = themeJson.optString("presetId", currentTheme.presetId),
                backgroundArgb = themeJson.optInt("backgroundArgb", currentTheme.backgroundArgb),
                textArgb = themeJson.optInt("textArgb", currentTheme.textArgb)
            )
        } else {
            currentTheme
        }

        repository.replaceAll(sessions)
        return ThemeSettingsStore.saveSettings(context, themeSettings)
    }
}
