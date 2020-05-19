package com.cy.shareText

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.ToastUtils
import com.king.zxing.util.CodeUtils
import kotlinx.android.synthetic.main.activity_qrcode.*

/**
 * Created by Eddie on 2020/4/18 0018.
 */
class QRCodeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qrcode)

        iv_code.post {
            iv_code.setImageBitmap(
                CodeUtils.createQRCode(
                    WebServer.address,
                    iv_code.height,
                    ContextCompat.getColor(this, R.color.colorAccent)
                )
            )
        }
        iv_code.setOnClickListener {
            ToastUtils.showShort(
                WebServer.address
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            iv_code.tooltipText = WebServer.address
        }
    }
}