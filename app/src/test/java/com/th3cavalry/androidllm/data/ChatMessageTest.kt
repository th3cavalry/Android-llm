package com.th3cavalry.androidllm.data

import org.junit.Assert.*
import org.junit.Test

class ChatMessageTest {

    @Test
    fun `MessageRole values are correct`() {
        assertEquals("system", MessageRole.SYSTEM.value)
        assertEquals("user", MessageRole.USER.value)
        assertEquals("assistant", MessageRole.ASSISTANT.value)
        assertEquals("tool", MessageRole.TOOL.value)
    }

    @Test
    fun `simple user message`() {
        val msg = ChatMessage(role = MessageRole.USER, content = "Hello")
        assertEquals(MessageRole.USER, msg.role)
        assertEquals("Hello", msg.content)
        assertNull(msg.toolCalls)
        assertNull(msg.toolCallId)
        assertNull(msg.responseInfo)
        assertNull(msg.errorInfo)
        assertFalse(msg.isStreaming)
    }

    @Test
    fun `assistant message with response info`() {
        val info = ResponseInfo(model = "gpt-4o", totalTokens = 150, durationMs = 2000)
        val msg = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "Response",
            responseInfo = info
        )
        assertEquals("gpt-4o", msg.responseInfo?.model)
        assertEquals(150, msg.responseInfo?.totalTokens)
        assertEquals(2000L, msg.responseInfo?.durationMs)
    }

    @Test
    fun `streaming message flag`() {
        val msg = ChatMessage(role = MessageRole.ASSISTANT, content = "partial...", isStreaming = true)
        assertTrue(msg.isStreaming)
    }

    @Test
    fun `error info with all fields`() {
        val errorInfo = ErrorInfo(
            message = "API Error",
            details = "Rate limited",
            category = ErrorCategory.NETWORK,
            isRetryable = true,
            originalMessage = "Tell me a joke"
        )
        val msg = ChatMessage(role = MessageRole.ASSISTANT, content = null, errorInfo = errorInfo)
        assertNull(msg.content)
        assertNotNull(msg.errorInfo)
        assertTrue(msg.errorInfo!!.isRetryable)
        assertEquals(ErrorCategory.NETWORK, msg.errorInfo!!.category)
        assertEquals("Tell me a joke", msg.errorInfo!!.originalMessage)
    }

    @Test
    fun `error info defaults`() {
        val errorInfo = ErrorInfo(message = "Something went wrong")
        assertNull(errorInfo.details)
        assertNull(errorInfo.category)
        assertFalse(errorInfo.isRetryable)
        assertNull(errorInfo.originalMessage)
    }

    @Test
    fun `executing info defaults`() {
        val info = ExecutingInfo(toolName = "web_search")
        assertEquals("web_search", info.toolName)
        assertNull(info.status)
        assertTrue(info.startTimeMs > 0)
    }

    @Test
    fun `response info with null tokens`() {
        val info = ResponseInfo(model = "local-model", totalTokens = null, durationMs = 500)
        assertNull(info.totalTokens)
    }

    @Test
    fun `tool message with tool call id`() {
        val msg = ChatMessage(
            role = MessageRole.TOOL,
            content = "{\"result\": \"success\"}",
            toolCallId = "call_abc123",
            toolName = "web_search"
        )
        assertEquals(MessageRole.TOOL, msg.role)
        assertEquals("call_abc123", msg.toolCallId)
        assertEquals("web_search", msg.toolName)
    }

    @Test
    fun `ChatSession basic creation`() {
        val messages = listOf(
            ChatMessage(role = MessageRole.USER, content = "Hello"),
            ChatMessage(role = MessageRole.ASSISTANT, content = "Hi there!")
        )
        val session = ChatSession(
            id = 1000L,
            title = "Hello",
            timestamp = 1000L,
            messages = messages
        )
        assertEquals(1000L, session.id)
        assertEquals("Hello", session.title)
        assertEquals(2, session.messages.size)
    }
}
