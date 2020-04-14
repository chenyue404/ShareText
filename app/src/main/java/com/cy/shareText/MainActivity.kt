package com.cy.shareText

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.ServiceUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class MainActivity : AppCompatActivity() {

    lateinit var runMenu: MenuItem
    var serverStatus = WebServerStatusEvent.STATUS_STOP
    private val intent = lazy {
        Intent(this, WebServer::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        runMenu = menu!!.findItem(R.id.m_start)

        val oldStatus = serverStatus
        val drawableId: Int
        if (ServiceUtils.isServiceRunning(WebServer::class.java)) {
            drawableId = android.R.drawable.ic_media_pause
            serverStatus = WebServerStatusEvent.STATUS_START
        } else {
            drawableId = android.R.drawable.ic_media_play
            serverStatus = WebServerStatusEvent.STATUS_STOP
        }
        if (oldStatus != serverStatus) {
            runMenu.icon =
                ContextCompat.getDrawable(
                    this@MainActivity,
                    drawableId
                )
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.m_start -> {
                if (serverStatus == WebServerStatusEvent.STATUS_STOP) {
                    startServer()
                } else {
                    stopServer()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startServer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent.value)
        } else {
            startService(intent.value)
        }
    }

    private fun stopServer() {
        stopService(intent.value)
    }

    override fun onStart() {
        EventBus.getDefault().register(this)
        super.onStart()
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Subscribe
    public fun onWebServerStatusEvent(event: WebServerStatusEvent) {
        val status = event.status
        serverStatus = status
        when (status) {
            WebServerStatusEvent.STATUS_START -> {
                runMenu.icon =
                    ContextCompat.getDrawable(
                        this@MainActivity,
                        android.R.drawable.ic_media_pause
                    )
            }
            WebServerStatusEvent.STATUS_STOP -> {
                runMenu.icon =
                    ContextCompat.getDrawable(
                        this@MainActivity,
                        android.R.drawable.ic_media_play
                    )
            }
            WebServerStatusEvent.STATUS_ERROR -> {

            }
        }
    }
}
