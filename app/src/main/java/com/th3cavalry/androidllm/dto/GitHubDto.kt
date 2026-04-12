package com.th3cavalry.androidllm.dto

import com.google.gson.annotations.SerializedName

/**
 * Typed DTOs for GitHub REST API v3 responses.
 * Replaces regex-based parsing with type-safe data structures.
 */

// ─── Contents API DTOs ─────────────────────────────────────────────────────

/** Single item in a directory listing response. */
data class GitHubContentsItem(
    val name: String,
    val path: String,
    val type: String,  // "file" or "dir"
    val size: Long,
    @SerializedName("encoding") val encoding: String?,
    val content: String?,
    val sha: String
)

/** Response for reading a single file. */
data class GitHubFileResponse(
    val name: String,
    val path: String,
    val type: String,
    val size: Long,
    @SerializedName("encoding") val encoding: String,
    val content: String,
    val sha: String
)

// ─── Search API DTOs ───────────────────────────────────────────────────────

/** Single search result from GitHub search API. */
data class GitHubSearchResult(
    val name: String,
    val path: String,
    val sha: String,
    val url: String,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("git_url") val gitUrl: String,
    val type: String,
    @SerializedName("score") val score: Double
)

/** Response for GitHub search API. */
data class GitHubSearchResponse(
    val totalCount: Int,
    @SerializedName("incomplete_results") val incompleteResults: Boolean,
    val items: List<GitHubSearchResult>
)
