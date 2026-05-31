package com.heightmeter.app.ui.history

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.heightmeter.app.R
import com.heightmeter.app.data.model.Measurement
import com.heightmeter.app.databinding.FragmentHistoryBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Displays the history of all saved height measurements.
 */
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HistoryViewModel
    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[HistoryViewModel::class.java]

        setupRecyclerView()
        observeData()
        setupSwipeToDelete()
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter(
            context = requireContext(),
            onDelete = { measurement -> showDeleteConfirmation(measurement) },
            onShare = { measurement -> shareMeasurement(measurement) }
        )
        binding.historyRecycler.adapter = adapter
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.measurements.collectLatest { list ->
                    adapter.submitList(list)
                    binding.emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun setupSwipeToDelete() {
        val callback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val measurement = adapter.currentList[position]
                showDeleteConfirmation(measurement)
                adapter.notifyItemChanged(position) // Restore the item
            }
        }
        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.historyRecycler)
    }

    private fun showDeleteConfirmation(measurement: Measurement) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Measurement")
            .setMessage("Delete measurement for ${measurement.personName}?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteMeasurement(measurement)
                Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun shareMeasurement(measurement: Measurement) {
        val text = "Height: ${"%.1f".format(measurement.heightCm)} cm " +
                "(${measurement.toFeetInches()}) - " +
                "${measurement.personName} - ${measurement.accuracyLabel}"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "Height Measurement")
        }
        startActivity(Intent.createChooser(shareIntent, "Share Measurement"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
