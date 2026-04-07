package com.cy.shareText

import android.content.Context
import java.net.ServerSocket

object PortConfig {
    private const val PREF_NAME = "share_text_prefs"
    private const val KEY_SERVER_PORT = "server_port"
    private const val DEFAULT_PORT = 3080

    fun load(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_SERVER_PORT, DEFAULT_PORT)
    }

    fun save(context: Context, port: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_SERVER_PORT, port)
            .apply()
    }

    fun isPortAvailable(port: Int): Boolean {
        if (port !in 1..65535) return false
        return try {
            ServerSocket(port).use { true }
        } catch (_: Exception) {
            false
        }
    }

    fun suggestAvailablePort(
        preferredPort: Int,
        check: (Int) -> Boolean = ::isPortAvailable
    ): Int? {
        if (preferredPort !in 1..65535) return null

        for (port in (preferredPort + 1)..65535) {
            if (check(port)) return port
        }
        for (port in 1 until preferredPort) {
            if (check(port)) return port
        }
        return null
    }
}

