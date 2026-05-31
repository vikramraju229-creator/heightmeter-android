package com.heightmeter.app.ui.history

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.heightmeter.app.R
import com.heightmeter.app.data.model.Measurement
import com.heightmeter.app.databinding.ItemHistoryBinding

/**
 * RecyclerView adapter for the measurement history list.
 * Supports item click for delete and share actions.
 */
class HistoryAdapter(
    private val context: Context,
    private val onDelete: (Measurement) -> Unit,
    private val onShare: (Measurement) -> Unit
) : ListAdapter<Measurement, HistoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(measurement: Measurement) {
            binding.itemName.text = measurement.personName
            binding.itemHeight.text = String.format("%.1f cm", measurement.heightCm)
            binding.itemDate.text = measurement.formattedDate()

            binding.itemAccuracy.text = measurement.accuracyLabel
            val accuracyColor = when (measurement.accuracyLabel) {
                "Excellent" -> R.color.accuracy_excellent
                "Good" -> R.color.accuracy_good
                else -> R.color.accuracy_fair
            }
            binding.itemAccuracy.setTextColor(
                ContextCompat.getColor(context, accuracyColor)
            )

            // Long press to delete
            binding.root.setOnLongClickListener {
                onDelete(measurement)
                true
            }

            // Click to share
            binding.root.setOnClickListener {
                onShare(measurement)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Measurement>() {
        override fun areItemsTheSame(oldItem: Measurement, newItem: Measurement): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Measurement, newItem: Measurement): Boolean =
            oldItem == newItem
    }
}
