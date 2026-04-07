package com.chenyue404.sharetext

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.ktor.http.ContentType
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

data class ShareTextItem(
    val id: Int,
    val text: String,
    val timeMillis: Long
)

private data class RemovedItem(
    val item: ShareTextItem,
    val index: Int
)

class WebService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVER) {
            stopServerIfNeeded(clearError = false)
            removeForegroundNotification()
            stopSelf()
            return START_NOT_STICKY
        }

        promoteToForeground(getString(R.string.notify_service_starting))

        if (intent?.action == ACTION_RESTART_SERVER) {
            stopServerIfNeeded(clearError = true)
        }

        startServerIfNeeded()
        if (serviceState.value == ServiceState.RUNNING) {
            promoteToForeground("http://${LanIpResolver.resolve(this)}:${getPort()}")
        } else {
            promoteToForeground(lastError.value ?: getString(R.string.error_service_start_failed))
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopServerIfNeeded(clearError = false)
        removeForegroundNotification()
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
        _serviceState.value = ServiceState.STOPPED
        super.onDestroy()
    }

    private fun startServerIfNeeded() {
        synchronized(serverLock) {
            if (server != null) {
                _serviceState.value = ServiceState.RUNNING
                _lastError.value = null
                return
            }

            if (!PortConfig.isPortAvailable(currentPort)) {
                _serviceState.value = ServiceState.ERROR
                _lastError.value = PortAdvice.occupiedMessage(this, currentPort)
                return
            }

            try {
                val localServer = embeddedServer(
                    CIO,
                    port = currentPort
                ) {
                    routing {
                        get("/") {
                            call.respondText(loadMainHtml(), ContentType.Text.Html)
                        }

                        get("/list") {
                            val data = synchronized(serverLock) {
                                val array = JSONArray()
                                textItems.forEach { item ->
                                    val obj = JSONObject()
                                        .put("id", item.id)
                                        .put("text", item.text)
                                        .put("timeMillis", item.timeMillis)
                                    array.put(obj)
                                }
                                array.toString()
                            }
                            call.respondText(data, ContentType.Application.Json)
                        }

                        post("/addText") {
                            val params = call.receiveParameters()
                            val text = normalizeText(params["text"])
                            if (text != null) {
                                addText(text)
                            }
                            call.respondText("ok", ContentType.Text.Plain)
                        }

                        post("/removeAt") {
                            val params = call.receiveParameters()
                            val index = params["index"]?.toIntOrNull()
                            if (index != null) {
                                removeAt(index)
                            }
                            call.respondText("ok", ContentType.Text.Plain)
                        }

                        post("/removeById") {
                            val params = call.receiveParameters()
                            val id = params["id"]?.toIntOrNull()
                            if (id != null) {
                                removeById(id)
                            }
                            call.respondText("ok", ContentType.Text.Plain)
                        }

                        post("/undoRemove") {
                            undoLastRemove()
                            call.respondText("ok", ContentType.Text.Plain)
                        }
                    }
                }

                expectedStop = false
                server = localServer
                val thread = Thread {
                    try {
                        localServer.start(wait = true)
                    } catch (e: Throwable) {
                        synchronized(serverLock) {
                            if (server === localServer && !expectedStop) {
                                _serviceState.value = ServiceState.ERROR
                                _lastError.value = simplifyError(e)
                            }
                        }
                    } finally {
                        synchronized(serverLock) {
                            val isCurrentServer = server === localServer
                            if (isCurrentServer) {
                                server = null
                                serverThread = null
                            }
                            if (isCurrentServer && !expectedStop && _serviceState.value != ServiceState.ERROR) {
                                _serviceState.value = ServiceState.STOPPED
                            }
                        }
                    }
                }.apply {
                    name = "sharetext-server"
                    isDaemon = true
                }
                serverThread = thread
                thread.start()

                _serviceState.value = ServiceState.RUNNING
                _lastError.value = null
            } catch (e: Throwable) {
                server = null
                _serviceState.value = ServiceState.ERROR
                _lastError.value = simplifyError(e)
            }
        }
    }

    private fun stopServerIfNeeded(clearError: Boolean) {
        synchronized(serverLock) {
            expectedStop = true
            val running = server
            if (running != null) {
                running.stop(500, 1000)
                server = null
            }
            serverThread = null
            _serviceState.value = ServiceState.STOPPED
            if (clearError) {
                _lastError.value = null
            }
        }
    }

    private fun loadMainHtml(): String {
        val html = assets.open("main.html").bufferedReader(Charsets.UTF_8).use { it.readText() }
        return html.replace("__APP_LANG__", resolveWebLanguageTag())
    }

    private fun resolveWebLanguageTag(): String {
        val locale: Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale
        }
        return locale.toLanguageTag()
    }

    private fun promoteToForeground(contentText: String) {
        ensureChannel()
        val notification = buildNotification(contentText)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(contentText: String): android.app.Notification {
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val openMainIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java),
            pendingIntentFlags
        )
        val openQrIntent = PendingIntent.getActivity(
            this,
            2,
            Intent(this, QRCodeActivity::class.java).putExtra(
                QRCodeActivity.EXTRA_ADDRESS,
                if (serviceState.value == ServiceState.RUNNING) {
                    "http://${LanIpResolver.resolve(this)}:${getPort()}"
                } else {
                    ""
                }
            ),
            pendingIntentFlags
        )
        val stopIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, WebService::class.java).setAction(ACTION_STOP_SERVER),
            pendingIntentFlags
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notify_service_running))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setOngoing(true)
            .setContentIntent(openMainIntent)
            .addAction(android.R.drawable.ic_menu_camera, getString(R.string.desc_qrcode), openQrIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.desc_stop_service), stopIntent)
            .build()

        return notification
    }

    private fun removeForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notify_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)
    }

    private fun simplifyError(e: Throwable): String {
        val msg = e.message?.trim().orEmpty()
        if (msg.contains("Address already in use", ignoreCase = true)) {
            return PortAdvice.occupiedMessage(this, getPort())
        }
        return if (msg.isEmpty()) {
            getString(R.string.error_service_start_failed)
        } else {
            getString(R.string.error_service_start_failed_with_detail, msg)
        }
    }

    companion object {
        private const val DEFAULT_PORT = 3080
        private const val CHANNEL_ID = "share_text_service"
        private const val NOTIFICATION_ID = 2001
        private const val ACTION_STOP_SERVER = "com.chenyue404.sharetext.action.STOP_SERVER"
        private const val ACTION_RESTART_SERVER = "com.chenyue404.sharetext.action.RESTART_SERVER"

        private val serverLock = Any()
        private var server: EmbeddedServer<*, *>? = null
        private var serverThread: Thread? = null
        private var expectedStop = false
        private var currentPort = DEFAULT_PORT

        private val idSeed = AtomicInteger(0)
        private val textItems = mutableListOf<ShareTextItem>()
        private var lastRemovedItem: RemovedItem? = null

        private val _listFlow = MutableStateFlow<List<ShareTextItem>>(emptyList())
        val listFlow = _listFlow.asStateFlow()

        private val _serviceState = MutableStateFlow(ServiceState.STOPPED)
        val serviceState = _serviceState.asStateFlow()

        private val _lastError = MutableStateFlow<String?>(null)
        val lastError = _lastError.asStateFlow()

        private val _portFlow = MutableStateFlow(DEFAULT_PORT)
        val portFlow = _portFlow.asStateFlow()

        fun requestStart(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, WebService::class.java))
        }

        fun requestStop(context: Context) {
            context.startService(
                Intent(context, WebService::class.java).setAction(
                    ACTION_STOP_SERVER
                )
            )
        }

        fun requestRestart(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, WebService::class.java).setAction(
                    ACTION_RESTART_SERVER
                )
            )
        }

        fun addText(text: String) {
            val value = normalizeText(text) ?: return
            synchronized(serverLock) {
                val item = ShareTextItem(
                    id = idSeed.incrementAndGet(),
                    text = value,
                    timeMillis = System.currentTimeMillis()
                )
                textItems.add(item)
                lastRemovedItem = null
                _listFlow.value = textItems.toList()
            }
        }

        fun getPort(): Int = synchronized(serverLock) { currentPort }

        fun setPort(newPort: Int): Boolean {
            if (newPort !in 1..65535) return false
            synchronized(serverLock) {
                currentPort = newPort
                _portFlow.value = newPort
            }
            return true
        }

        fun clearAll() {
            synchronized(serverLock) {
                textItems.clear()
                lastRemovedItem = null
                _listFlow.value = emptyList()
            }
        }

        fun removeAt(index: Int): Boolean {
            synchronized(serverLock) {
                if (index < 0 || index >= textItems.size) return false
                val removed = textItems.removeAt(index)
                lastRemovedItem = RemovedItem(item = removed, index = index)
                _listFlow.value = textItems.toList()
                return true
            }
        }

        fun removeById(id: Int): Boolean {
            synchronized(serverLock) {
                val index = textItems.indexOfFirst { it.id == id }
                if (index == -1) return false
                val removed = textItems.removeAt(index)
                lastRemovedItem = RemovedItem(item = removed, index = index)
                _listFlow.value = textItems.toList()
                return true
            }
        }

        fun undoLastRemove(): Boolean {
            synchronized(serverLock) {
                val pending = lastRemovedItem ?: return false
                val restoreIndex = pending.index.coerceIn(0, textItems.size)
                textItems.add(restoreIndex, pending.item)
                lastRemovedItem = null
                _listFlow.value = textItems.toList()
                return true
            }
        }

        private fun normalizeText(raw: String?): String? {
            val value = raw?.trim() ?: return null
            if (value.isEmpty()) return null
            if (value.equals("null", ignoreCase = true)) return null
            if (value.equals("undefined", ignoreCase = true)) return null
            return value
        }
    }
}

enum class ServiceState {
    RUNNING,
    STOPPED,
    ERROR
}






