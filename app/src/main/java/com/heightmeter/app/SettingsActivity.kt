package com.heightmeter.app

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.heightmeter.app.databinding.ActivitySettingsBinding

/**
 * Settings activity for the AR Measurement app.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupUnitSelection()
        setupSnapToSurface()
        setupGridOverlay()
        setupLineThickness()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.title = getString(R.string.settings_title)
    }

    private fun setupUnitSelection() {
        val units = arrayOf("Meters (m)", "Feet (ft)", "Centimeters (cm)", "Inches (in)")
        val unitValues = arrayOf("m", "ft", "cm", "in")

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val currentUnit = prefs.getString("unit", "m")

        val selectedIndex = unitValues.indexOf(currentUnit).coerceAtLeast(0)

        val spinner = binding.unitSpinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, units)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(selectedIndex)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putString("unit", unitValues[position]).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupSnapToSurface() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        binding.snapSwitch.isChecked = prefs.getBoolean("snap_to_surface", true)
        binding.snapSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("snap_to_surface", isChecked).apply()
        }
    }

    private fun setupGridOverlay() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        binding.gridSwitch.isChecked = prefs.getBoolean("grid_overlay", false)
        binding.gridSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("grid_overlay", isChecked).apply()
        }
    }

    private fun setupLineThickness() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val thickness = prefs.getFloat("line_thickness", 1.0f)
        binding.thicknessSlider.value = thickness
        binding.thicknessValue.text = String.format("%.1f", thickness)

        binding.thicknessSlider.addOnChangeListener { _, value, _ ->
            binding.thicknessValue.text = String.format("%.1f", value)
            prefs.edit().putFloat("line_thickness", value).apply()
        }
    }
}
