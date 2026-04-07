package com.cy.shareText

import org.junit.Assert.assertEquals
import org.junit.Test

class PortAdviceTest {

    @Test
    fun occupiedMessage_returnsSuggestionWhenPresent() {
        val message = PortAdvice.occupiedMessage(3080) { 3088 }
        assertEquals("端口被占用，建议改为 3088", message)
    }

    @Test
    fun occupiedMessage_returnsFallbackWhenNoSuggestion() {
        val message = PortAdvice.occupiedMessage(3080) { null }
        assertEquals("端口被占用，请换一个端口", message)
    }

    @Test
    fun saveFailedOccupiedMessage_wrapsOccupiedMessage() {
        val message = PortAdvice.saveFailedOccupiedMessage(3080) { 3088 }
        assertEquals("保存失败：端口被占用，建议改为 3088", message)
    }

    @Test
    fun saveSuccessMessage_returnsExpectedText() {
        assertEquals("端口已保存并重启服务：3088", PortAdvice.saveSuccessMessage(3088))
    }

    @Test
    fun portChangedMessage_returnsExpectedText() {
        assertEquals("服务端口已切换到：3088", PortAdvice.portChangedMessage(3088))
    }
}

