package com.example.grandprixhub

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class NotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        // 1. Retrieve the session and race names passed from the ViewModel
        val sessionName = inputData.getString("SESSION_NAME") ?: "F1 Session"
        val raceName = inputData.getString("RACE_NAME") ?: "Grand Prix"

        // 2. Create a unique ID so different sessions don't overwrite each other
        val notificationId = (raceName + sessionName).hashCode()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 3. Ensure the Notification Channel matches the ID we set in MainActivity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("F1_NOTIFS", "Race Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        // 4. Build the actual visual alert
        val notification = NotificationCompat.Builder(applicationContext, "F1_NOTIFS")
            .setContentTitle(sessionName)
            .setContentText("$raceName starts in 15 mins!")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // 5. Fire the notification
        notificationManager.notify(notificationId, notification)

        return Result.success()
    }
}