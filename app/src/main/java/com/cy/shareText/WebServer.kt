package com.cy.shareText

import android.app.*
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.blankj.utilcode.util.CacheMemoryUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.NetworkUtils
import com.yanzhenjie.andserver.AndServer
import com.yanzhenjie.andserver.Server
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

class WebServer : IntentService(WebServer::class.simpleName) {
    private val msg_id = 1
    private var port = 3080

    companion object {
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
                    stopSelf()
                }
            })
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        val portString = PreferenceManager.getDefaultSharedPreferences(this)
            .getString(
                getString(R.string.preference_port_key)
                , resources.getInteger(R.integer.preference_port_default_value).toString()
            )
        portString?.let {
            port = it.toInt()
        }
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
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_MAX)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(msg_id, notification)
        } else {
            with(NotificationManagerCompat.from(this)) {
                notify(msg_id, notification)
            }
        }
        listenClipboardManager()
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
        stopListenClipboardManager()
        NotificationManagerCompat.from(this@WebServer).cancel(msg_id)
    }

    private val listener: ClipboardManager.OnPrimaryClipChangedListener =
        ClipboardManager.OnPrimaryClipChangedListener {
            val clipboardManager =
                this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            if (clipboardManager.hasPrimaryClip() && clipboardManager.primaryClip!!.itemCount > 0) {
                val text = clipboardManager.primaryClip!!.getItemAt(0).text
                LogUtils.e(text)

                if (text.isEmpty()) {
                    return@OnPrimaryClipChangedListener
                }

                var list = CacheMemoryUtils.getInstance()
                    .get<ArrayList<String>>(MainActivity.KEY_CACHE)
                if (list == null) {
                    list = arrayListOf()
                } else if (list.last() == text.toString()) {
                    return@OnPrimaryClipChangedListener
                }
                list.add(text.toString())
                CacheMemoryUtils.getInstance().put(MainActivity.KEY_CACHE, list)
                EventBus.getDefault().post(NewTextEvent(text.toString()))
            }
        }

    private fun listenClipboardManager() {
        val clipboardManager = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager
            .addPrimaryClipChangedListener(listener)
    }

    private fun stopListenClipboardManager() {
        val clipboardManager = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager
            .removePrimaryClipChangedListener(listener)
    }
}