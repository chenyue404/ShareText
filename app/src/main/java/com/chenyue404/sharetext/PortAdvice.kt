package com.chenyue404.sharetext

object PortAdvice {

    fun occupiedMessage(
        port: Int,
        suggest: (Int) -> Int? = PortConfig::suggestAvailablePort
    ): String {
        val suggestion = suggest(port)
        return if (suggestion != null) {
            "端口被占用，建议改为 $suggestion"
        } else {
            "端口被占用，请换一个端口"
        }
    }

    fun saveFailedOccupiedMessage(
        port: Int,
        suggest: (Int) -> Int? = PortConfig::suggestAvailablePort
    ): String {
        return "保存失败：${occupiedMessage(port, suggest)}"
    }

    fun saveSuccessMessage(port: Int): String {
        return "端口已保存并重启服务：$port"
    }

    fun portChangedMessage(port: Int): String {
        return "服务端口已切换到：$port"
    }
}
