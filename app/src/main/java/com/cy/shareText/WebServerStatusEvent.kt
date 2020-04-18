package com.cy.shareText

class WebServerStatusEvent(status: Int) {

    companion object {
        val STATUS_STOP = 0
        val STATUS_START = 1
        val STATUS_ERROR = 2
    }

    var status: Int = STATUS_STOP
    var errorMsg: String = ""

    init {
        this.status = status
    }
}