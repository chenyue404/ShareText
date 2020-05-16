package com.cy.shareText

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Created by Eddie on 2020/4/18 0018.
 */
class StopServerReceiver : BroadcastReceiver() {
    companion object {
        val NEED_RESTART = "need_restart"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) {
            return
        }
        val serviceIntent = Intent(context, WebServer::class.java)
        context.stopService(serviceIntent)
        val needRestart = intent?.getBooleanExtra(NEED_RESTART, false)
        if (null != needRestart && needRestart) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}