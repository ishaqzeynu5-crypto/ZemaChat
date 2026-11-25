package com.zemachat

import android.content.Context
import org.telegram.messenger.ApplicationLoader
import org.telegram.tgnet.TLObject

object ZemaGhostHelper {
    private const val PREFS = "zema_ghost_prefs"

    fun isHideOnline(ctx: Context): Boolean = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("hide_online", false)
    fun isHideRead(ctx: Context): Boolean = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("hide_read", false)
    fun isHideTyping(ctx: Context): Boolean = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("hide_typing", false)

    fun setHideOnline(ctx: Context, v: Boolean) { ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("hide_online", v).apply() }
    fun setHideRead(ctx: Context, v: Boolean) { ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("hide_read", v).apply() }
    fun setHideTyping(ctx: Context, v: Boolean) { ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("hide_typing", v).apply() }

    @JvmStatic
    fun filterRequest(obj: TLObject?): Boolean {
        if (obj == null) return false
        val ctx = ApplicationLoader.applicationContext ?: return false
        val name = obj.javaClass.simpleName
        if (isHideOnline(ctx) && (name == "TL_account_updateStatus" || name == "updateStatus")) {
            try {
                obj.javaClass.getField("offline").setBoolean(obj, true)
            } catch (_: Exception) {}
            return false
        }
        if (isHideRead(ctx) && (name == "TL_messages_readHistory" || name == "TL_channels_readHistory" || name == "TL_messages_readEncryptedHistory" || name == "TL_messages_readMessageContents")) {
            return true
        }
        if (isHideTyping(ctx) && name == "TL_messages_setTyping") {
            return true
        }
        return false
    }
}
