package com.zemachat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.io.FileOutputStream

class WatermarkSettingsActivity : Activity() {

    companion object {
        private const val REQ_PICK_LOGO = 4012

        fun open(context: Context) {
            val intent = Intent(context, WatermarkSettingsActivity::class.java)
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private lateinit var logoStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        val title = TextView(this).apply {
            text = "Photo Watermark Settings"
            setTextColor(Color.parseColor("#D4AF37"))
            textSize = 20f
            setPadding(0, 0, 0, dp(14))
        }
        root.addView(title)

        // ON / OFF Switch
        val enableSwitch = Switch(this).apply {
            text = "Enable watermark on sent photos"
            setTextColor(Color.WHITE)
            textSize = 15f
            isChecked = WatermarkHelper.isEnabled(this@WatermarkSettingsActivity)
            setPadding(0, dp(6), 0, dp(14))
        }
        root.addView(enableSwitch)

        // Text input
        root.addView(makeLabel("Watermark Text (Name / Phone)"))
        val textInput = EditText(this).apply {
            hint = "e.g. Zema Shop - 0911..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#222222"))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setText(WatermarkHelper.getText(this@WatermarkSettingsActivity))
        }
        root.addView(textInput)

        // Logo picker
        root.addView(makeLabel("Logo Image (Optional)"))
        logoStatus = TextView(this).apply {
            setTextColor(Color.LTGRAY)
            textSize = 13f
            setPadding(0, 0, 0, dp(6))
        }
        updateLogoStatus()
        root.addView(logoStatus)

        val logoRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val pickLogoBtn = Button(this).apply {
            text = "Pick Logo"
            setOnClickListener { pickLogoImage() }
        }
        val clearLogoBtn = Button(this).apply {
            text = "Remove Logo"
            setOnClickListener {
                val f = WatermarkHelper.getLogoFile(this@WatermarkSettingsActivity)
                if (f.exists()) f.delete()
                updateLogoStatus()
                Toast.makeText(this@WatermarkSettingsActivity, "Logo removed", Toast.LENGTH_SHORT).show()
            }
        }
        logoRow.addView(pickLogoBtn)
        logoRow.addView(clearLogoBtn)
        root.addView(logoRow)

        // Position selector
        root.addView(makeLabel("Position"))
        val positions = arrayOf("Bottom-Right", "Bottom-Left", "Top-Right", "Top-Left", "Center")
        val posSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@WatermarkSettingsActivity,
                android.R.layout.simple_spinner_dropdown_item,
                positions
            )
            setSelection(WatermarkHelper.getPosition(this@WatermarkSettingsActivity))
            setBackgroundColor(Color.parseColor("#2A2A2A"))
        }
        root.addView(posSpinner)

        // Size slider
        val sizeVal = WatermarkHelper.getSize(this)
        val sizeLabel = makeLabel("Size: $sizeVal%")
        root.addView(sizeLabel)
        val sizeSeek = SeekBar(this).apply {
            max = 90
            progress = (sizeVal - 10).coerceIn(0, 90)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    sizeLabel.text = "Size: ${p + 10}%"
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        root.addView(sizeSeek)

        // Opacity slider
        val opacityVal = WatermarkHelper.getOpacity(this)
        val opacityLabel = makeLabel("Opacity: $opacityVal%")
        root.addView(opacityLabel)
        val opacitySeek = SeekBar(this).apply {
            max = 90
            progress = (opacityVal - 10).coerceIn(0, 90)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    opacityLabel.text = "Opacity: ${p + 10}%"
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        root.addView(opacitySeek)

        // Save button
        val saveBtn = Button(this).apply {
            text = "Save Settings"
            setBackgroundColor(Color.parseColor("#D4AF37"))
            setTextColor(Color.parseColor("#121212"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(20) }
            setOnClickListener {
                WatermarkHelper.saveSettings(
                    context = this@WatermarkSettingsActivity,
                    enabled = enableSwitch.isChecked,
                    text = textInput.text.toString(),
                    position = posSpinner.selectedItemPosition,
                    size = sizeSeek.progress + 10,
                    opacity = opacitySeek.progress + 10
                )
                Toast.makeText(this@WatermarkSettingsActivity, "Watermark saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        root.addView(saveBtn)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#121212"))
            addView(root)
        }
        setContentView(scroll)
    }

    @Suppress("DEPRECATION")
    private fun pickLogoImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, REQ_PICK_LOGO)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PICK_LOGO && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                val logoFile = WatermarkHelper.getLogoFile(this)
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(logoFile).use { output ->
                        input.copyTo(output)
                    }
                }
                updateLogoStatus()
                Toast.makeText(this, "Logo saved!", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(this, "Failed to load logo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateLogoStatus() {
        val f = WatermarkHelper.getLogoFile(this)
        logoStatus.text = if (f.exists() && f.length() > 0) "Logo: Selected ✓" else "Logo: None"
    }

    private fun makeLabel(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(Color.parseColor("#D4AF37"))
            textSize = 14f
            setPadding(0, dp(14), 0, dp(6))
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
