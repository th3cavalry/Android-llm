package com.th3cavalry.androidllm.data

/**
 * Curated registry of popular MCP servers that users can browse and add.
 */
object MCPServerRegistry {

    data class ServerTemplate(
        val name: String,
        val url: String,
        val description: String,
        val category: String
    )

    val templates: List<ServerTemplate> = listOf(
        // Developer Tools
        ServerTemplate(
            name = "GitHub",
            url = "https://mcp-github.example.com/sse",
            description = "Manage repos, issues, pull requests, and code search",
            category = "Developer Tools"
        ),
        ServerTemplate(
            name = "GitLab",
            url = "https://mcp-gitlab.example.com/sse",
            description = "GitLab project management, merge requests, CI/CD",
            category = "Developer Tools"
        ),
        ServerTemplate(
            name = "Linear",
            url = "https://mcp-linear.example.com/sse",
            description = "Issue tracking, project management, and team workflows",
            category = "Developer Tools"
        ),

        // Search & Knowledge
        ServerTemplate(
            name = "Brave Search",
            url = "https://mcp-brave-search.example.com/sse",
            description = "Privacy-focused web search powered by Brave",
            category = "Search & Knowledge"
        ),
        ServerTemplate(
            name = "Wikipedia",
            url = "https://mcp-wikipedia.example.com/sse",
            description = "Search and read Wikipedia articles",
            category = "Search & Knowledge"
        ),

        // File & Storage
        ServerTemplate(
            name = "Filesystem",
            url = "http://localhost:3000/sse",
            description = "Read, write, and manage local files (requires local server)",
            category = "File & Storage"
        ),
        ServerTemplate(
            name = "Google Drive",
            url = "https://mcp-gdrive.example.com/sse",
            description = "Search, read, and manage Google Drive files",
            category = "File & Storage"
        ),

        // Communication
        ServerTemplate(
            name = "Slack",
            url = "https://mcp-slack.example.com/sse",
            description = "Send messages, search channels, manage Slack workspace",
            category = "Communication"
        ),
        ServerTemplate(
            name = "Email (IMAP)",
            url = "https://mcp-email.example.com/sse",
            description = "Read, search, and send emails via IMAP/SMTP",
            category = "Communication"
        ),

        // Databases
        ServerTemplate(
            name = "PostgreSQL",
            url = "http://localhost:3001/sse",
            description = "Query and manage PostgreSQL databases (requires local server)",
            category = "Databases"
        ),
        ServerTemplate(
            name = "SQLite",
            url = "http://localhost:3002/sse",
            description = "Query local SQLite databases (requires local server)",
            category = "Databases"
        ),

        // Utilities
        ServerTemplate(
            name = "Fetch",
            url = "https://mcp-fetch.example.com/sse",
            description = "Fetch and parse web pages, APIs, and RSS feeds",
            category = "Utilities"
        ),
        ServerTemplate(
            name = "Time",
            url = "https://mcp-time.example.com/sse",
            description = "Get current time, timezone conversions, date calculations",
            category = "Utilities"
        )
    )

    /** Returns all unique categories in display order. */
    val categories: List<String>
        get() = templates.map { it.category }.distinct()

    /** Returns templates filtered by category. */
    fun byCategory(category: String): List<ServerTemplate> =
        templates.filter { it.category == category }
}
