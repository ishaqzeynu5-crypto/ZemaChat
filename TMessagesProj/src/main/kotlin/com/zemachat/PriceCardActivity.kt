package com.zemachat

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class PriceCardActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("zema_price_card", Context.MODE_PRIVATE)
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
            text = "Price Card / \u12cb\u130b \u1218\u120b\u12aa\u12eb"
            setTextColor(Color.parseColor("#D4AF37"))
            textSize = 18f
        })
        root.addView(bar)

        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(20))
        }
        fun field(hintTxt: String, savedKey: String? = null): EditText {
            return EditText(this).apply {
                hint = hintTxt
                setHintTextColor(Color.parseColor("#888888"))
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#1E1E1E"))
                setPadding(dp(12), dp(12), dp(12), dp(12))
                if (savedKey != null) setText(prefs.getString(savedKey, ""))
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.bottomMargin = dp(12)
                layoutParams = lp
            }
        }
        val itemEt = field("Item Name / \u12e8\u12d5\u1243\u12cd \u1235\u121d")
        val priceEt = field("Price (ETB) / \u12cb\u130b (\u1265\u122d)")
        val addrEt = field("Delivery / Address / \u12a0\u12f5\u122b\u123b", "addr")
        val phoneEt = field("Phone / \u1235\u120d\u12ad", "phone")
        val noteEt = field("Note (optional) / \u1270\u1328\u121b\u122a \u121b\u1233\u1230\u1262\u12eb")

        fun buildMsg(): String {
            prefs.edit().putString("addr", addrEt.text.toString().trim()).putString("phone", phoneEt.text.toString().trim()).apply()
            val sb = StringBuilder()
            val it = itemEt.text.toString().trim()
            val pr = priceEt.text.toString().trim()
            val ad = addrEt.text.toString().trim()
            val ph = phoneEt.text.toString().trim()
            val nt = noteEt.text.toString().trim()
            if (it.isNotEmpty()) sb.append("\u2728 ").append(it).append("\n")
            if (pr.isNotEmpty()) sb.append("\u2022 \u12cb\u130b / Price: ").append(pr).append(" ETB\n")
            if (ad.isNotEmpty()) sb.append("\u2022 \u12a0\u12f5\u122b\u123b / Address: ").append(ad).append("\n")
            if (ph.isNotEmpty()) sb.append("\u260e \u1235\u120d\u12ad / Phone: ").append(ph).append("\n")
            if (nt.isNotEmpty()) sb.append("\u270e ").append(nt)
            return sb.toString().trim()
        }

        val copyBtn = Button(this).apply {
            text = "Copy / \u1245\u12f3"
            setBackgroundColor(Color.parseColor("#2A2A2A"))
            setTextColor(Color.parseColor("#D4AF37"))
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = dp(10)
            layoutParams = lp
            setOnClickListener {
                val msg = buildMsg()
                if (msg.isEmpty()) { Toast.makeText(this@PriceCardActivity, "Fill item or price first", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Zema Price Card", msg))
                Toast.makeText(this@PriceCardActivity, "Copied!", Toast.LENGTH_SHORT).show()
            }
        }
        val sendBtn = Button(this).apply {
            text = "Send to Chat / \u120b\u12ad"
            setBackgroundColor(Color.parseColor("#D4AF37"))
            setTextColor(Color.parseColor("#121212"))
            setOnClickListener {
                val msg = buildMsg()
                if (msg.isEmpty()) { Toast.makeText(this@PriceCardActivity, "Fill item or price first", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                val i = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, msg)
                    setPackage(packageName)
                }
                startActivity(Intent.createChooser(i, "Send Price Card"))
            }
        }

        form.addView(itemEt); form.addView(priceEt); form.addView(addrEt); form.addView(phoneEt); form.addView(noteEt)
        form.addView(copyBtn); form.addView(sendBtn)
        root.addView(ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f); addView(form) })
        setContentView(root)
    }
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
