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
import androidx.recyclerview.widget.DefaultItemAnimator
import com.blankj.utilcode.util.CacheMemoryUtils
import com.blankj.utilcode.util.ServiceUtils
import com.blankj.utilcode.util.ToastUtils
import com.cy.shareText.list.ListAdapter
import com.cy.shareText.list.SpaceItemDecoration
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe


class MainActivity : AppCompatActivity() {

    companion object {
        const val KEY_CACHE = "list"
    }

    private lateinit var runMenu: MenuItem
    private var serverStatus = WebServerStatusEvent.STATUS_STOP
    private val intent = lazy {
        Intent(this, WebServer::class.java)
    }
    private var dataList = arrayListOf<String>()
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

        getShareText()
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    private fun addText(text: String) {
        et_input.text?.clear()
        dataList.add(text)
        adapter.notifyItemInserted(dataList.size - 1)
        rv_list.scrollToPosition(dataList.size - 1)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        runMenu = menu!!.findItem(R.id.m_start)

        if (ServiceUtils.isServiceRunning(WebServer::class.java)) {
            changeStatus(WebServerStatusEvent.STATUS_START)
        } else {
            changeStatus(WebServerStatusEvent.STATUS_STOP)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.m_start -> {
                if (serverStatus != WebServerStatusEvent.STATUS_START) {
                    startServer()
                } else {
                    stopServer()
                }
            }
            R.id.m_setting -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
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
        changeStatus(status)
        if (status == WebServerStatusEvent.STATUS_ERROR) {
            ToastUtils.showShort(event.errorMsg)
        }
    }

    private fun changeStatus(status: Int) {
        serverStatus = status
        when (status) {
            WebServerStatusEvent.STATUS_START -> {
                runMenu.icon =
                    ContextCompat.getDrawable(
                        this@MainActivity,
                        android.R.drawable.ic_media_pause
                    )
                runMenu.title = getString(R.string.end_server)
            }
            WebServerStatusEvent.STATUS_STOP,
            WebServerStatusEvent.STATUS_ERROR
            -> {
                runMenu.icon =
                    ContextCompat.getDrawable(
                        this@MainActivity,
                        android.R.drawable.ic_media_play
                    )
                runMenu.title = getString(R.string.start_server)
            }
        }
    }

    private fun initList() {
        var list = CacheMemoryUtils.getInstance().get<ArrayList<String>>(KEY_CACHE)
        if (list == null) {
            list = arrayListOf()
        }
        dataList = list
        CacheMemoryUtils.getInstance().put(KEY_CACHE, dataList)
        adapter = ListAdapter(dataList)
        rv_list.adapter = adapter
        val dimen = resources.getDimension(R.dimen.list_space).toInt()
        rv_list.addItemDecoration(
            SpaceItemDecoration(
                dimen,
                0,
                dimen,
                dimen
            )
        )
        rv_list.itemAnimator = DefaultItemAnimator()
    }

    @Subscribe
    public fun onNewTetEvent(event: NewTextEvent) {
        val text = event.text
        if (TextUtils.isEmpty(text)) {
            return
        }
        rv_list.post {
            adapter.notifyItemInserted(adapter.itemCount - 1)
            rv_list.scrollToPosition(adapter.itemCount - 1)
        }
    }

    private fun getShareText() {
        val intent = getIntent()
        val action = intent.action
        val type = intent.type
        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                val share = intent.getStringExtra(Intent.EXTRA_TEXT)
                share?.let {
                    addText(share)
                }
            }
        }
    }
}
