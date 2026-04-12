package com.th3cavalry.androidllm.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the AppError error taxonomy.
 */
class ErrorsTest {

    @Test
    fun `NetworkError should have correct category`() {
        val error = NetworkError("Connection failed", 500)
        assertEquals(ErrorCategory.NETWORK, error.category)
        assertEquals("Connection failed", error.message)
        assertEquals(500, error.statusCode)
    }

    @Test
    fun `TimeoutError should have correct category`() {
        val error = TimeoutError("Request timed out", 30000)
        assertEquals(ErrorCategory.NETWORK, error.category)
        assertEquals("Request timed out", error.message)
        assertEquals(30000, error.timeoutMs)
    }

    @Test
    fun `AuthError should have correct category`() {
        val error = AuthError("Invalid token", "Bearer")
        assertEquals(ErrorCategory.AUTH, error.category)
        assertEquals("Invalid token", error.message)
        assertEquals("Bearer", error.authType)
    }

    @Test
    fun `ParsingError should have correct category`() {
        val error = ParsingError("Invalid JSON", "XML", "<root>data</root>")
        assertEquals(ErrorCategory.PARSING, error.category)
        assertEquals("Invalid JSON", error.message)
        assertEquals("XML", error.sourceType)
        assertEquals("<root>data</root>", error.rawData)
    }

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
    fun `UnknownError should include cause`() {
        val cause = RuntimeException("Original error")
        val error = UnknownError("Wrapped error", cause)
        assertEquals(ErrorCategory.UNKNOWN, error.category)
        assertEquals("Wrapped error", error.message)
        assertEquals(cause, error.cause)
    }
}
