package com.cy.shareText

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Created by Eddie on 2020/4/18 0018.
 */
class StopServerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.stopService(Intent(context, WebServer::class.java))
    }
}