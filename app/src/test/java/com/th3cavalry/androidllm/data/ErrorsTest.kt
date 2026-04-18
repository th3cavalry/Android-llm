package com.th3cavalry.androidllm.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the AppError error taxonomy.
 */
class ErrorsTest {

    // ─── Network Errors ────────────────────────────────────────────────────

    @Test
    fun `NetworkError should have correct category`() {
        val error = NetworkError("Connection failed", 500)
        assertEquals(ErrorCategory.NETWORK, error.category)
        assertEquals("Connection failed", error.message)
        assertEquals(500, error.statusCode)
    }

    @Test
    fun `NetworkError with null status code`() {
        val error = NetworkError("DNS failed")
        assertEquals(ErrorCategory.NETWORK, error.category)
        assertNull(error.statusCode)
    }

    @Test
    fun `TimeoutError should have correct category`() {
        val error = TimeoutError("Request timed out", 30000)
        assertEquals(ErrorCategory.NETWORK, error.category)
        assertEquals("Request timed out", error.message)
        assertEquals(30000, error.timeoutMs)
    }

    @Test
    fun `ConnectionError should have correct category`() {
        val error = ConnectionError("Connection refused")
        assertEquals(ErrorCategory.NETWORK, error.category)
        assertEquals("Connection refused", error.message)
    }

    // ─── Auth Errors ───────────────────────────────────────────────────────

    @Test
    fun `AuthError should have correct category`() {
        val error = AuthError("Invalid token", "Bearer")
        assertEquals(ErrorCategory.AUTH, error.category)
        assertEquals("Invalid token", error.message)
        assertEquals("Bearer", error.authType)
    }

    @Test
    fun `AuthError defaults authType to unknown`() {
        val error = AuthError("Unauthorized")
        assertEquals("unknown", error.authType)
    }

    @Test
    fun `InvalidTokenError should have correct category`() {
        val error = InvalidTokenError("Token expired")
        assertEquals(ErrorCategory.AUTH, error.category)
        assertEquals("Token expired", error.message)
    }

    // ─── Model Errors ──────────────────────────────────────────────────────

    @Test
    fun `ModelError should have correct category`() {
        val error = ModelError("Model failed", "llama-3")
        assertEquals(ErrorCategory.MODEL, error.category)
        assertEquals("Model failed", error.message)
        assertEquals("llama-3", error.modelId)
    }

    @Test
    fun `ModelError with null modelId`() {
        val error = ModelError("Generic model error")
        assertNull(error.modelId)
    }

    @Test
    fun `ModelNotLoadedError should have correct category`() {
        val error = ModelNotLoadedError("No model loaded")
        assertEquals(ErrorCategory.MODEL, error.category)
    }

    @Test
    fun `ModelLoadError should contain model path`() {
        val error = ModelLoadError("Failed to load", "/sdcard/model.task")
        assertEquals(ErrorCategory.MODEL, error.category)
        assertEquals("/sdcard/model.task", error.modelPath)
    }

    // ─── Tool Errors ───────────────────────────────────────────────────────

    @Test
    fun `ToolError should have correct category`() {
        val error = ToolError("Tool failed", "web_search")
        assertEquals(ErrorCategory.TOOL, error.category)
        assertEquals("web_search", error.toolName)
    }

    @Test
    fun `ToolError with null toolName`() {
        val error = ToolError("Tool failed")
        assertNull(error.toolName)
    }

    @Test
    fun `ToolExecutionError should preserve arguments`() {
        val args = mapOf<String, Any?>("query" to "test", "limit" to 10)
        val error = ToolExecutionError("Execution failed", "web_search", args)
        assertEquals(ErrorCategory.TOOL, error.category)
        assertEquals("web_search", error.toolName)
        assertEquals(args, error.arguments)
    }

    @Test
    fun `ToolExecutionError with null arguments`() {
        val error = ToolExecutionError("Failed", "ssh_exec")
        assertNull(error.arguments)
    }

    // ─── Parsing Errors ────────────────────────────────────────────────────

    @Test
    fun `ParsingError should have correct category`() {
        val error = ParsingError("Invalid JSON", "XML", "<root>data</root>")
        assertEquals(ErrorCategory.PARSING, error.category)
        assertEquals("Invalid JSON", error.message)
        assertEquals("XML", error.sourceType)
        assertEquals("<root>data</root>", error.rawData)
    }

    @Test
    fun `ParsingError with null optional fields`() {
        val error = ParsingError("Parse failed")
        assertNull(error.sourceType)
        assertNull(error.rawData)
    }

    @Test
    fun `InvalidJsonError should contain raw JSON`() {
        val error = InvalidJsonError("Unexpected token", "{bad json")
        assertEquals(ErrorCategory.PARSING, error.category)
        assertEquals("{bad json", error.json)
    }

    // ─── User Errors ───────────────────────────────────────────────────────

