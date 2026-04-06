package com.chenyue404.sharetext

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
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
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_RESTART_SERVER) {
            stopServerIfNeeded(clearError = true)
            startServerIfNeeded()
            if (serviceState.value == ServiceState.RUNNING) {
                showOrUpdateNotification()
            }
            return START_STICKY
        }

        startServerIfNeeded()
        if (serviceState.value == ServiceState.RUNNING) {
            showOrUpdateNotification()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopServerIfNeeded(clearError = false)
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
                _lastError.value = PortAdvice.occupiedMessage(currentPort)
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
        return assets.open("main.html").bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun showOrUpdateNotification() {
        ensureChannel()
        val address = "http://${getLocalIpAddress()}:${getPort()}"
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
                address
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
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("ShareText 服务运行中")
            .setContentText(address)
            .setStyle(NotificationCompat.BigTextStyle().bigText(address))
            .setOngoing(true)
            .setContentIntent(openMainIntent)
            .addAction(android.R.drawable.ic_menu_camera, "二维码", openQrIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止服务", stopIntent)
            .build()

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "ShareText 服务",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)
    }

    private fun getLocalIpAddress(): String {
        val fromActiveNetwork = getIpFromActiveNetwork()
        if (fromActiveNetwork != null) return fromActiveNetwork
        return getIpFromInterfaces() ?: "127.0.0.1"
    }

    private fun getIpFromActiveNetwork(): String? {
        return try {
            val cm = getSystemService(ConnectivityManager::class.java) ?: return null
            val network = cm.activeNetwork ?: return null
            val link = cm.getLinkProperties(network) ?: return null
            link.linkAddresses
                .mapNotNull { it.address as? Inet4Address }
                .firstOrNull { isUsableLanIpv4(it) }
                ?.hostAddress
        } catch (_: Exception) {
            null
        }
    }

    private fun getIpFromInterfaces(): String? {
        return try {
            val all = NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .sortedByDescending {
                    val n = it.name.lowercase()
                    when {
                        n.startsWith("wlan") -> 3
                        n.startsWith("eth") -> 2
                        else -> 1
                    }
                }

            all.asSequence()
                .flatMap { it.inetAddresses.toList().asSequence() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { isUsableLanIpv4(it) }
                ?.hostAddress
        } catch (_: Exception) {
            null
        }
    }

    private fun isUsableLanIpv4(ip: InetAddress): Boolean {
        if (ip.isAnyLocalAddress || ip.isLoopbackAddress || ip.isLinkLocalAddress || ip.isMulticastAddress) {
            return false
        }
        val host = ip.hostAddress ?: return false
        if (host.startsWith("169.254.")) return false
        return host.startsWith("10.")
                || host.startsWith("192.168.")
                || host.matches(Regex("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..+"))
    }

    private fun simplifyError(e: Throwable): String {
        val msg = e.message?.trim().orEmpty()
        if (msg.contains("Address already in use", ignoreCase = true)) {
            return PortAdvice.occupiedMessage(getPort())
        }
        return if (msg.isEmpty()) "服务启动失败" else "服务启动失败：$msg"
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
            context.startService(Intent(context, WebService::class.java))
        }

        fun requestStop(context: Context) {
            context.startService(
                Intent(context, WebService::class.java).setAction(
                    ACTION_STOP_SERVER
                )
            )
        }

        fun requestRestart(context: Context) {
            context.startService(
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
