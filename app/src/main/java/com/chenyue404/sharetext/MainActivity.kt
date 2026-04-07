package com.cy.shareText

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyStatusBarStyle()
        requestNotificationPermissionIfNeeded()

        WebService.setPort(PortConfig.load(this))
        WebService.requestStart(this)
        handleShareIntent(intent)

        setContent {
            MaterialTheme {
                Scaffold(
                    containerColor = UiStyle.ScreenBackground,
                    contentWindowInsets = WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                ) { innerPadding ->
                    MainScreen(
                        serverIp = LanIpResolver.resolve(this),
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        val notice = UiNoticeStore.consumePortChanged(this)
        if (!notice.isNullOrBlank()) {
            Toast.makeText(this, notice, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleShareIntent(inIntent: Intent?) {
        if (inIntent?.action != Intent.ACTION_SEND) return
        if (inIntent.type != "text/plain") return
        val rawText = inIntent.getStringExtra(Intent.EXTRA_TEXT) ?: return
        val text = rawText.trim()
        if (text.isEmpty()) return
        WebService.addText(text)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_CODE_NOTIFICATIONS
        )
    }

    companion object {
        private const val REQUEST_CODE_NOTIFICATIONS = 1001
    }
}

@Composable
private fun MainScreen(serverIp: String, modifier: Modifier = Modifier) {
    val list by WebService.listFlow.collectAsState()
    val status by WebService.serviceState.collectAsState()
    val currentPort by WebService.portFlow.collectAsState()
    val lastError by WebService.lastError.collectAsState()
    var input by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                ""
            )
        )
    }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val actionIconTint = UiStyle.PrimaryAction
    val listState = rememberLazyListState()
    val canSend = input.text.trim().isNotEmpty()
    var undoVisible by rememberSaveable { mutableStateOf(false) }
    var undoLabel by rememberSaveable { mutableStateOf("") }
    var undoVersion by rememberSaveable { mutableStateOf(0) }

    fun sendInput() {
        val text = input.text.trim()
        if (text.isEmpty()) return
        WebService.addText(text)
        input = TextFieldValue("")
    }

    LaunchedEffect(list.size) {
        if (list.isNotEmpty()) {
            listState.animateScrollToItem(list.lastIndex)
        }
    }

    LaunchedEffect(undoVersion) {
        if (!undoVisible) return@LaunchedEffect
        delay(5000)
        undoVisible = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(UiStyle.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(UiStyle.InnerSpacing)
    ) {
        val statusText = when (status) {
            ServiceState.RUNNING -> stringResource(R.string.status_running)
            ServiceState.STOPPED -> stringResource(R.string.status_stopped)
            ServiceState.ERROR -> stringResource(R.string.status_error)
        }
        val statusColor = when (status) {
            ServiceState.RUNNING -> UiStyle.RunningText
            ServiceState.STOPPED -> UiStyle.StoppedText
            ServiceState.ERROR -> UiStyle.ErrorText
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = UiStyle.StatusCardBackground),
            shape = RoundedCornerShape(UiStyle.CornerRadius),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(UiStyle.CardPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.status_line, statusText),
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (status == ServiceState.RUNNING) {
                        val address = context.getString(R.string.server_address_format, serverIp, currentPort)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = address,
                                style = MaterialTheme.typography.bodySmall,
                                color = UiStyle.SecondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(
                                modifier = Modifier.size(UiStyle.IconButtonSize),
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(address))
                                    Toast.makeText(context, context.getString(R.string.toast_address_copied), Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = stringResource(R.string.desc_copy_address),
                                    tint = actionIconTint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.address_unavailable),
                            style = MaterialTheme.typography.bodySmall,
                            color = UiStyle.SecondaryText
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.End) {
                    IconButton(
                        modifier = Modifier.size(UiStyle.IconButtonSize),
                        onClick = {
                            context.startActivity(
                                Intent(
                                    context,
                                    SettingsActivity::class.java
                                )
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.desc_settings),
                            tint = actionIconTint
                        )
                    }
                    IconButton(
                        modifier = Modifier.size(UiStyle.IconButtonSize),
                        onClick = { WebService.clearAll() }) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = stringResource(R.string.desc_clear_all),
                            tint = actionIconTint
                        )
                    }
                    if (status == ServiceState.RUNNING) {
                        IconButton(
                            modifier = Modifier.size(UiStyle.IconButtonSize),
                            onClick = {
                                val go = Intent(context, QRCodeActivity::class.java)
                                go.putExtra(
                                    QRCodeActivity.EXTRA_ADDRESS,
                                    "http://$serverIp:$currentPort"
                                )
                                context.startActivity(go)
                            }
                        ) {
                                Icon(
                                    imageVector = Icons.Filled.QrCode2,
                                    contentDescription = stringResource(R.string.desc_qrcode),
                                    tint = actionIconTint
                                )
                            }
                    }
                    IconButton(
                        modifier = Modifier.size(UiStyle.IconButtonSize),
                        onClick = {
                            if (status == ServiceState.RUNNING) {
                                WebService.requestStop(context)
                            } else {
                                WebService.requestStart(context)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (status == ServiceState.RUNNING) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (status == ServiceState.RUNNING) {
                                stringResource(R.string.desc_stop_service)
                            } else {
                                stringResource(R.string.desc_start_service)
                            },
                            tint = actionIconTint
                        )
                    }
                }
            }
            if (status == ServiceState.ERROR && !lastError.isNullOrBlank()) {
                Text(
                    text = lastError!!,
                    color = UiStyle.ErrorText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = UiStyle.CardPadding, vertical = 2.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(UiStyle.InnerSpacing)
        ) {
            if (list.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = UiStyle.EmptyCardBackground),
                        shape = RoundedCornerShape(UiStyle.CornerRadius),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.empty_list_hint),
                            modifier = Modifier.padding(UiStyle.CardPadding),
                            color = UiStyle.SecondaryText
                        )
                    }
                }
            }
            itemsIndexed(list) { _, item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = UiStyle.ListCardBackground),
                    shape = RoundedCornerShape(UiStyle.CornerRadius),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(UiStyle.CardPadding),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = item.text)
                            Text(
                                text = formatTime(item.timeMillis),
                                style = MaterialTheme.typography.bodySmall,
                                color = UiStyle.SecondaryText
                            )
                        }
                        Row(verticalAlignment = Alignment.Bottom) {
                            IconButton(
                                modifier = Modifier.size(UiStyle.IconButtonSize),
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(item.text))
                                    Toast.makeText(context, context.getString(R.string.toast_item_copied), Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = stringResource(R.string.desc_copy_item),
                                    tint = actionIconTint
                                )
                            }
                            IconButton(
                                modifier = Modifier.size(UiStyle.IconButtonSize),
                                onClick = {
                                    if (WebService.removeById(item.id)) {
                                        undoLabel = item.text
                                        undoVisible = true
                                        undoVersion += 1
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.desc_delete_item),
                                    tint = actionIconTint
                                )
                            }
                        }
                    }
                }
            }
        }

        if (undoVisible) {
            Card(
                colors = CardDefaults.cardColors(containerColor = UiStyle.StatusCardBackground),
                shape = RoundedCornerShape(UiStyle.CornerRadius),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = UiStyle.CardPadding, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.undo_line, undoLabel.take(18)),
                        modifier = Modifier.weight(1f),
                        color = UiStyle.SecondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(
                        onClick = {
                            if (WebService.undoLastRemove()) {
                                undoVisible = false
                                Toast.makeText(context, context.getString(R.string.toast_undo_done), Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.undo_action))
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = UiStyle.InputCardBackground),
            shape = RoundedCornerShape(UiStyle.CornerRadius),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(UiStyle.CardPadding),
                horizontalArrangement = Arrangement.spacedBy(UiStyle.InnerSpacing),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.Bottom),
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text(stringResource(R.string.input_placeholder)) },
                    singleLine = false,
                    minLines = 1,
                    maxLines = UiStyle.InputMaxLines
                )
                IconButton(
                    modifier = Modifier
                        .size(UiStyle.IconButtonSize)
                        .align(Alignment.Bottom),
                    onClick = {
                        val clipText = clipboardManager.getText()?.text?.trim().orEmpty()
                        if (clipText.isEmpty()) {
                            Toast.makeText(context, context.getString(R.string.toast_clipboard_empty), Toast.LENGTH_SHORT)
                                .show()
                            return@IconButton
                        }
                        input = TextFieldValue(
                            text = clipText,
                            selection = TextRange(clipText.length)
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentPaste,
                        contentDescription = stringResource(R.string.desc_fill_clipboard),
                        tint = actionIconTint
                    )
                }
                IconButton(
                    modifier = Modifier
                        .size(UiStyle.IconButtonSize)
                        .align(Alignment.Bottom),
                    enabled = canSend,
                    onClick = { sendInput() }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = stringResource(R.string.desc_send),
                        tint = if (canSend) actionIconTint else UiStyle.SecondaryText
                    )
                }
            }
        }
    }
}

private fun formatTime(timeMillis: Long): String {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(timeMillis))
}







