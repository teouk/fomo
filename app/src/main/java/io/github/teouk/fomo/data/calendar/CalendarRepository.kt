package io.github.teouk.fomo.data.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

/** Where calendar entries come from. An interface, so tests can use a fake calendar. */
interface CalendarSource {
    fun hasPermission(): Boolean

    suspend fun entriesBetween(from: Instant, to: Instant): List<CalendarEntry>
}

/**
 * Reads the phone's calendar through Android's Calendar Provider.
 *
 * It queries CalendarContract.Instances rather than Events: Events stores a weekly
 * lecture once, with a repeat rule, while Instances lists every single week's
 * occurrence with its real start and end. (CW1 design decision.)
 *
 * Calendar data is only read on the phone and never sent anywhere.
 */
class CalendarRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : CalendarSource {
    override fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    /** Every calendar entry overlapping [from]..[to], or an empty list without permission. */
    override suspend fun entriesBetween(from: Instant, to: Instant): List<CalendarEntry> {
        if (!hasPermission()) return emptyList()
        return withContext(ioDispatcher) {
            val cursor = try {
                CalendarContract.Instances.query(
                    context.contentResolver,
                    PROJECTION,
                    from.toEpochMilli(),
                    to.toEpochMilli(),
                )
            } catch (e: SecurityException) {
                // Permission was revoked between the check and the query.
                null
            }
            cursor?.use { rows ->
                buildList {
                    while (rows.moveToNext()) {
                        add(
                            CalendarEntry(
                                title = rows.getString(COLUMN_TITLE).orEmpty(),
                                begin = Instant.ofEpochMilli(rows.getLong(COLUMN_BEGIN)),
                                end = Instant.ofEpochMilli(rows.getLong(COLUMN_END)),
                                isAllDay = rows.getInt(COLUMN_ALL_DAY) == 1,
                                isFree = rows.getInt(COLUMN_AVAILABILITY) == CalendarContract.Events.AVAILABILITY_FREE,
                                isDeclined = rows.getInt(COLUMN_SELF_STATUS) ==
                                    CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED,
                            ),
                        )
                    }
                }
            }.orEmpty()
        }
    }

    private companion object {
        val PROJECTION = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.AVAILABILITY,
            CalendarContract.Instances.SELF_ATTENDEE_STATUS,
        )

        // Positions in PROJECTION, so the cursor is read by index rather than by name each row.
        const val COLUMN_TITLE = 0
        const val COLUMN_BEGIN = 1
        const val COLUMN_END = 2
        const val COLUMN_ALL_DAY = 3
        const val COLUMN_AVAILABILITY = 4
        const val COLUMN_SELF_STATUS = 5
    }
}
