package io.github.teouk.fomo.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import io.github.teouk.fomo.model.Event
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Schedules a notification before a planned event starts.
 *
 * WorkManager keeps the job even if the app is closed or the phone restarts. Android may
 * delay it slightly to save battery (Doze), so the reminder is approximate, not exact.
 * Each event gets a unique work name, so its reminder can be replaced or cancelled.
 */
class ReminderScheduler(
    context: Context,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val workManager = WorkManager.getInstance(context)

    fun schedule(event: Event, leadHours: Int) {
        val startsAt = event.startsAt ?: return
        val delay = reminderDelay(startsAt, leadHours, clock.instant()) ?: return
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    ReminderWorker.KEY_EVENT_ID to event.id,
                    ReminderWorker.KEY_EVENT_NAME to event.name,
                    ReminderWorker.KEY_VENUE_NAME to event.venue?.name,
                    ReminderWorker.KEY_STARTS_AT_MILLIS to startsAt.toEpochMilli(),
                ),
            )
            .build()
        workManager.enqueueUniqueWork(workName(event.id), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(eventId: String) {
        workManager.cancelUniqueWork(workName(eventId))
    }

    companion object {
        private fun workName(eventId: String) = "reminder_$eventId"

        /**
         * How long to wait before reminding: [leadHours] before the start. If that moment
         * has already passed but the event has not started, remind straight away.
         * Returns null for events that have already started.
         */
        fun reminderDelay(startsAt: Instant, leadHours: Int, now: Instant): Duration? {
            if (!startsAt.isAfter(now)) return null
            val remindAt = startsAt.minus(Duration.ofHours(leadHours.toLong()))
            return if (remindAt.isAfter(now)) Duration.between(now, remindAt) else Duration.ZERO
        }
    }
}
