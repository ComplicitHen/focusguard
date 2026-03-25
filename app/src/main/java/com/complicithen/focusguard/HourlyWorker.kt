package com.complicithen.focusguard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlin.random.Random

class HourlyWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Focus reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Hourly check-ins to help you stay off your phone"
        }
        nm.createNotificationChannel(channel)

        val message = MESSAGES[Random.nextInt(MESSAGES.size)]

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("FocusGuard check-in")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        nm.notify(NOTIF_ID, notification)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "focus_reminders"
        const val NOTIF_ID = 1001

        val MESSAGES = listOf(
            "Still going strong — one more hour of focus.",
            "Phone check: Is this use intentional right now?",
            "You've been in control for another hour. Keep it up.",
            "Your attention is yours to give — spend it wisely.",
            "Hour check-in: You're building a great habit.",
            "Remember why you started this. Stay the course.",
            "Another hour done. You're stronger than the scroll.",
            "Stay present. The phone can wait.",
            "You control your phone — not the other way around.",
            "One hour at a time. You've got this.",
            "Check in: How are you feeling? Still focused?",
            "Great work. Another distraction-free hour behind you."
        )
    }
}
