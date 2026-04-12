package com.th3cavalry.androidllm.dto

import com.google.gson.annotations.SerializedName

/**
 * Typed DTOs for Web Search API responses.
 * Replaces regex-based parsing with type-safe data structures.
 */

// ─── Brave Search API DTOs ─────────────────────────────────────────────────

/** Single web search result from Brave. */
data class BraveWebResult(
    val title: String,
    val url: String,
    val description: String,
    @SerializedName("rich_info") val richInfo: Map<String, String>?
)

/** Web search section in Brave response. */
data class BraveWebSection(
    val total: Int,
    val offset: Int,
    val results: List<BraveWebResult>
)

/** Main response from Brave Search API. */
data class BraveSearchResponse(
    val web: BraveWebSection?
)

// ─── SerpAPI DTOs ──────────────────────────────────────────────────────────

/** Single organic search result from SerpAPI. */
data class SerpApiResult(
    val title: String,
    val link: String,
    val snippet: String,
    @SerializedName("position") val position: Int?
)

/** Main response from SerpAPI. */
data class SerpApiResponse(
    val organic_results: List<SerpApiResult>?
)

// ─── DuckDuckGo HTML DTOs ──────────────────────────────────────────────────

/** Single search result from DuckDuckGo HTML parsing. */
data class DuckDuckGoResult(
    val title: String,
    val url: String,
    val snippet: String
)

/** Parsed results from DuckDuckGo HTML. */
data class DuckDuckGoSearchResponse(
    val results: List<DuckDuckGoResult>,
    val hasMore: Boolean
)
