package com.th3cavalry.androidllm.data

import org.junit.Assert.*
import org.junit.Test

class MCPServerRegistryTest {

    @Test
    fun `templates list is not empty`() {
        assertTrue(MCPServerRegistry.templates.isNotEmpty())
    }

    @Test
    fun `all templates have non-blank fields`() {
        MCPServerRegistry.templates.forEach { t ->
            assertTrue("name blank for $t", t.name.isNotBlank())
            assertTrue("url blank for ${t.name}", t.url.isNotBlank())
            assertTrue("description blank for ${t.name}", t.description.isNotBlank())
            assertTrue("category blank for ${t.name}", t.category.isNotBlank())
        }
    }

    @Test
    fun `categories returns distinct values`() {
        val categories = MCPServerRegistry.categories
        assertEquals(categories.size, categories.distinct().size)
    }

    @Test
    fun `categories covers all templates`() {
        val categories = MCPServerRegistry.categories
        MCPServerRegistry.templates.forEach { t ->
            assertTrue("Category '${t.category}' not in categories list", categories.contains(t.category))
        }
    }

    @Test
    fun `byCategory returns only matching templates`() {
        MCPServerRegistry.categories.forEach { cat ->
            val filtered = MCPServerRegistry.byCategory(cat)
            assertTrue(filtered.isNotEmpty())
            filtered.forEach { t ->
                assertEquals(cat, t.category)
            }
        }
    }

    @Test
    fun `byCategory with unknown category returns empty`() {
        val result = MCPServerRegistry.byCategory("NonexistentCategory")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `expected categories are present`() {
        val categories = MCPServerRegistry.categories
        assertTrue(categories.contains("Developer Tools"))
        assertTrue(categories.contains("Search & Knowledge"))
        assertTrue(categories.contains("File & Storage"))
    }
}
