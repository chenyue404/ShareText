package com.cy.shareText.web

import com.blankj.utilcode.util.CacheMemoryUtils
import com.blankj.utilcode.util.GsonUtils
import com.cy.shareText.MainActivity
import com.cy.shareText.NewTextEvent
import com.yanzhenjie.andserver.annotation.GetMapping
import com.yanzhenjie.andserver.annotation.PostMapping
import com.yanzhenjie.andserver.annotation.RequestParam
import com.yanzhenjie.andserver.annotation.RestController
import com.yanzhenjie.andserver.util.MediaType
import org.greenrobot.eventbus.EventBus

@RestController
class WebController {

    @GetMapping(path = ["/list"], produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun getListJson(): String {
        val list = CacheMemoryUtils.getInstance().get<ArrayList<String>>(MainActivity.KEY_CACHE)
        return if (list != null && list.size > 0) {
            GsonUtils.toJson(list)
        } else {
            ""
        }
    }

    @PostMapping("/addText")
    fun addText(@RequestParam("text") text: String) {
        var list = CacheMemoryUtils.getInstance().get<ArrayList<String>>(MainActivity.KEY_CACHE)
        if (list == null) {
            list = arrayListOf()
        }
        list.add(text)
        EventBus.getDefault().post(NewTextEvent(text))
    }
}