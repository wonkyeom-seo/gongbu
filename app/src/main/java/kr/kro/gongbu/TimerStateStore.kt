package kr.kro.gongbu

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class TimerSnapshot(
    val active: Boolean,
    val date: String,
    val subject: String,
    val accumulatedMillis: Long,
    val startedAtElapsedMillis: Long,
    val running: Boolean
) {
    fun elapsedMillis(nowElapsedMillis: Long = SystemClock.elapsedRealtime()): Long {
        if (!active) return 0L
        return if (running) {
            accumulatedMillis + (nowElapsedMillis - startedAtElapsedMillis).coerceAtLeast(0L)
        } else {
            accumulatedMillis
        }
    }
}

object TimerStateStore {
    private const val PREFS = "study_timer_state"
    private const val KEY_ACTIVE = "active"
    private const val KEY_DATE = "date"
    private const val KEY_SUBJECT = "subject"
    private const val KEY_ACCUMULATED = "accumulated"
    private const val KEY_STARTED_AT = "started_at"
    private const val KEY_RUNNING = "running"

    fun todayString(): String = dateFormat().format(Calendar.getInstance().time)

    fun shiftDate(date: String, days: Int): String {
        val calendar = Calendar.getInstance()
        runCatching {
            calendar.time = dateFormat().parse(date) ?: calendar.time
        }
        calendar.add(Calendar.DAY_OF_MONTH, days)
        return dateFormat().format(calendar.time)
    }

    fun millisToDateString(millis: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        return dateFormat().format(calendar.time)
    }

    fun start(context: Context, date: String, subject: String) {
        prefs(context).edit()
            .putBoolean(KEY_ACTIVE, true)
            .putString(KEY_DATE, date)
            .putString(KEY_SUBJECT, subject.trim())
            .putLong(KEY_ACCUMULATED, 0L)
            .putLong(KEY_STARTED_AT, SystemClock.elapsedRealtime())
            .putBoolean(KEY_RUNNING, true)
            .commit()
    }

    fun pause(context: Context) {
        val snapshot = snapshot(context)
        if (!snapshot.active || !snapshot.running) return

        prefs(context).edit()
            .putLong(KEY_ACCUMULATED, snapshot.elapsedMillis())
            .putLong(KEY_STARTED_AT, 0L)
            .putBoolean(KEY_RUNNING, false)
            .commit()
    }

    fun resume(context: Context) {
        val snapshot = snapshot(context)
        if (!snapshot.active || snapshot.running) return

        prefs(context).edit()
            .putLong(KEY_STARTED_AT, SystemClock.elapsedRealtime())
            .putBoolean(KEY_RUNNING, true)
            .commit()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().commit()
    }

    fun snapshot(context: Context): TimerSnapshot {
        val prefs = prefs(context)
        val active = prefs.getBoolean(KEY_ACTIVE, false)
        return TimerSnapshot(
            active = active,
            date = prefs.getString(KEY_DATE, todayString()).orEmpty(),
            subject = prefs.getString(KEY_SUBJECT, "").orEmpty(),
            accumulatedMillis = prefs.getLong(KEY_ACCUMULATED, 0L),
            startedAtElapsedMillis = prefs.getLong(KEY_STARTED_AT, 0L),
            running = active && prefs.getBoolean(KEY_RUNNING, false)
        )
    }

    private fun prefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    private fun dateFormat(): SimpleDateFormat {
        return DATE_FORMAT.get() ?: SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
    }

    private val DATE_FORMAT = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
        }
    }
}
