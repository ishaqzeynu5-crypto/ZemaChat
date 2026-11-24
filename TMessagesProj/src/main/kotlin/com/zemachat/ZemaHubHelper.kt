package com.zemachat

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.LinearLayout
import android.widget.TextView

object ZemaHubHelper {
    @JvmStatic
    fun showMenu(context: Context) {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        val dlgHolder = arrayOfNulls<AlertDialog>(1)
        fun addOption(title: String, sub: String, onClick: () -> Unit) {
            val card = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#1E1E1E"))
                setPadding(dp(14), dp(12), dp(14), dp(12))
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.bottomMargin = dp(10)
                layoutParams = lp
                setOnClickListener {
                    dlgHolder[0]?.dismiss()
                    onClick()
                }
            }
            card.addView(TextView(context).apply { text = title; setTextColor(Color.parseColor("#D4AF37")); textSize = 16f })
            card.addView(TextView(context).apply { text = sub; setTextColor(Color.parseColor("#CCCCCC")); textSize = 12f })
            box.addView(card)
        }
        addOption("Zema Social", "Open TikTok, YouTube, WhatsApp Web & shortcuts") {
            SocialBrowserActivity.open(context, null)
        }
        addOption("Photo Watermark", "Set text or logo watermark for sent photos") {
            val i = Intent(context, WatermarkSettingsActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
        addOption("Quick Replies", "Manage saved reply templates") {
            val i = Intent(context, QuickRepliesActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
        val dlg = AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Zema Tools")
            .setView(box)
            .setNegativeButton("Close", null)
            .create()
        dlgHolder[0] = dlg
        dlg.show()
    }
}
