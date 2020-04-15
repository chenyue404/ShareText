package com.cy.shareText

import com.blankj.utilcode.util.CacheMemoryUtils
import com.blankj.utilcode.util.GsonUtils
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
        CacheMemoryUtils.getInstance().put(MainActivity.KEY_CACHE, list)
        EventBus.getDefault().post(NewTextEvent(text))
    }
}