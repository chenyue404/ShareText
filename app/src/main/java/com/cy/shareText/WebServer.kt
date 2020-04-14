package com.cy.shareText

import android.app.IntentService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.yanzhenjie.andserver.AndServer
import com.yanzhenjie.andserver.Server
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

class WebServer : IntentService(WebServer::class.simpleName) {
    private var mServer: Lazy<Server> = lazy {
        AndServer.webServer(this)
            .port(8080)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId: String = getString(R.string.app_name)
            val channel =
                NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_NONE)
            //设置绕过免打扰模式
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                channel
            )
            val notification: Notification =
                Notification.Builder(this, channelId).build()
            startForeground(1, notification)
        }
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