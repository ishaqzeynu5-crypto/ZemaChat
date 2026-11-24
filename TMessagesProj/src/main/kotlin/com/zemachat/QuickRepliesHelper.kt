package com.zemachat

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject

object QuickRepliesHelper {
    private const val PREFS_NAME = "zema_quick_replies_prefs"
    private const val KEY_TEMPLATES = "templates_json"

    data class Template(val title: String, val text: String)

    interface OnReplySelected {
        fun onSelect(text: String)
    }

    fun loadTemplates(context: Context): MutableList<Template> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_TEMPLATES, null)
        val list = mutableListOf<Template>()
        if (raw == null) {
            list.add(Template("Price", "Price: ___ ETB"))
            list.add(Template("Address", "Address: Addis Ababa"))
            list.add(Template("Phone", "Phone: +251 9"))
            saveTemplates(context, list)
            return list
        }
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val t = o.optString("title", "")
                val b = o.optString("text", "")
                if (b.isNotBlank()) list.add(Template(t.ifBlank { b.take(20) }, b))
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveTemplates(context: Context, list: List<Template>) {
        val arr = JSONArray()
        for (item in list) {
            val o = JSONObject()
            o.put("title", item.title)
            o.put("text", item.text)
            arr.put(o)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_TEMPLATES, arr.toString()).apply()
    }

    @JvmStatic
    fun showPickerDialog(context: Context, listener: OnReplySelected) {
        val list = loadTemplates(context)
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        val dialogHolder = arrayOfNulls<AlertDialog>(1)
        if (list.isEmpty()) {
            val empty = TextView(context).apply {
                text = "No quick replies saved yet."
                setTextColor(Color.parseColor("#AAAAAA"))
                textSize = 14f
                setPadding(dp(8), dp(16), dp(8), dp(16))
            }
            container.addView(empty)
        } else {
            for (item in list) {
                val card = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(Color.parseColor("#1E1E1E"))
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.bottomMargin = dp(8)
                    layoutParams = lp
                    setOnClickListener {
                        dialogHolder[0]?.dismiss()
                        listener.onSelect(item.text)
                    }
                }
                card.addView(TextView(context).apply {
                    text = item.title
                    setTextColor(Color.parseColor("#D4AF37"))
                    textSize = 15f
                })
                card.addView(TextView(context).apply {
                    text = item.text
                    setTextColor(Color.WHITE)
                    textSize = 13f
                    maxLines = 2
                })
                container.addView(card)
            }
        }
        val scroll = ScrollView(context).apply { addView(container) }
        val dlg = AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Zema Quick Replies")
            .setView(scroll)
            .setNeutralButton("Manage") { _, _ ->
                val intent = Intent(context, QuickRepliesActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            .setNegativeButton("Close", null)
            .create()
        dialogHolder[0] = dlg
        dlg.show()
    }
}
