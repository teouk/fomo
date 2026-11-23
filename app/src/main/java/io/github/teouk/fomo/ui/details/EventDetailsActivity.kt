package io.github.teouk.fomo.ui.details

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil3.load
import coil3.request.crossfade
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import io.github.teouk.fomo.R
import io.github.teouk.fomo.databinding.ActivityEventDetailsBinding
import io.github.teouk.fomo.model.Event
import io.github.teouk.fomo.model.WeatherCondition
import io.github.teouk.fomo.model.WeatherForecast
import io.github.teouk.fomo.ui.common.EventCardBinder
import io.github.teouk.fomo.ui.common.EventFormatter
import io.github.teouk.fomo.ui.common.messageRes
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.ZoneId

/** Event Details screen. Opened from any event card, on any tab, with that event's ID. */
class EventDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEventDetailsBinding
    private val viewModel: EventDetailsViewModel by viewModels { EventDetailsViewModel.Factory }
    private var descriptionExpanded = false

    /**
     * Android 13+ asks the user before an app may post notifications. The result
     * arrives here; the plan is added either way, only the reminder depends on it.
     */
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) Snackbar.make(binding.root, R.string.reminders_off, Snackbar.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEventDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = systemBars.left, top = systemBars.top, right = systemBars.right)
            binding.fabAddToPlans.translationY = -systemBars.bottom.toFloat()
            insets
        }

        binding.toolbarDetails.setNavigationOnClickListener { finish() }
        binding.toolbarDetails.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_save_event) viewModel.toggleSave()
            item.itemId == R.id.action_save_event
        }
        binding.fabAddToPlans.setOnClickListener {
            val adding = !viewModel.uiState.value.status.isPlanned
            viewModel.togglePlan()
            if (adding) askForNotificationsIfNeeded()
        }
        binding.buttonDetailsRetry.setOnClickListener { viewModel.load() }
        binding.buttonReadMore.setOnClickListener {
            descriptionExpanded = !descriptionExpanded
            updateDescription()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.messages.collect(::showMessage) }
            }
        }
    }

    private fun render(state: EventDetailsUiState) {
        val event = state.event
        binding.progressDetails.isVisible = state.isLoading && event == null
        binding.layoutDetailsMessage.isVisible = state.error != null && event == null
        state.error?.let { binding.textDetailsMessage.setText(it.messageRes()) }
        binding.scrollDetails.isVisible = event != null
        binding.fabAddToPlans.isVisible = event != null

        val saveItem = binding.toolbarDetails.menu.findItem(R.id.action_save_event)
        saveItem.isVisible = event != null
        saveItem.setIcon(if (state.status.isSaved) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border)
        saveItem.setTitle(if (state.status.isSaved) R.string.action_unsave_event else R.string.action_save_event)

        binding.fabAddToPlans.setText(if (state.status.isPlanned) R.string.action_in_plans else R.string.action_add_to_plans)
        binding.fabAddToPlans.setIconResource(
            if (state.status.isPlanned) R.drawable.ic_event_available else R.drawable.ic_event_note,
        )

        if (event != null) showEvent(event)
        showWeather(state.weather)
    }

    private fun showEvent(event: Event) {
        binding.imageEvent.load(event.imageUrl) { crossfade(true) }
        binding.textEventTitle.text = event.name
        binding.textEventDateTime.text = event.localDate?.let { date ->
            val day = EventFormatter.longDay(date)
            event.localTime?.let { "$day · ${EventFormatter.time(it)}" } ?: day
        } ?: getString(R.string.date_tbc)

        val venue = event.venue
        binding.textEventVenue.isVisible = venue != null
        binding.textEventVenue.text = venue?.let { v -> v.city?.let { getString(R.string.venue_with_city, v.name, it) } ?: v.name }
        binding.textEventPrice.text = EventCardBinder.priceText(this, event.minPrice, event.currency)

        binding.buttonGetTickets.isVisible = event.ticketUrl != null
        binding.buttonGetTickets.setOnClickListener { event.ticketUrl?.let { openUri(Uri.parse(it)) } }

        binding.textEventDescription.text = event.description
        updateDescription()

        val lat = venue?.latitude
        val lon = venue?.longitude
        binding.cardMap.isVisible = venue != null
        binding.textMapAddress.text = listOfNotNull(venue?.name, venue?.address, venue?.city).joinToString(", ")
        // The map preview image arrives with the Map tab; until then only the address shows.
        binding.imageMap.isVisible = false
        binding.textMapCredit.isVisible = false
        binding.buttonDirections.isVisible = lat != null && lon != null
        binding.buttonDirections.setOnClickListener {
            // Hands over to Google Maps (or the browser if Maps is not installed).
            openUri(Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lon"))
        }
    }

    private fun updateDescription() {
        val hasDescription = !binding.textEventDescription.text.isNullOrBlank()
        binding.textEventDescription.maxLines = if (descriptionExpanded) Int.MAX_VALUE else COLLAPSED_LINES
        binding.buttonReadMore.setText(if (descriptionExpanded) R.string.action_show_less else R.string.action_read_more)
        // Only offer "Read more" when the text is actually cut off.
        binding.textEventDescription.post {
            val layout = binding.textEventDescription.layout
            val isCut = layout != null && layout.lineCount > 0 && layout.getEllipsisCount(layout.lineCount - 1) > 0
            binding.buttonReadMore.isVisible = hasDescription && (isCut || descriptionExpanded)
        }
    }

    private fun showWeather(forecast: WeatherForecast?) {
        binding.cardWeather.isVisible = forecast != null
        if (forecast == null) return
        val condition = getString(forecast.condition.labelRes())
        val rain = forecast.precipitationChancePercent
        binding.textWeatherSummary.text = if (rain != null && rain > 0) {
            getString(R.string.weather_summary_with_rain, condition, forecast.temperatureCelsius, rain)
        } else {
            getString(R.string.weather_summary, condition, forecast.temperatureCelsius)
        }
        val local = forecast.time.atZone(ZoneId.systemDefault())
        binding.textWeatherTime.text = getString(
            R.string.weather_time,
            EventFormatter.dateTime(local.toLocalDate(), local.toLocalTime()),
        )
        // The only weather icon in the design is the umbrella, shown when it may be wet.
        binding.imageWeather.isVisible = forecast.condition.isWet
    }

    private fun showMessage(message: DetailsMessage) {
        when (message) {
            is DetailsMessage.PlanAdded ->
                Snackbar.make(binding.root, R.string.plan_added, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_add_to_calendar) { addToCalendar(message.event) }
                    .show()
            DetailsMessage.PlanRemoved -> Snackbar.make(binding.root, R.string.plan_removed, Snackbar.LENGTH_SHORT).show()
            is DetailsMessage.SaveToggled -> Snackbar.make(
                binding.root,
                if (message.isSaved) R.string.event_saved else R.string.event_unsaved,
                Snackbar.LENGTH_SHORT,
            ).show()
        }
    }

    /** Asks for the notification permission at the moment it is needed, with a reason first. */
    private fun askForNotificationsIfNeeded() {
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) return
        if (shouldShowRequestPermissionRationale(permission)) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.notification_rationale_title)
                .setMessage(R.string.notification_rationale_body)
                .setPositiveButton(R.string.action_allow) { _, _ -> notificationPermission.launch(permission) }
                .setNegativeButton(R.string.action_not_now, null)
                .show()
        } else {
            notificationPermission.launch(permission)
        }
    }

    /**
     * Opens the phone's calendar with the event filled in. The user confirms there,
     * so FOMO needs no calendar permission for this.
     */
    private fun addToCalendar(event: Event) {
        val start = event.startsAt ?: return
        val end = start.plus(Duration.ofHours(ASSUMED_EVENT_DURATION_HOURS))
        val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start.toEpochMilli())
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end.toEpochMilli())
            .putExtra(CalendarContract.Events.TITLE, event.name)
            .putExtra(CalendarContract.Events.EVENT_LOCATION, event.venue?.name)
        startOrExplain(intent)
    }

    private fun openUri(uri: Uri) = startOrExplain(Intent(Intent.ACTION_VIEW, uri))

    private fun startOrExplain(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.error_no_app, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_EVENT_ID = "io.github.teouk.fomo.extra.EVENT_ID"
        private const val COLLAPSED_LINES = 4

        /** Ticketmaster rarely gives an end time, so calendar entries assume 3 hours. */
        private const val ASSUMED_EVENT_DURATION_HOURS = 3L

        /**
         * Builds the Intent that opens the details of [eventId].
         * Only the ID is passed, not the whole event: the screen loads fresh data itself,
         * and Intents have a small size limit.
         */
        fun newIntent(context: Context, eventId: String): Intent =
            Intent(context, EventDetailsActivity::class.java).putExtra(EXTRA_EVENT_ID, eventId)
    }
}

private fun WeatherCondition.labelRes(): Int = when (this) {
    WeatherCondition.CLEAR -> R.string.weather_clear
    WeatherCondition.MAINLY_CLEAR -> R.string.weather_mainly_clear
    WeatherCondition.PARTLY_CLOUDY -> R.string.weather_partly_cloudy
    WeatherCondition.OVERCAST -> R.string.weather_overcast
    WeatherCondition.FOG -> R.string.weather_fog
    WeatherCondition.DRIZZLE -> R.string.weather_drizzle
    WeatherCondition.RAIN -> R.string.weather_rain
    WeatherCondition.SNOW -> R.string.weather_snow
    WeatherCondition.THUNDERSTORM -> R.string.weather_thunderstorm
    WeatherCondition.UNKNOWN -> R.string.weather_unknown
}
