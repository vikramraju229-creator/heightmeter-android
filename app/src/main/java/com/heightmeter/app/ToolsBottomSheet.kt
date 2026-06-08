package com.heightmeter.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.heightmeter.app.databinding.ToolsBottomSheetBinding
import com.heightmeter.app.measurement.BaseTool
import com.heightmeter.app.measurement.MeasurementViewModel

/**
 * Bottom sheet dialog that displays the grid of measurement tools.
 */
class ToolsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: ToolsBottomSheetBinding? = null
    private val binding get() = _binding!!

    var onToolSelected: ((BaseTool) -> Unit)? = null

    private lateinit var toolItems: List<ToolItem>
    private lateinit var tools: List<BaseTool>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Get tools from ViewModel
        val activity = requireActivity()
        val viewModel = ViewModelProvider(activity)[MeasurementViewModel::class.java]
        tools = viewModel.availableTools
        toolItems = createToolItems()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ToolsBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolGrid()
    }

    private fun createToolItems(): List<ToolItem> {
        return tools.mapIndexed { index, tool ->
            // Use the tool's own display name and icon; replace spaces with
            // newlines to keep the 3-column grid compact
            val label = tool.displayName.replace(" ", "\n")
            ToolItem(label, tool.iconResId, index)
        }
    }

    private fun setupToolGrid() {
        binding.toolsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.toolsRecyclerView.adapter = ToolAdapter(toolItems) { toolItem ->
            if (toolItem.toolIndex in tools.indices) {
                val tool = tools[toolItem.toolIndex]
                onToolSelected?.invoke(tool)
            }
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class ToolItem(
        val label: String,
        val iconResId: Int,
        val toolIndex: Int
    )
}

/**
 * RecyclerView adapter for the tools grid.
 */
class ToolAdapter(
    private val tools: List<ToolsBottomSheet.ToolItem>,
    private val onItemClick: (ToolsBottomSheet.ToolItem) -> Unit
) : RecyclerView.Adapter<ToolAdapter.ToolViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tool, parent, false)
        return ToolViewHolder(view)
    }

    override fun onBindViewHolder(holder: ToolViewHolder, position: Int) {
        val tool = tools[position]
        holder.bind(tool)
        holder.itemView.setOnClickListener { onItemClick(tool) }
    }

    override fun getItemCount() = tools.size

    class ToolViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.tool_icon)
        private val label: TextView = itemView.findViewById(R.id.tool_label)

        fun bind(tool: ToolsBottomSheet.ToolItem) {
            icon.setImageResource(tool.iconResId)
            label.text = tool.label
        }
    }
}
