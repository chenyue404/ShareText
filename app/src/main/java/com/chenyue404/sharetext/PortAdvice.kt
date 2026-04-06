package com.chenyue404.sharetext

import android.content.Context

object PortAdvice {

    fun occupiedMessage(
        context: Context,
        port: Int,
        suggest: (Int) -> Int? = PortConfig::suggestAvailablePort
    ): String {
        val suggestion = suggest(port)
        return if (suggestion != null) {
            context.getString(R.string.port_occupied_with_suggestion, suggestion)
        } else {
            context.getString(R.string.port_occupied_change_port)
        }
    }

    fun saveFailedOccupiedMessage(
        context: Context,
        port: Int,
        suggest: (Int) -> Int? = PortConfig::suggestAvailablePort
    ): String {
        return context.getString(
            R.string.port_save_failed,
            occupiedMessage(context, port, suggest)
        )
    }

    fun saveSuccessMessage(context: Context, port: Int): String {
        return context.getString(R.string.port_saved_restart, port)
    }

    fun portChangedMessage(context: Context, port: Int): String {
        return context.getString(R.string.port_switched_to, port)
    }
}
