package com.cy.shareText

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ServerSocket

class PortConfigTest {

    @Test
    fun suggestAvailablePort_returnsNextPortWhenAvailable() {
        val used = setOf(3080, 3081)
        val result = PortConfig.suggestAvailablePort(3080) { it !in used }
        assertEquals(3082, result)
    }

    @Test
    fun suggestAvailablePort_wrapsToLowPortsWhenNeeded() {
        val result = PortConfig.suggestAvailablePort(65535) { it == 80 }
        assertEquals(80, result)
    }

    @Test
    fun suggestAvailablePort_returnsNullWhenNoPortAvailable() {
        val result = PortConfig.suggestAvailablePort(3080) { false }
        assertNull(result)
    }

    @Test
    fun suggestAvailablePort_canRecoverFromOccupiedPort() {
        ServerSocket(0).use { occupied ->
            val busyPort = occupied.localPort
            val suggestion = PortConfig.suggestAvailablePort(busyPort)

            assertNotEquals(busyPort, suggestion)
            assertTrue(suggestion != null && PortConfig.isPortAvailable(suggestion))
        }
    }
}