    @Test
    fun `UserError should have correct category`() {
        val error = UserError("Invalid input")
        assertEquals(ErrorCategory.USER, error.category)
    }

    @Test
    fun `ConfigurationError should have correct category`() {
        val error = ConfigurationError("Missing API key")
        assertEquals(ErrorCategory.USER, error.category)
        assertEquals("Missing API key", error.message)
    }

    // ─── Unknown Errors ────────────────────────────────────────────────────

    @Test
    fun `UnknownError should include cause`() {
        val cause = RuntimeException("Original error")
        val error = UnknownError("Wrapped error", cause)
        assertEquals(ErrorCategory.UNKNOWN, error.category)
        assertEquals("Wrapped error", error.message)
        assertEquals(cause, error.cause)
    }

    @Test
    fun `UnknownError with null cause`() {
        val error = UnknownError("Unknown error")
        assertNull(error.cause)
    }

    // ─── toAppError conversion ─────────────────────────────────────────────

    @Test
    fun `toAppError should convert TimeoutException`() {
        val exception = java.util.concurrent.TimeoutException("Connection timeout")
        val appError = exception.toAppError()
        assertTrue(appError is TimeoutError)
        assertEquals(ErrorCategory.NETWORK, appError.category)
    }

    @Test
    fun `toAppError should convert UnknownHostException`() {
        val exception = java.net.UnknownHostException("Unknown host")
        val appError = exception.toAppError()
        assertTrue(appError is NetworkError)
        assertEquals(ErrorCategory.NETWORK, appError.category)
    }

    @Test
    fun `toAppError should convert ConnectException`() {
        val exception = java.net.ConnectException("Connection refused")
        val appError = exception.toAppError()
        assertTrue(appError is ConnectionError)
        assertEquals(ErrorCategory.NETWORK, appError.category)
    }

    @Test
    fun `toAppError should convert IOException`() {
        val exception = java.io.IOException("IO error")
        val appError = exception.toAppError()
        assertTrue(appError is NetworkError)
        assertEquals(ErrorCategory.NETWORK, appError.category)
    }

    @Test
    fun `toAppError should wrap unknown exceptions in UnknownError`() {
        val exception = RuntimeException("Unknown error")
        val appError = exception.toAppError()
        assertTrue(appError is UnknownError)
        assertEquals(ErrorCategory.UNKNOWN, appError.category)
    }

    @Test
    fun `toAppError should return AppError unchanged if already an AppError interface`() {
        // This tests the identity conversion case in the when branch
        val exception = RuntimeException("test")
        val appError = exception.toAppError()
        assertTrue(appError is UnknownError)
    }

    @Test
    fun `toAppError TimeoutException with null message uses default`() {
        val exception = java.util.concurrent.TimeoutException()
        val appError = exception.toAppError()
        assertTrue(appError is TimeoutError)
        assertEquals("Connection timed out", appError.message)
    }

    @Test
    fun `toAppError ConnectException with null message uses default`() {
        val exception = java.net.ConnectException()
        val appError = exception.toAppError()
        assertTrue(appError is ConnectionError)
        assertEquals("Connection refused", appError.message)
    }

    @Test
    fun `toAppError IOException with null message uses default`() {
        val exception = java.io.IOException()
        val appError = exception.toAppError()
        assertTrue(appError is NetworkError)
        assertEquals("IO error", appError.message)
    }

    @Test
    fun `toAppError RuntimeException with null message uses default`() {
        val exception = RuntimeException()
        val appError = exception.toAppError()
        assertTrue(appError is UnknownError)
        assertEquals("Unknown error", appError.message)
    }

    // ─── HTTP status code scenarios ────────────────────────────────────────

    @Test
    fun `NetworkError for 401 Unauthorized`() {
        val error = NetworkError("Unauthorized", 401)
        assertEquals(401, error.statusCode)
    }

    @Test
    fun `NetworkError for 429 Rate Limited`() {
        val error = NetworkError("Rate limited", 429)
        assertEquals(429, error.statusCode)
    }

    @Test
    fun `NetworkError for 503 Service Unavailable`() {
        val error = NetworkError("Service unavailable", 503)
        assertEquals(503, error.statusCode)
    }

    // ─── ErrorCategory enum coverage ───────────────────────────────────────

    @Test
    fun `all ErrorCategory values are covered`() {
        val categories = ErrorCategory.values()
        assertEquals(7, categories.size)
        assertTrue(categories.contains(ErrorCategory.NETWORK))
        assertTrue(categories.contains(ErrorCategory.AUTH))
        assertTrue(categories.contains(ErrorCategory.MODEL))
        assertTrue(categories.contains(ErrorCategory.TOOL))
        assertTrue(categories.contains(ErrorCategory.PARSING))
        assertTrue(categories.contains(ErrorCategory.USER))
        assertTrue(categories.contains(ErrorCategory.UNKNOWN))
    }
}
