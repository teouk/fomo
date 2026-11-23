package io.github.teouk.fomo.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.teouk.fomo.R
import io.github.teouk.fomo.ui.common.EventFormatter
import io.github.teouk.fomo.ui.details.EventDetailsActivity
import java.time.Instant
import java.time.ZoneId

/** Runs when a reminder is due and shows the notification. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val eventId = inputData.getString(KEY_EVENT_ID) ?: return Result.failure()
        val name = inputData.getString(KEY_EVENT_NAME) ?: return Result.failure()
        val venue = inputData.getString(KEY_VENUE_NAME)
        val startsAt = Instant.ofEpochMilli(inputData.getLong(KEY_STARTS_AT_MILLIS, 0))

        // Without the permission (Android 13+) the notification would be dropped anyway.
        val granted = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return Result.success()

        val time = EventFormatter.time(startsAt.atZone(ZoneId.systemDefault()).toLocalTime())
        val body = if (venue != null) {
            applicationContext.getString(R.string.reminder_body, venue, time)
        } else {
            applicationContext.getString(R.string.reminder_body_no_venue, time)
        }

        // Tapping the notification opens the event's details.
        val openDetails = PendingIntent.getActivity(
            applicationContext,
            eventId.hashCode(),
            EventDetailsActivity.newIntent(applicationContext, eventId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_event_note)
            .setContentTitle(applicationContext.getString(R.string.reminder_title, name))
            .setContentText(body)
            .setContentIntent(openDetails)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(eventId.hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "event_reminders"
        const val KEY_EVENT_ID = "event_id"
        const val KEY_EVENT_NAME = "event_name"
        const val KEY_VENUE_NAME = "venue_name"
        const val KEY_STARTS_AT_MILLIS = "starts_at_millis"

        /**
         * Android 8+ needs a channel before any notification is shown. Users can mute
         * a channel in system settings. Creating it again is harmless.
         */
        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.reminder_channel_description) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
