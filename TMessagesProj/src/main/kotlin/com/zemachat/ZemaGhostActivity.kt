package com.zemachat

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

class ZemaGhostActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
        }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        bar.addView(TextView(this).apply {
            text = "<"
            setTextColor(Color.parseColor("#D4AF37"))
            textSize = 22f
            setPadding(dp(10), dp(4), dp(14), dp(4))
            setOnClickListener { finish() }
        })
        bar.addView(TextView(this).apply {
            text = "Zema Ghost Mode"
            setTextColor(Color.parseColor("#D4AF37"))
            textSize = 18f
        })
        root.addView(bar)

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(16))
        }
        fun addToggle(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(Color.parseColor("#1E1E1E"))
                setPadding(dp(14), dp(12), dp(14), dp(12))
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.bottomMargin = dp(10)
                layoutParams = lp
            }
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            col.addView(TextView(this).apply { text = title; setTextColor(Color.parseColor("#D4AF37")); textSize = 15f })
            col.addView(TextView(this).apply { text = desc; setTextColor(Color.parseColor("#BBBBBB")); textSize = 12f })
            val sw = Switch(this).apply {
                isChecked = checked
                setOnCheckedChangeListener { _, isCh -> onChange(isCh) }
            }
            row.addView(col); row.addView(sw)
            body.addView(row)
        }

        addToggle("Hide Online Status", "Stay offline even while using Zema Chat", ZemaGhostHelper.isHideOnline(this)) { ZemaGhostHelper.setHideOnline(this, it) }
        addToggle("Don't Send Read Receipts", "Read messages without sending blue double-check", ZemaGhostHelper.isHideRead(this)) { ZemaGhostHelper.setHideRead(this, it) }
        addToggle("Hide Typing Status", "Hide 'typing...' and 'recording...' indicators", ZemaGhostHelper.isHideTyping(this)) { ZemaGhostHelper.setHideTyping(this, it) }

        root.addView(body)
        setContentView(root)
    }
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
