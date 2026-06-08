package com.heightmeter.app

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.heightmeter.app.databinding.ActivityGalleryBinding
import com.heightmeter.app.measurement.MeasurementViewModel
import com.heightmeter.app.measurement.data.Measurement

/**
 * Gallery activity showing saved measurements.
 */
class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var viewModel: MeasurementViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MeasurementViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        observeMeasurements()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.toolbar.title = getString(R.string.gallery_title)
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun observeMeasurements() {
        viewModel.allMeasurements.observe(this) { measurements ->
            if (measurements.isEmpty()) {
                binding.recyclerView.visibility = View.GONE
                binding.emptyView.visibility = View.VISIBLE
            } else {
                binding.recyclerView.visibility = View.VISIBLE
                binding.emptyView.visibility = View.GONE
                binding.recyclerView.adapter = MeasurementAdapter(measurements) { measurement ->
                    showDeleteDialog(measurement)
                }
            }
        }
    }

    private fun showDeleteDialog(measurement: Measurement) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_confirm))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteMeasurement(measurement)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}

/**
 * RecyclerView adapter for saved measurements.
 */
class MeasurementAdapter(
    private val measurements: List<Measurement>,
    private val onDeleteClick: (Measurement) -> Unit
) : RecyclerView.Adapter<MeasurementAdapter.MeasurementViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeasurementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_measurement, parent, false)
        return MeasurementViewHolder(view)
    }

    override fun onBindViewHolder(holder: MeasurementViewHolder, position: Int) {
        holder.bind(measurements[position], onDeleteClick)
    }

    override fun getItemCount() = measurements.size

    class MeasurementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnail: ImageView = itemView.findViewById(R.id.measurement_thumbnail)
        private val typeText: TextView = itemView.findViewById(R.id.measurement_type)
        private val valueText: TextView = itemView.findViewById(R.id.measurement_value)
        private val dateText: TextView = itemView.findViewById(R.id.measurement_date)
        private val deleteBtn: ImageView = itemView.findViewById(R.id.btn_delete)

        fun bind(measurement: Measurement, onDelete: (Measurement) -> Unit) {
            typeText.text = measurement.typeName.replace("_", " ")
            valueText.text = measurement.label.ifEmpty {
                formatMeasurementValue(measurement)
            }
            dateText.text = java.text.SimpleDateFormat(
                "MMM dd, yyyy HH:mm",
                java.util.Locale.getDefault()
            ).format(java.util.Date(measurement.timestamp))

            // Load thumbnail if exists
            if (measurement.screenshotPath != null) {
                try {
                    val bitmap = BitmapFactory.decodeFile(measurement.screenshotPath)
                    thumbnail.setImageBitmap(bitmap)
                    thumbnail.scaleType = ImageView.ScaleType.CENTER_CROP
                } catch (e: Exception) {
                    thumbnail.setImageResource(R.drawable.ic_line)
                    thumbnail.scaleType = ImageView.ScaleType.CENTER_INSIDE
                }
            } else {
                thumbnail.setImageResource(R.drawable.ic_line)
                thumbnail.scaleType = ImageView.ScaleType.CENTER_INSIDE
            }

            deleteBtn.setOnClickListener { onDelete(measurement) }
        }

        private fun formatMeasurementValue(measurement: Measurement): String {
            return when (measurement.unit) {
                "m" -> String.format("%.2f m", measurement.primaryValue)
                "ft" -> String.format("%.2f ft", measurement.primaryValue)
                "cm" -> String.format("%.1f cm", measurement.primaryValue)
                "in" -> String.format("%.1f in", measurement.primaryValue)
                else -> String.format("%.2f", measurement.primaryValue)
            }
        }
    }
}
