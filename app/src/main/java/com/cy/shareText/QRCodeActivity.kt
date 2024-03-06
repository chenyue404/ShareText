package com.cy.shareText

import android.os.Build
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.ToastUtils
import com.chenyue404.androidlib.extends.bind
import com.chenyue404.androidlib.widget.BaseActivity
import com.king.zxing.util.CodeUtils

/**
 * Created by Eddie on 2020/4/18 0018.
 */
class QRCodeActivity : BaseActivity() {
    private val ivCode: ImageView by bind(R.id.iv_code)

    override fun getContentViewResId() = R.layout.activity_qrcode

    override fun initView() {
        ivCode.post {
            ivCode.setImageBitmap(
                CodeUtils.createQRCode(
                    WebServer.address,
                    ivCode.height,
                    ContextCompat.getColor(this, R.color.colorAccent)
                )
            )
        }
        ivCode.setOnClickListener {
            ToastUtils.showShort(
                WebServer.address
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ivCode.tooltipText = WebServer.address
        }
    }
}