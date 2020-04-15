package com.cy.shareText

import android.app.IntentService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.blankj.utilcode.util.NetworkUtils
import com.yanzhenjie.andserver.AndServer
import com.yanzhenjie.andserver.Server
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

class WebServer : IntentService(WebServer::class.simpleName) {
    companion object {
        val port = 3080
    }

    private var mServer: Lazy<Server> = lazy {
        AndServer.webServer(this)
            .port(port)
            .timeout(10, TimeUnit.SECONDS)
            .listener(object : Server.ServerListener {
                override fun onStarted() {
                    EventBus.getDefault()
                        .post(WebServerStatusEvent(WebServerStatusEvent.STATUS_START))
                }

                override fun onStopped() {
                    EventBus.getDefault()
                        .post(WebServerStatusEvent(WebServerStatusEvent.STATUS_STOP))
                }

                override fun onException(e: Exception) {
                    Log.e("123", e.toString())
                }
            })
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        val channelId: String = getString(R.string.app_name)
        val builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, channelId)
            } else {
                Notification.Builder(this)
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_NONE)
            //设置绕过免打扰模式
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                channel
            )

        }
        val address = NetworkUtils.getIPAddress(true) + ":" + port
        val notification: Notification =
            builder
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.running))
                .setContentText(address)
                .setStyle(Notification.BigTextStyle().bigText(address))
                .build()
        startForeground(1, notification)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onHandleIntent(p0: Intent?) {
    }

    override fun onStart(intent: Intent?, startId: Int) {
        mServer.value.startup()
    }

    override fun onDestroy() {
        mServer.value.shutdown()
    }
}