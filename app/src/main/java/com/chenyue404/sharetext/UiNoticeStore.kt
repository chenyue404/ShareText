package com.chenyue404.sharetext

import android.content.Context

object UiNoticeStore {
    private const val PREF_NAME = "share_text_notice_prefs"
    private const val KEY_PORT_CHANGED = "port_changed_notice"

    fun savePortChanged(context: Context, port: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PORT_CHANGED, PortAdvice.portChangedMessage(context, port))
            .apply()
    }

    fun consumePortChanged(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val value = prefs.getString(KEY_PORT_CHANGED, null)
        if (value != null) {
            prefs.edit().remove(KEY_PORT_CHANGED).apply()
        }
        return value
    }
}
