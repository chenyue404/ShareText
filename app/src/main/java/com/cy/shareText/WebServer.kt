package com.cy.shareText

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.NetworkUtils
import com.yanzhenjie.andserver.AndServer
import com.yanzhenjie.andserver.Server
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

class WebServer : IntentService(WebServer::class.simpleName) {
    companion object {
        const val port = 3080
        var address = ""
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
                    LogUtils.e(e.toString())
                    EventBus.getDefault()
                        .post(WebServerStatusEvent(WebServerStatusEvent.STATUS_ERROR).also {
                            it.errorMsg = e.toString()
                        })
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
                NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_DEFAULT)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                channel
            )
        }
        address = NetworkUtils.getIpAddressByWifi() + ":" + port
        val notification: Notification =
            builder
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(getString(R.string.running))
                .setContentText(address)
                .setStyle(Notification.BigTextStyle().bigText(address))
                .setContentIntent(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        0
                    )
                )
                .addAction(
                    Notification.Action.Builder(
                        R.drawable.qr_code,
                        getString(R.string.qrcode),
                        PendingIntent.getActivity(
                            this,
                            0,
                            Intent(this, QRCodeActivity::class.java),
                            0
                        )
                    ).build()
                )
                .addAction(
                    Notification.Action.Builder(
                        android.R.drawable.ic_media_pause,
                        getString(R.string.end_server),
                        PendingIntent.getBroadcast(
                            this,
                            0,
                            Intent(this, StopServerReceiver::class.java),
                            0
                        )
                    ).build()
                )
                .build()
        startForeground(
            1, notification
        )
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