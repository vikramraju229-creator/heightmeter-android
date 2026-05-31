package com.heightmeter.app.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.common.util.concurrent.ListenableFuture
import com.heightmeter.app.R
import com.heightmeter.app.camera.PoseAnalyzer
import com.heightmeter.app.databinding.FragmentCameraBinding
import java.util.concurrent.Executors

/**
 * Camera screen that shows a live preview with ML Kit pose detection overlay.
 */
class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CameraViewModel
    private lateinit var poseAnalyzer: PoseAnalyzer

    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    /** Camera permission launcher. */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            // Permission denied — show a message or close
            requireActivity().finish()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[CameraViewModel::class.java]
        poseAnalyzer = PoseAnalyzer()

        setupViews()
        observeViewModel()
        checkCameraPermission()
    }

    private fun setupViews() {
        // Capture button
        binding.captureButton.setOnClickListener {
            viewModel.calculateHeight()
        }

        // History button
        binding.historyButton.setOnClickListener {
            findNavController().navigate(R.id.history_fragment)
        }

        // Guide visibility
        binding.guideOverlay.visibility = View.VISIBLE
        binding.guideText.visibility = View.VISIBLE
    }

    private fun observeViewModel() {
        // Confidence text
        viewModel.headConfidence.observe(viewLifecycleOwner) { conf ->
            val headPct = (conf * 100).toInt()
            val feetPct = (viewModel.ankleConfidence.value ?: 0f * 100).toInt()
            binding.confidenceText.text = "Head: ${headPct}%  Feet: ${feetPct}%"
        }

        // Head / feet status
        viewModel.headDetected.observe(viewLifecycleOwner) { detected ->
            binding.headStatus.text = if (detected) "Head detected ✓" else "Head: waiting..."
            binding.headStatus.setTextColor(
                if (detected) ContextCompat.getColor(requireContext(), R.color.landmark_detected)
                else ContextCompat.getColor(requireContext(), R.color.landmark_missing)
            )
        }

        viewModel.feetDetected.observe(viewLifecycleOwner) { detected ->
            binding.feetStatus.text = if (detected) "Feet detected ✓" else "Feet: waiting..."
            binding.feetStatus.setTextColor(
                if (detected) ContextCompat.getColor(requireContext(), R.color.landmark_detected)
                else ContextCompat.getColor(requireContext(), R.color.landmark_missing)
            )
        }

        // Capture enabled
        viewModel.captureEnabled.observe(viewLifecycleOwner) { enabled ->
            binding.captureButton.isEnabled = enabled
        }

        // Guide overlay
        viewModel.showGuide.observe(viewLifecycleOwner) { show ->
            binding.guideOverlay.visibility = if (show) View.VISIBLE else View.GONE
            binding.guideText.visibility = if (show) View.VISIBLE else View.GONE
        }

        // Pose overlay updates
        viewModel.poseResult.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                binding.poseOverlay.pose = result.pose
                binding.poseOverlay.headDetected = viewModel.headDetected.value ?: false
                binding.poseOverlay.feetDetected = viewModel.feetDetected.value ?: false
                binding.poseOverlay.imageWidth = result.imageWidth
                binding.poseOverlay.imageHeight = result.imageHeight
                binding.poseOverlay.invalidate()
            }
        }

        // Height calculated — navigate to result
        viewModel.calculatedResult.observe(viewLifecycleOwner) { heightResult ->
            if (heightResult != null) {
                val args = Bundle().apply {
                    putFloat("heightCm", heightResult.heightCm)
                    putString("accuracy", heightResult.accuracyLabel)
                }
                findNavController().navigate(R.id.action_camera_to_result, args)
                // Reset so repeated captures work
                viewModel.resetCalculatedResult()
            }
        }
    }

    // --- Camera setup ---

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Optionally show rationale UI
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    @Suppress("DEPRECATION") // CameraX 1.3.x still uses ListenableFuture
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        this.cameraProviderFuture = cameraProviderFuture

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }

            // Select back camera
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            // Get camera ID for FOV
            val cameraId = getBackCameraId()
            viewModel.currentCameraId = cameraId

            // Image analysis with ML Kit
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            // Wire up the pose analyzer
            poseAnalyzer.onPoseResult = { result ->
                viewModel.onPoseAnalyzed(result)
            }
            imageAnalysis.setAnalyzer(analysisExecutor, poseAnalyzer.getImageAnalyzer())

            // Bind to lifecycle
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                binding.confidenceText.text = "Camera error: ${e.localizedMessage}"
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /**
     * Returns the ID of the back-facing camera, defaulting to "0".
     */
    private fun getBackCameraId(): String {
        return try {
            val cameraManager = requireContext().getSystemService(android.content.Context.CAMERA_SERVICE)
                    as android.hardware.camera2.CameraManager
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(
                    android.hardware.camera2.CameraCharacteristics.LENS_FACING
                )
                if (facing == android.hardware.camera2.CameraMetadata.LENS_FACING_BACK) {
                    return id
                }
            }
            "0"
        } catch (e: Exception) {
            "0"
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.tiltSensor.start()
    }

    override fun onPause() {
        super.onPause()
        viewModel.tiltSensor.stop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraProviderFuture?.let { future ->
            future.get()?.unbindAll()
        }
        poseAnalyzer.close()
        analysisExecutor.shutdown()
        _binding = null
    }
}
