package kr.kro.gongbu33

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class StudyTimerService : Service() {
    private val repository by lazy { StudySessionRepository(this) }
    private val handler = Handler(Looper.getMainLooper())
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }
    private val notificationUpdater = object : Runnable {
        override fun run() {
            val snapshot = TimerStateStore.snapshot(this@StudyTimerService)
            if (!snapshot.active) return

            notificationManager.notify(NOTIFICATION_ID, buildNotification(snapshot))
            if (snapshot.running) {
                handler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val date = intent.getStringExtra(EXTRA_DATE).orEmpty()
                val subject = intent.getStringExtra(EXTRA_SUBJECT).orEmpty()
                if (date.isNotBlank() && subject.isNotBlank()) {
                    TimerStateStore.start(this, date, subject)
                }
            }

            ACTION_PAUSE -> TimerStateStore.pause(this)
            ACTION_RESUME -> TimerStateStore.resume(this)
            ACTION_SAVE -> saveAndStop()
            ACTION_STOP -> {
                TimerStateStore.clear(this)
                handler.removeCallbacks(notificationUpdater)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val snapshot = TimerStateStore.snapshot(this)
        return if (snapshot.active) {
            startForeground(NOTIFICATION_ID, buildNotification(snapshot))
            scheduleNotificationUpdates(snapshot)
            START_STICKY
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            START_NOT_STICKY
        }
    }

    private fun saveAndStop() {
        val snapshot = TimerStateStore.snapshot(this)
        if (snapshot.active) {
            val endMillis = System.currentTimeMillis()
            repository.addSession(
                date = snapshot.date,
                subject = snapshot.subject,
                durationMillis = snapshot.elapsedMillis(),
                startMillis = snapshot.startedAtWallMillis,
                endMillis = endMillis,
                breakMillis = snapshot.breakMillis(endMillis)
            )
        }
        TimerStateStore.clear(this)
        handler.removeCallbacks(notificationUpdater)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(notificationUpdater)
        super.onDestroy()
    }

    private fun buildNotification(snapshot: TimerSnapshot): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val toggleAction = if (snapshot.running) ACTION_PAUSE else ACTION_RESUME
        val toggleLabel = if (snapshot.running) "멈춤" else "계속"

        val elapsed = snapshot.elapsedMillis()
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle("${snapshot.subject} ${if (snapshot.running) "측정 중" else "일시정지"}")
            .setContentText("${snapshot.date} · ${formatDuration(elapsed)}")
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, toggleLabel, servicePendingIntent(toggleAction, 10))
            .addAction(0, "저장", servicePendingIntent(ACTION_SAVE, 11))

        if (!snapshot.running) {
            builder.addAction(0, "초기화", servicePendingIntent(ACTION_STOP, 12))
        }

        if (snapshot.running) {
            val baseTime = System.currentTimeMillis() - elapsed
            builder
                .setWhen(baseTime)
                .setUsesChronometer(true)
                .setChronometerCountDown(false)
        }

        return builder.build()
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        return PendingIntent.getService(
            this,
            requestCode,
            Intent(this, StudyTimerService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "순공 타이머",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "백그라운드 공부 시간 측정과 알림 제어"
            setShowBadge(false)
        }

        notificationManager.createNotificationChannel(channel)
    }

    private fun scheduleNotificationUpdates(snapshot: TimerSnapshot) {
        handler.removeCallbacks(notificationUpdater)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(snapshot))
        if (snapshot.running) {
            handler.postDelayed(notificationUpdater, 1000L)
        }
    }

    companion object {
        const val ACTION_START = "kr.kro.gongbu33.action.START"
        const val ACTION_PAUSE = "kr.kro.gongbu33.action.PAUSE"
        const val ACTION_RESUME = "kr.kro.gongbu33.action.RESUME"
        const val ACTION_SAVE = "kr.kro.gongbu33.action.SAVE"
        const val ACTION_STOP = "kr.kro.gongbu33.action.STOP"
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_SUBJECT = "extra_subject"

        private const val CHANNEL_ID = "study_timer"
        private const val NOTIFICATION_ID = 1001

        fun startIntent(context: Context, date: String, subject: String): Intent {
            return Intent(context, StudyTimerService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_DATE, date)
                .putExtra(EXTRA_SUBJECT, subject)
        }

        fun actionIntent(context: Context, action: String): Intent {
            return Intent(context, StudyTimerService::class.java).setAction(action)
        }
    }
}

fun formatDuration(durationMillis: Long): String {
    val totalSeconds = (durationMillis / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}
