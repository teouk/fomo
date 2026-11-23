package io.github.teouk.fomo.ui.plans

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.teouk.fomo.R
import io.github.teouk.fomo.databinding.ItemDayHeaderBinding
import io.github.teouk.fomo.databinding.ItemEventRowBinding
import io.github.teouk.fomo.model.Event
import io.github.teouk.fomo.ui.common.EventCardBinder
import io.github.teouk.fomo.ui.common.EventFormatter

/** Shows day headings and event rows in one list, using two view types. */
class PlansAdapter(
    private val onEventClick: (Event) -> Unit,
    private val onSaveClick: (Event) -> Unit,
) : ListAdapter<PlansRow, RecyclerView.ViewHolder>(Diff) {

    private class HeaderHolder(val binding: ItemDayHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    private class EventHolder(val binding: ItemEventRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is PlansRow.DayHeader -> TYPE_HEADER
        is PlansRow.EventRow -> TYPE_EVENT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderHolder(ItemDayHeaderBinding.inflate(inflater, parent, false))
        } else {
            EventHolder(ItemEventRowBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is PlansRow.DayHeader -> {
                val text = (holder as HeaderHolder).binding.textDayHeader
                text.text = row.date?.let(EventFormatter::longDay) ?: text.context.getString(R.string.date_unknown)
            }
            is PlansRow.EventRow -> with((holder as EventHolder).binding) {
                EventCardBinder.bind(
                    row.item, imageEvent, textEventTitle, textEventDateTime, textEventVenue, textEventPrice, buttonSaveEvent,
                )
                root.setOnClickListener { onEventClick(row.item.event) }
                buttonSaveEvent.setOnClickListener { onSaveClick(row.item.event) }
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<PlansRow>() {
        override fun areItemsTheSame(oldItem: PlansRow, newItem: PlansRow) = when {
            oldItem is PlansRow.DayHeader && newItem is PlansRow.DayHeader -> oldItem.date == newItem.date
            oldItem is PlansRow.EventRow && newItem is PlansRow.EventRow -> oldItem.item.event.id == newItem.item.event.id
            else -> false
        }

        override fun areContentsTheSame(oldItem: PlansRow, newItem: PlansRow) = oldItem == newItem
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_EVENT = 1
    }
}
