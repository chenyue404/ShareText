package com.cy.shareText

import android.content.Context
import com.yanzhenjie.andserver.annotation.Config
import com.yanzhenjie.andserver.framework.config.Multipart
import com.yanzhenjie.andserver.framework.config.WebConfig
import com.yanzhenjie.andserver.framework.website.AssetsWebsite
import java.io.File

@Config
class InternalWebsite : WebConfig {
    override fun onConfig(context: Context?, delegate: WebConfig.Delegate?) {
        delegate!!.addWebsite(AssetsWebsite(context!!, "/web"))

        delegate.setMultipart(
            Multipart.newBuilder()
                .allFileMaxSize(1024 * 1024 * 20.toLong()) // 20M
                .fileMaxSize(1024 * 1024 * 5.toLong()) // 5M
                .maxInMemorySize(1024 * 10) // 1024 * 10 bytes
                .uploadTempDir(File(context.cacheDir, "_server_upload_cache_"))
                .build()
        )
    }
}