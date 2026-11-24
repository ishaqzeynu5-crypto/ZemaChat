package com.zemachat

import android.app.Activity
import android.app.AlertDialog
import android.content.Context

object WatchHereHelper {
    @JvmStatic
    fun isWatchableVideoLink(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val lower = url.lowercase()
        return lower.contains("tiktok.com") ||
            lower.contains("youtube.com") ||
            lower.contains("youtu.be") ||
            lower.contains("instagram.com")
    }

    @JvmStatic
    fun showWatchHereDialog(context: Context, url: String, onOpenNormal: Runnable): Boolean {
        if (context !is Activity || context.isFinishing || context.isDestroyed) {
            return false
        }
        AlertDialog.Builder(context)
            .setTitle("Watch Video")
            .setMessage(url)
            .setPositiveButton("Watch here") { _, _ ->
                SocialBrowserActivity.open(context, url)
            }
            .setNegativeButton("Open normally") { _, _ ->
                onOpenNormal.run()
            }
            .show()
        return true
    }
}
