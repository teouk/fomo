package io.github.teouk.fomo.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.teouk.fomo.databinding.ItemEventFeaturedBinding
import io.github.teouk.fomo.databinding.ItemEventRowBinding
import io.github.teouk.fomo.model.Event

/**
 * DiffUtil compares the old and new list and updates only the rows that changed,
 * instead of redrawing everything as notifyDataSetChanged() would.
 */
object EventDiff : DiffUtil.ItemCallback<EventListItem>() {
    override fun areItemsTheSame(oldItem: EventListItem, newItem: EventListItem) = oldItem.event.id == newItem.event.id

    override fun areContentsTheSame(oldItem: EventListItem, newItem: EventListItem) = oldItem == newItem
}

/** Compact rows (item_event_row), used for "Near you", search results and My Plans. */
class EventRowAdapter(
    private val onEventClick: (Event) -> Unit,
    private val onSaveClick: (Event) -> Unit,
) : ListAdapter<EventListItem, EventRowAdapter.ViewHolder>(EventDiff) {

    class ViewHolder(val binding: ItemEventRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemEventRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            EventCardBinder.bind(
                item, imageEvent, textEventTitle, textEventDateTime, textEventVenue, textEventPrice, buttonSaveEvent, textEventBusy,
            )
            root.setOnClickListener { onEventClick(item.event) }
            buttonSaveEvent.setOnClickListener { onSaveClick(item.event) }
        }
    }
}

/** Large 16:9 cards (item_event_featured), used for "This weekend". */
class FeaturedEventAdapter(
    private val onEventClick: (Event) -> Unit,
    private val onSaveClick: (Event) -> Unit,
) : ListAdapter<EventListItem, FeaturedEventAdapter.ViewHolder>(EventDiff) {

    class ViewHolder(val binding: ItemEventFeaturedBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemEventFeaturedBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            EventCardBinder.bind(
                item, imageEvent, textEventTitle, textEventDateTime, textEventVenue, textEventPrice, buttonSaveEvent, textEventBusy,
            )
            root.setOnClickListener { onEventClick(item.event) }
            buttonSaveEvent.setOnClickListener { onSaveClick(item.event) }
        }
    }
}
