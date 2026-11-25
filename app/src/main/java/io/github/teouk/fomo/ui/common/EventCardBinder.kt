package io.github.teouk.fomo.ui.common

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import androidx.core.view.isVisible
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import io.github.teouk.fomo.R

/**
 * Fills the views shared by item_event_row and item_event_featured, so the two
 * adapters show an event in exactly the same way.
 */
object EventCardBinder {
    fun bind(
        item: EventListItem,
        image: ImageView,
        title: TextView,
        dateTime: TextView,
        venue: TextView,
        price: TextView,
        saveButton: MaterialButton,
        mayClashLabel: TextView,
    ) {
        val context = title.context
        val event = item.event
        title.text = event.name
        dateTime.text = EventFormatter.dateTime(event.localDate, event.localTime) ?: context.getString(R.string.date_tbc)
        venue.text = venueText(context, item)
        price.text = priceText(context, event.minPrice, event.currency)
        image.load(event.imageUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_launcher_background)
            error(R.drawable.ic_launcher_background)
        }
        mayClashLabel.isVisible = item.clash != null
        saveButton.setIconResource(if (item.isSaved) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border)
        saveButton.contentDescription = context.getString(
            if (item.isSaved) R.string.action_unsave_event else R.string.action_save_event,
        )
    }

    fun venueText(context: Context, item: EventListItem): String {
        val venueName = item.event.venue?.name ?: return ""
        val miles = item.distanceMiles ?: return venueName
        return context.getString(R.string.venue_with_distance, venueName, EventFormatter.distance(miles))
    }

    fun priceText(context: Context, minPrice: Double?, currency: String?): String =
        when (val label = EventFormatter.price(minPrice, currency)) {
            PriceLabel.Free -> context.getString(R.string.price_free)
            PriceLabel.ToBeConfirmed -> context.getString(R.string.price_tbc)
            is PriceLabel.From -> context.getString(R.string.price_from, label.amount)
        }
}
