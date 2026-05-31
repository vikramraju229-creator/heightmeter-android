package com.heightmeter.app.ui.result

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.heightmeter.app.R
import com.heightmeter.app.databinding.FragmentResultBinding
import com.google.android.material.snackbar.Snackbar

/**
 * Displays the calculated height result and allows saving with a name.
 */
class ResultFragment : Fragment() {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ResultViewModel
    private val args: ResultFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[ResultViewModel::class.java]

        displayResult()
        setupButtons()
        observeSave()
    }

    private fun displayResult() {
        val heightCm = args.heightCm
        val accuracy = args.accuracy

        // Height in cm
        binding.heightCmText.text = String.format("%.1f cm", heightCm)

        // Height in feet/inches
        val totalInches = (heightCm / 2.54).toInt()
        val feet = totalInches / 12
        val inches = totalInches % 12
        binding.heightFtText.text = String.format("%d ft %d in", feet, inches)

        // Accuracy
        binding.accuracyText.text = accuracy
        val accuracyColor = when (accuracy) {
            "Excellent" -> R.color.accuracy_excellent
            "Good" -> R.color.accuracy_good
            else -> R.color.accuracy_fair
        }
        binding.accuracyText.setTextColor(
            ContextCompat.getColor(requireContext(), accuracyColor)
        )
    }

    private fun setupButtons() {
        binding.saveButton.setOnClickListener {
            val name = binding.nameInput.text?.toString()?.trim()
            if (name.isNullOrBlank()) {
                binding.nameInputLayout.error = "Please enter a name"
                return@setOnClickListener
            }
            binding.nameInputLayout.error = null
            viewModel.saveMeasurement(name, args.heightCm, args.accuracy)
        }

        binding.remeasureButton.setOnClickListener {
            findNavController().navigate(R.id.action_result_to_camera)
        }
    }

    private fun observeSave() {
        viewModel.saved.observe(viewLifecycleOwner) { saved ->
            if (saved) {
                Toast.makeText(requireContext(), "Measurement saved!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_result_to_camera)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
