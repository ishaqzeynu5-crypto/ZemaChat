package com.zemachat

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class QuickRepliesActivity : Activity() {
    private lateinit var listContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
        }
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        toolbar.addView(makeBtn("<") { finish() })
        toolbar.addView(TextView(this).apply {
            text = "Zema Quick Replies"
            setTextColor(Color.parseColor("#D4AF37"))
            textSize = 18f
            setPadding(dp(12), 0, dp(12), 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        toolbar.addView(makeBtn("+") { showEditDialog(-1) })
        root.addView(toolbar)
        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(16))
        }
        root.addView(ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            addView(listContainer)
        })
        setContentView(root)
        renderList()
    }

    private fun renderList() {
        listContainer.removeAllViews()
        val items = QuickRepliesHelper.loadTemplates(this)
        if (items.isEmpty()) {
            listContainer.addView(TextView(this).apply {
                text = "Tap + above to add a quick reply."
                setTextColor(Color.parseColor("#AAAAAA"))
                textSize = 14f
                setPadding(dp(8), dp(24), dp(8), dp(24))
            })
            return
        }
        items.forEachIndexed { idx, item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(Color.parseColor("#1E1E1E"))
                setPadding(dp(12), dp(10), dp(12), dp(10))
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.bottomMargin = dp(8)
                layoutParams = lp
                setOnClickListener { showEditDialog(idx) }
            }
            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textCol.addView(TextView(this).apply {
                text = item.title
                setTextColor(Color.parseColor("#D4AF37"))
                textSize = 15f
            })
            textCol.addView(TextView(this).apply {
                text = item.text
                setTextColor(Color.WHITE)
                textSize = 13f
            })
            val delBtn = makeBtn("X") {
                val updated = QuickRepliesHelper.loadTemplates(this).toMutableList()
                if (idx in updated.indices) {
                    updated.removeAt(idx)
                    QuickRepliesHelper.saveTemplates(this, updated)
                    renderList()
                }
            }
            row.addView(textCol)
            row.addView(delBtn)
            listContainer.addView(row)
        }
    }

    private fun showEditDialog(index: Int) {
        val items = QuickRepliesHelper.loadTemplates(this).toMutableList()
        val existing = if (index in items.indices) items[index] else null
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        val titleEt = EditText(this).apply { hint = "Title (e.g. Price)"; setText(existing?.title ?: "") }
        val bodyEt = EditText(this).apply { hint = "Message text"; minLines = 3; setText(existing?.text ?: "") }
        box.addView(titleEt)
        box.addView(bodyEt)
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(if (existing == null) "Add Quick Reply" else "Edit Quick Reply")
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                val t = titleEt.text.toString().trim()
                val b = bodyEt.text.toString().trim()
                if (b.isBlank()) {
                    Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val entry = QuickRepliesHelper.Template(t.ifBlank { b.take(20) }, b)
                if (index in items.indices) items[index] = entry else items.add(entry)
                QuickRepliesHelper.saveTemplates(this, items)
                renderList()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun makeBtn(label: String, onClick: () -> Unit) = TextView(this).apply {
        text = label
        setTextColor(Color.parseColor("#D4AF37"))
        textSize = 20f
        setPadding(dp(10), dp(6), dp(10), dp(6))
        setOnClickListener { onClick() }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
