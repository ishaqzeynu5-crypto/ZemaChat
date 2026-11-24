package com.zemachat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.io.File

object WatermarkHelper {
    private const val PREFS = "zema_watermark_prefs"
    private const val KEY_ENABLED = "wm_enabled"
    private const val KEY_TEXT = "wm_text"
    private const val KEY_POSITION = "wm_position" // 0=BR, 1=BL, 2=TR, 3=TL, 4=Center
    private const val KEY_SIZE = "wm_size"         // 10..100
    private const val KEY_OPACITY = "wm_opacity"   // 10..100
    const val LOGO_FILENAME = "zema_watermark_logo.png"

    @JvmStatic
    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun getText(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TEXT, "") ?: ""

    fun getPosition(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_POSITION, 0)

    fun getSize(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_SIZE, 50)

    fun getOpacity(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_OPACITY, 70)

    fun getLogoFile(context: Context): File = File(context.filesDir, LOGO_FILENAME)

    fun saveSettings(
        context: Context,
        enabled: Boolean,
        text: String,
        position: Int,
        size: Int,
        opacity: Int
    ) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putString(KEY_TEXT, text.trim())
            .putInt(KEY_POSITION, position.coerceIn(0, 4))
            .putInt(KEY_SIZE, size.coerceIn(10, 100))
            .putInt(KEY_OPACITY, opacity.coerceIn(10, 100))
            .apply()
    }

    @JvmStatic
    fun applyWatermarkIfEnabled(context: Context?, src: Bitmap?): Bitmap? {
        if (context == null || src == null || !isEnabled(context)) return src
        val text = getText(context)
        val logoFile = getLogoFile(context)
        val hasLogo = logoFile.exists() && logoFile.length() > 0
        if (text.isBlank() && !hasLogo) return src

        val out = try {
            src.copy(Bitmap.Config.ARGB_8888, true) ?: return src
        } catch (_: Throwable) {
            return src
        }

        val canvas = Canvas(out)
        val w = out.width.toFloat()
        val h = out.height.toFloat()
        val pad = w * 0.04f
        val pos = getPosition(context)
        val sizeRatio = getSize(context) / 100f
        val alphaVal = (getOpacity(context).coerceIn(10, 100) * 255) / 100

        var logoHeightUsed = 0f
        if (hasLogo) {
            try {
                val rawLogo = BitmapFactory.decodeFile(logoFile.absolutePath)
                if (rawLogo != null && rawLogo.width > 0 && rawLogo.height > 0) {
                    val targetW = (w * (0.08f + 0.25f * sizeRatio)).toInt().coerceAtLeast(24)
                    val targetH = ((rawLogo.height.toFloat() / rawLogo.width.toFloat()) * targetW)
                        .toInt().coerceAtLeast(24)
                    val scaledLogo = Bitmap.createScaledBitmap(rawLogo, targetW, targetH, true)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                        alpha = alphaVal
                    }
                    val lx = when (pos) {
                        1, 3 -> pad
                        4 -> (w - targetW) / 2f
                        else -> w - targetW - pad
                    }
                    val ly = when (pos) {
                        2, 3 -> pad
                        4 -> (h - targetH) / 2f
                        else -> h - targetH - pad
                    }
                    canvas.drawBitmap(scaledLogo, lx, ly, paint)
                    logoHeightUsed = targetH + (w * 0.015f)
                    if (scaledLogo != rawLogo) scaledLogo.recycle()
                    rawLogo.recycle()
                }
            } catch (_: Throwable) {
            }
        }

        if (text.isNotBlank()) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                alpha = alphaVal
                textSize = (w * (0.025f + 0.05f * sizeRatio)).coerceAtLeast(18f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setShadowLayer(4f, 2f, 2f, Color.argb(alphaVal, 0, 0, 0))
            }
            val textW = textPaint.measureText(text)
            val fm = textPaint.fontMetrics
            val textH = fm.descent - fm.ascent

            val tx = when (pos) {
                1, 3 -> pad
                4 -> (w - textW) / 2f
                else -> w - textW - pad
            }
            val ty = when (pos) {
                2, 3 -> pad + logoHeightUsed - fm.ascent
                4 -> (h / 2f) + (if (hasLogo) (logoHeightUsed / 2f + textH * 0.5f) else (textH / 2f - fm.descent))
                else -> h - pad - logoHeightUsed - fm.descent
            }
            canvas.drawText(text, tx, ty, textPaint)
        }

        return out
    }
}
