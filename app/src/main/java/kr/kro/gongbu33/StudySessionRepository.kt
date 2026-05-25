package kr.kro.gongbu33

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
    val startMillis: Long,
    val endMillis: Long,
    val breakMillis: Long,
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
                    val durationMillis = item.optLong("durationMillis")
                    val savedAtMillis = item.optLong("savedAtMillis")
                    val fallbackEndMillis = savedAtMillis.takeIf { it > 0L } ?: 0L
                    val endMillis = item.optLong("endMillis", fallbackEndMillis)
                    val startMillis = item.optLong(
                        "startMillis",
                        (endMillis - durationMillis).coerceAtLeast(0L)
                    )
                    add(
                        StudySession(
                            id = item.optString("id"),
                            date = item.optString("date"),
                            subject = item.optString("subject"),
                            durationMillis = durationMillis,
                            startMillis = startMillis,
                            endMillis = endMillis,
                            breakMillis = item.optLong("breakMillis", 0L),
                            savedAtMillis = savedAtMillis
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    fun addSession(
        date: String,
        subject: String,
        durationMillis: Long,
        startMillis: Long,
        endMillis: Long,
        breakMillis: Long
    ): StudySession? = synchronized(lock) {
        val trimmedSubject = subject.trim()
        if (date.isBlank() || trimmedSubject.isBlank() || durationMillis <= 0L) return@synchronized null

        val existing = readSessions()
        val session = StudySession(
            id = UUID.randomUUID().toString(),
            date = date,
            subject = trimmedSubject,
            durationMillis = durationMillis,
            startMillis = startMillis,
            endMillis = endMillis,
            breakMillis = breakMillis.coerceAtLeast(0L),
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

    fun replaceAll(sessions: List<StudySession>) = synchronized(lock) {
        writeSessions(sessions)
    }

    private fun writeSessions(sessions: List<StudySession>) {
        val array = JSONArray()
        sessions.forEach { session ->
            array.put(session.toJson())
        }
        file.writeText(array.toString(2))
    }

    companion object {
        private const val FILE_NAME = "study_sessions.json"
        private val lock = Any()

        fun fromJson(item: JSONObject): StudySession {
            val durationMillis = item.optLong("durationMillis")
            val savedAtMillis = item.optLong("savedAtMillis")
            val fallbackEndMillis = savedAtMillis.takeIf { it > 0L } ?: 0L
            val endMillis = item.optLong("endMillis", fallbackEndMillis)
            val startMillis = item.optLong(
                "startMillis",
                (endMillis - durationMillis).coerceAtLeast(0L)
            )
            return StudySession(
                id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                date = item.optString("date"),
                subject = item.optString("subject"),
                durationMillis = durationMillis,
                startMillis = startMillis,
                endMillis = endMillis,
                breakMillis = item.optLong("breakMillis", 0L),
                savedAtMillis = savedAtMillis
            )
        }
    }
}

fun StudySession.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("date", date)
        .put("subject", subject)
        .put("durationMillis", durationMillis)
        .put("startMillis", startMillis)
        .put("endMillis", endMillis)
        .put("breakMillis", breakMillis)
        .put("savedAtMillis", savedAtMillis)
}
