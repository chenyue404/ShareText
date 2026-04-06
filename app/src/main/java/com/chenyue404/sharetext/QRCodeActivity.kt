package com.chenyue404.sharetext

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

class QRCodeActivity : ComponentActivity() {
    private var address by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyStatusBarStyle()
        address = resolveAddress(intent)

        setContent {
            MaterialTheme {
                QRCodeScreen(address = address)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        address = resolveAddress(intent)
    }

    private fun resolveAddress(inIntent: android.content.Intent?): String {
        return inIntent?.getStringExtra(EXTRA_ADDRESS)?.trim().orEmpty().ifEmpty {
            "http://127.0.0.1:${WebService.getPort()}"
        }
    }

    companion object {
        const val EXTRA_ADDRESS = "extra_address"
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun QRCodeScreen(address: String) {
    val bitmap = buildQrBitmap(address, 880)
    val context = LocalContext.current

    Scaffold(
        containerColor = UiStyle.ScreenBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.qr_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = UiStyle.TopBarBackground
                ),
                navigationIcon = {
                    IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.desc_back),
                            tint = UiStyle.PrimaryAction
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(UiStyle.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(UiStyle.InnerSpacing)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = UiStyle.StatusCardBackground),
                shape = RoundedCornerShape(UiStyle.CornerRadius),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(UiStyle.CardPadding),
                    verticalArrangement = Arrangement.spacedBy(UiStyle.InnerSpacing)
                ) {
                    Text(
                        text = stringResource(R.string.qr_lan_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(text = address)
                    Text(
                        text = stringResource(R.string.qr_lan_hint),
                        color = UiStyle.SecondaryText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = UiStyle.ListCardBackground),
                shape = RoundedCornerShape(UiStyle.CornerRadius),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(UiStyle.CardPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(UiStyle.InnerSpacing)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.qr_image_desc)
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { copyAddress(context, address) }
                    ) {
                        Text(stringResource(R.string.qr_copy_address))
                    }
                }
            }
        }
    }
}

private fun copyAddress(context: Context, address: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText("sharetext_address", address))
    Toast.makeText(context, context.getString(R.string.toast_address_copied), Toast.LENGTH_SHORT).show()
}

private fun buildQrBitmap(content: String, size: Int): Bitmap {
    val matrix: BitMatrix = MultiFormatWriter().encode(
        content,
        BarcodeFormat.QR_CODE,
        size,
        size
    )
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        for (x in 0 until size) {
            pixels[y * size + x] = if (matrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
    }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}
