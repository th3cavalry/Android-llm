package com.th3cavalry.androidllm.data

/**
 * Represents an MCP (Model Context Protocol) server configuration.
 */
data class MCPServer(
    val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean = true,
    val description: String = "",
    /** Last known connection status: null = never tested, true = ok, false = failed. */
    val lastStatus: Boolean? = null,
    /** Epoch millis of the last connection test, or 0 if never tested. */
    val lastTestedAt: Long = 0,
    /** Number of tools discovered on last successful test. */
    val toolCount: Int = 0,
    /** Error message from the last failed test, if any. */
    val lastError: String? = null
)
