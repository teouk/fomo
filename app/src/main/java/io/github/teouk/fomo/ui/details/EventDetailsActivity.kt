package io.github.teouk.fomo.ui.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.teouk.fomo.databinding.ActivityEventDetailsBinding

/** Event Details screen. Opened from any event card, on any tab, with that event's ID. */
class EventDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEventDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEventDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbarDetails.setNavigationOnClickListener { finish() }
    }

    companion object {
        private const val EXTRA_EVENT_ID = "io.github.teouk.fomo.extra.EVENT_ID"

        /**
         * Builds the Intent that opens the details of [eventId].
         * Only the ID is passed, not the whole event: the screen loads fresh data itself,
         * and Intents have a small size limit.
         */
        fun newIntent(context: Context, eventId: String): Intent =
            Intent(context, EventDetailsActivity::class.java).putExtra(EXTRA_EVENT_ID, eventId)
    }
}
