package com.th3cavalry.androidllm

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.th3cavalry.androidllm.data.MCPServer
import com.th3cavalry.androidllm.data.MCPServerRegistry
import com.th3cavalry.androidllm.databinding.ActivityMcpManagerBinding
import com.th3cavalry.androidllm.service.MCPClient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MCPManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMcpManagerBinding
    private val servers: MutableList<MCPServer> = mutableListOf()
    private lateinit var serverAdapter: MCPServerAdapter
    private var appliedThemeIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        appliedThemeIndex = ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMcpManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        servers.addAll(Prefs.getMCPServers(this))

        serverAdapter = MCPServerAdapter(servers,
            onDelete = { pos -> deleteServer(pos) },
            onToggle = { pos, enabled -> toggleServer(pos, enabled) },
            onTest = { pos -> testServer(pos) }
        )

        binding.rvServers.apply {
            layoutManager = LinearLayoutManager(this@MCPManagerActivity)
            adapter = serverAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        binding.fab.setOnClickListener { showAddServerDialog() }
        binding.btnBrowse.setOnClickListener { showBrowseDialog() }

        updateEmptyState()
    }

    private fun showAddServerDialog(
        prefillName: String = "",
        prefillUrl: String = "",
        prefillDesc: String = ""
    ) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_mcp_server, null)
        val etName = view.findViewById<EditText>(R.id.etServerName)
        val etUrl = view.findViewById<EditText>(R.id.etServerUrl)
        val etDesc = view.findViewById<EditText>(R.id.etServerDescription)

        etName.setText(prefillName)
        etUrl.setText(prefillUrl)
        etDesc.setText(prefillDesc)

        AlertDialog.Builder(this)
            .setTitle(R.string.add_mcp_server)
            .setView(view)
            .setPositiveButton(R.string.add) { _, _ ->
                val name = etName.text.toString().trim()
                val url = etUrl.text.toString().trim()
                val desc = etDesc.text.toString().trim()
                if (name.isNotEmpty() && url.isNotEmpty()) {
                    addServer(MCPServer(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        url = url,
                        description = desc
                    ))
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun addServer(server: MCPServer) {
        servers.add(server)
        serverAdapter.notifyItemInserted(servers.size - 1)
        saveServers()
        updateEmptyState()
    }

    private fun deleteServer(position: Int) {
        if (position < 0 || position >= servers.size) return
        servers.removeAt(position)
        serverAdapter.notifyItemRemoved(position)
        saveServers()
        updateEmptyState()
    }

    private fun toggleServer(position: Int, enabled: Boolean) {
        if (position < 0 || position >= servers.size) return
        servers[position] = servers[position].copy(enabled = enabled)
        saveServers()
    }

    private fun testServer(position: Int) {
        val server = servers.getOrNull(position) ?: return
        lifecycleScope.launch {
            Snackbar.make(binding.root, "Testing ${server.name}…", Snackbar.LENGTH_SHORT).show()
            try {
                val client = MCPClient(server)
                val ok = client.initialize()
                val tools = if (ok) client.listTools() else emptyList()
                val now = System.currentTimeMillis()
                if (ok) {
                    servers[position] = server.copy(
                        lastStatus = true,
                        lastTestedAt = now,
                        toolCount = tools.size,
                        lastError = null
                    )
                    Snackbar.make(binding.root, "✅ Connected! Found ${tools.size} tool(s).", Snackbar.LENGTH_LONG).show()
                } else {
                    servers[position] = server.copy(
                        lastStatus = false,
                        lastTestedAt = now,
                        lastError = "Connection failed"
                    )
                    Snackbar.make(binding.root, "❌ Failed to connect to ${server.name}", Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                val errorMsg = parseMCPError(e)
                servers[position] = server.copy(
                    lastStatus = false,
                    lastTestedAt = System.currentTimeMillis(),
                    lastError = errorMsg
                )
                Snackbar.make(binding.root, "❌ $errorMsg", Snackbar.LENGTH_LONG).show()
            }
            serverAdapter.notifyItemChanged(position)
            saveServers()
        }
    }

    /**
     * Parses MCP server errors into user-friendly messages with hints.
     */
    private fun parseMCPError(e: Exception): String {
        val msg = e.message ?: "Unknown error"
        return when {
            msg.contains("UnknownHostException", ignoreCase = true) ||
            msg.contains("Unable to resolve host", ignoreCase = true) ->
                "Server not found — check the URL or your network connection"
            msg.contains("ConnectException", ignoreCase = true) ||
            msg.contains("Connection refused", ignoreCase = true) ->
                "Connection refused — is the server running?"
            msg.contains("SocketTimeoutException", ignoreCase = true) ||
            msg.contains("timeout", ignoreCase = true) ->
                "Connection timed out — the server may be overloaded or unreachable"
            msg.contains("SSLHandshakeException", ignoreCase = true) ||
            msg.contains("SSL", ignoreCase = true) ->
                "SSL/TLS error — check if the server uses HTTPS and has a valid certificate"
            msg.contains("401") || msg.contains("Unauthorized", ignoreCase = true) ->
                "Authentication required — the server needs an API key or token"
            msg.contains("403") || msg.contains("Forbidden", ignoreCase = true) ->
                "Access denied — you don't have permission to access this server"
            msg.contains("404") || msg.contains("Not Found", ignoreCase = true) ->
                "Endpoint not found — check that the URL path is correct"
            msg.contains("500") || msg.contains("Internal Server Error", ignoreCase = true) ->
                "Server error — the MCP server encountered an internal problem"
            else -> "Error: $msg"
        }
    }

    /**
     * Shows a dialog with curated popular MCP servers grouped by category.
     */
    private fun showBrowseDialog() {
        val categories = MCPServerRegistry.categories
        val items = mutableListOf<String>()
        val templates = mutableListOf<MCPServerRegistry.ServerTemplate>()

        for (category in categories) {
            items.add("— $category —")
            templates.add(MCPServerRegistry.ServerTemplate("", "", "", category)) // placeholder
            for (t in MCPServerRegistry.byCategory(category)) {
                items.add("${t.name}\n${t.description}")
                templates.add(t)
            }
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.browse_servers_title)
            .setItems(items.toTypedArray()) { _, which ->
                val template = templates[which]
                if (template.url.isEmpty()) return@setItems // category header, ignore

                // Check if already configured
                if (servers.any { it.url == template.url }) {
                    Snackbar.make(binding.root, R.string.server_already_exists, Snackbar.LENGTH_SHORT).show()
                    return@setItems
                }

                // Show add dialog pre-filled with template values
                showAddServerDialog(template.name, template.url, template.description)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun saveServers() = Prefs.saveMCPServers(this, servers)

    private fun updateEmptyState() {
        binding.tvEmpty.visibility = if (servers.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        ThemeHelper.recreateIfNeeded(this, appliedThemeIndex)
    }

    // ─── Inner Adapter ────────────────────────────────────────────────────────

    class MCPServerAdapter(
        private val items: List<MCPServer>,
        private val onDelete: (Int) -> Unit,
        private val onToggle: (Int, Boolean) -> Unit,
        private val onTest: (Int) -> Unit
    ) : RecyclerView.Adapter<MCPServerAdapter.VH>() {

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_mcp_server, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val server = items[position]
            val tvName = holder.itemView.findViewById<TextView>(R.id.tvServerName)
            val tvUrl = holder.itemView.findViewById<TextView>(R.id.tvServerUrl)
            val tvDesc = holder.itemView.findViewById<TextView>(R.id.tvServerDesc)
            val tvStatus = holder.itemView.findViewById<TextView>(R.id.tvServerStatus)
            val viewStatus = holder.itemView.findViewById<View>(R.id.viewStatus)
            val switchEnabled = holder.itemView.findViewById<SwitchCompat>(R.id.switchEnabled)
            val btnDelete = holder.itemView.findViewById<ImageButton>(R.id.btnDelete)
            val btnTest = holder.itemView.findViewById<MaterialButton>(R.id.btnTest)

            tvName.text = server.name
            tvUrl.text = server.url
            tvDesc.text = server.description
            tvDesc.visibility = if (server.description.isBlank()) View.GONE else View.VISIBLE

            // Health status indicator
            if (server.lastStatus != null) {
                viewStatus.visibility = View.VISIBLE
                val color = if (server.lastStatus) 0xFF4CAF50.toInt() else 0xFFF44336.toInt()
                (viewStatus.background as? GradientDrawable)?.setColor(color)

                tvStatus.visibility = View.VISIBLE
                val dateStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                    .format(Date(server.lastTestedAt))
                tvStatus.text = if (server.lastStatus) {
                    "✅ ${server.toolCount} tool(s) • $dateStr"
                } else {
                    "❌ ${server.lastError ?: "Failed"} • $dateStr"
                }
            } else {
                viewStatus.visibility = View.GONE
                tvStatus.visibility = View.GONE
            }

            switchEnabled.isChecked = server.enabled
            switchEnabled.setOnCheckedChangeListener { _, checked ->
                onToggle(holder.adapterPosition, checked)
            }

            btnDelete.setOnClickListener { onDelete(holder.adapterPosition) }
            btnTest.setOnClickListener { onTest(holder.adapterPosition) }
        }

        override fun getItemCount() = items.size
    }
}
