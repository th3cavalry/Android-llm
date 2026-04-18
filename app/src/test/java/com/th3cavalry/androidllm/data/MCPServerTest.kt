package com.th3cavalry.androidllm.data

import org.junit.Assert.*
import org.junit.Test

class MCPServerTest {

    @Test
    fun `default values are correct`() {
        val server = MCPServer(id = "test", name = "Test", url = "http://localhost:3000/sse")
        assertTrue(server.enabled)
        assertEquals("", server.description)
        assertNull(server.lastStatus)
        assertEquals(0L, server.lastTestedAt)
        assertEquals(0, server.toolCount)
        assertNull(server.lastError)
    }

    @Test
    fun `healthy server state`() {
        val server = MCPServer(
            id = "github",
            name = "GitHub",
            url = "https://mcp-github.example.com/sse",
            lastStatus = true,
            lastTestedAt = System.currentTimeMillis(),
            toolCount = 5
        )
        assertTrue(server.lastStatus!!)
        assertTrue(server.toolCount > 0)
        assertNull(server.lastError)
    }

    @Test
    fun `failed server state`() {
        val server = MCPServer(
            id = "broken",
            name = "Broken",
            url = "https://broken.example.com/sse",
            lastStatus = false,
            lastError = "Connection refused"
        )
        assertFalse(server.lastStatus!!)
        assertEquals("Connection refused", server.lastError)
    }

    @Test
    fun `disabled server`() {
        val server = MCPServer(
            id = "disabled",
            name = "Disabled Server",
            url = "http://localhost:3000/sse",
            enabled = false
        )
        assertFalse(server.enabled)
    }

    @Test
    fun `copy with updated status`() {
        val server = MCPServer(id = "s1", name = "Server", url = "http://localhost/sse")
        val updated = server.copy(lastStatus = true, toolCount = 3)
        assertNull(server.lastStatus)
        assertTrue(updated.lastStatus!!)
        assertEquals(3, updated.toolCount)
    }
}
