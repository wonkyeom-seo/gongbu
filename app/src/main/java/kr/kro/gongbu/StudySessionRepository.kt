package kr.kro.gongbu

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class StudySession(
    val id: String,
    val date: String,
    val subject: String,
    val durationMillis: Long,
    val savedAtMillis: Long
)

class StudySessionRepository(context: Context) {
    private val file = File(context.applicationContext.filesDir, FILE_NAME)

    fun readSessions(): List<StudySession> = synchronized(lock) {
        if (!file.exists()) return@synchronized emptyList()

        runCatching {
            val array = JSONArray(file.readText())
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        StudySession(
                            id = item.optString("id"),
                            date = item.optString("date"),
                            subject = item.optString("subject"),
                            durationMillis = item.optLong("durationMillis"),
                            savedAtMillis = item.optLong("savedAtMillis")
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    fun addSession(date: String, subject: String, durationMillis: Long): StudySession? = synchronized(lock) {
        val trimmedSubject = subject.trim()
        if (date.isBlank() || trimmedSubject.isBlank() || durationMillis <= 0L) return@synchronized null

        val existing = readSessions()
        val session = StudySession(
            id = UUID.randomUUID().toString(),
            date = date,
            subject = trimmedSubject,
            durationMillis = durationMillis,
            savedAtMillis = System.currentTimeMillis()
        )
        writeSessions(existing + session)
        session
    }

    fun clearAll() = synchronized(lock) {
        if (file.exists()) {
            file.delete()
        }
    }

    private fun writeSessions(sessions: List<StudySession>) {
        val array = JSONArray()
        sessions.forEach { session ->
            array.put(
                JSONObject()
                    .put("id", session.id)
                    .put("date", session.date)
                    .put("subject", session.subject)
                    .put("durationMillis", session.durationMillis)
                    .put("savedAtMillis", session.savedAtMillis)
            )
        }
        file.writeText(array.toString(2))
    }

    companion object {
        private const val FILE_NAME = "study_sessions.json"
        private val lock = Any()
    }
}
