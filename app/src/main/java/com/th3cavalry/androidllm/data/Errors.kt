package com.th3cavalry.androidllm.data

/**
 * Error taxonomy for the Android LLM app.
 * Categories: Network, Auth, Model, Tool, Parsing, User, Unknown
 */
sealed interface AppError {
    val message: String
    val category: ErrorCategory
}

enum class ErrorCategory {
    NETWORK,
    AUTH,
    MODEL,
    TOOL,
    PARSING,
    USER,
    UNKNOWN
}

// ─── Network Errors ────────────────────────────────────────────────────────────

data class NetworkError(
    override val message: String,
    val statusCode: Int? = null
) : AppError {
    override val category: ErrorCategory = ErrorCategory.NETWORK
}

data class TimeoutError(
    override val message: String,
    val timeoutMs: Long
) : AppError {
    override val category: ErrorCategory = ErrorCategory.NETWORK
}

data class ConnectionError(
    override val message: String
) : AppError {
    override val category: ErrorCategory = ErrorCategory.NETWORK
}

// ─── Auth Errors ───────────────────────────────────────────────────────────────

data class AuthError(
    override val message: String,
    val authType: String = "unknown"
) : AppError {
    override val category: ErrorCategory = ErrorCategory.AUTH
}

data class InvalidTokenError(
    override val message: String
) : AppError {
    override val category: ErrorCategory = ErrorCategory.AUTH
}

// ─── Model Errors ──────────────────────────────────────────────────────────────

data class ModelError(
    override val message: String,
    val modelId: String? = null
) : AppError {
    override val category: ErrorCategory = ErrorCategory.MODEL
}

data class ModelNotLoadedError(
    override val message: String
) : AppError {
    override val category: ErrorCategory = ErrorCategory.MODEL
}

data class ModelLoadError(
    override val message: String,
    val modelPath: String
) : AppError {
    override val category: ErrorCategory = ErrorCategory.MODEL
}

// ─── Tool Errors ───────────────────────────────────────────────────────────────

data class ToolError(
    override val message: String,
    val toolName: String? = null
) : AppError {
    override val category: ErrorCategory = ErrorCategory.TOOL
}

data class ToolExecutionError(
    override val message: String,
    val toolName: String,
    val arguments: Map<String, Any?>? = null
) : AppError {
    override val category: ErrorCategory = ErrorCategory.TOOL
}

// ─── Parsing Errors ────────────────────────────────────────────────────────────

data class ParsingError(
    override val message: String,
    val sourceType: String? = null,
    val rawData: String? = null
) : AppError {
    override val category: ErrorCategory = ErrorCategory.PARSING
}

data class InvalidJsonError(
    override val message: String,
    val json: String
) : AppError {
    override val category: ErrorCategory = ErrorCategory.PARSING
}

// ─── User Errors ───────────────────────────────────────────────────────────────

data class UserError(
    override val message: String
) : AppError {
    override val category: ErrorCategory = ErrorCategory.USER
}

data class ConfigurationError(
    override val message: String
) : AppError {
    override val category: ErrorCategory = ErrorCategory.USER
}

// ─── Unknown Errors ────────────────────────────────────────────────────────────

data class UnknownError(
    override val message: String,
    val cause: Throwable? = null
) : AppError {
    override val category: ErrorCategory = ErrorCategory.UNKNOWN
}

// ─── Error Utilities ───────────────────────────────────────────────────────────

fun Throwable.toAppError(): AppError = when (this) {
    is AppError -> this
    is java.util.concurrent.TimeoutException -> TimeoutError(message ?: "Connection timed out", 0)
    is java.net.UnknownHostException -> NetworkError(message ?: "Unknown host", null)
    is java.net.ConnectException -> ConnectionError(message ?: "Connection refused")
    is java.io.IOException -> NetworkError(message ?: "IO error", null)
    else -> UnknownError(message ?: "Unknown error", this)
}
