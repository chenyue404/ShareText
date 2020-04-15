package com.cy.shareText

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.CacheMemoryUtils
import com.blankj.utilcode.util.ServiceUtils
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class MainActivity : AppCompatActivity() {

    companion object {
        val KEY_CACHE = "list"
    }

    private lateinit var runMenu: MenuItem
    private var serverStatus = WebServerStatusEvent.STATUS_STOP
    private val intent = lazy {
        Intent(this, WebServer::class.java)
    }
    private val dataList = arrayListOf<String>()
    private lateinit var adapter: ListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
        setContentView(R.layout.activity_main)
        initList()

        bt_send.setOnClickListener {
            val text = et_input.text.toString().trim()
            if (TextUtils.isEmpty(text)) {
                return@setOnClickListener
            }
            addText(text)
        }
        rv_list.post { startServer() }

        getShareText();
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    override fun onResume() {
        super.onResume()
        val list = CacheMemoryUtils.getInstance().get<ArrayList<String>>(KEY_CACHE)
        if (list != null && !list.equals(dataList)) {
            dataList.clear()
            dataList.addAll(list)
            adapter.notifyDataSetChanged()
            rv_list.scrollToPosition(dataList.size - 1)
        }
    }

    private fun addText(text: String) {
        et_input.text?.clear()
        dataList.add(text)
        adapter.notifyItemChanged(dataList.size - 1)
        rv_list.scrollToPosition(dataList.size - 1)
        CacheMemoryUtils.getInstance().put(KEY_CACHE, dataList)
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

    private fun initList() {
        adapter = ListAdapter(dataList)
        rv_list.adapter = adapter
        val dimen = resources.getDimension(R.dimen.list_space).toInt()
        rv_list.addItemDecoration(SpaceItemDecoration(dimen, 0, dimen, dimen))
    }

    @Subscribe
    public fun onNewTetEvent(event: NewTextEvent) {
        val text = event.text
        if (TextUtils.isEmpty(text)) {
            return
        }
        rv_list.post {
            dataList.clear()
            dataList.addAll(
                CacheMemoryUtils.getInstance().get<ArrayList<String>>(KEY_CACHE)
            )
            adapter.notifyItemInserted(dataList.size - 1)
            rv_list.scrollToPosition(adapter.getItemCount() - 1)
        }
    }

    private fun getShareText() {
        val intent = getIntent()
        val action = intent.action
        val type = intent.type
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                val share = intent.getStringExtra(Intent.EXTRA_TEXT)
                addText(share)
            }
        }
    }
}
