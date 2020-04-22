package com.cy.shareText.web

import com.yanzhenjie.andserver.annotation.Controller
import com.yanzhenjie.andserver.annotation.GetMapping

@Controller
class PageController {

    @GetMapping(path = ["/"])
    fun index(): String? {
        // Equivalent to [return "/index"].
        return "forward:MdHome.html"
    }
}