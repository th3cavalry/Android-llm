package com.th3cavalry.androidllm

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.th3cavalry.androidllm.databinding.ActivityHelpBinding

class HelpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelpBinding
    private var appliedThemeIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        appliedThemeIndex = ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        populateDiagnostics()
        binding.btnCopyDiagnostics.setOnClickListener { copyDiagnostics() }
    }

    private fun populateDiagnostics() {
        val backend = Prefs.getString(this, Prefs.KEY_INFERENCE_BACKEND, Prefs.BACKEND_REMOTE)
        val endpoint = Prefs.getString(this, Prefs.KEY_LLM_ENDPOINT, Prefs.DEFAULT_ENDPOINT)
        val model = Prefs.getString(this, Prefs.KEY_LLM_MODEL, Prefs.DEFAULT_MODEL)
        val mcpServers = Prefs.getMCPServers(this)
        val searchProvider = Prefs.getString(this, Prefs.KEY_SEARCH_PROVIDER, "duckduckgo")
        val runtime = Runtime.getRuntime()
        val usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMb = runtime.maxMemory() / (1024 * 1024)

        val diag = buildString {
            appendLine("App Version: ${BuildConfig.VERSION_NAME}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Memory: ${usedMb}MB / ${maxMb}MB")
            appendLine()
            appendLine("Backend: $backend")
            if (backend == Prefs.BACKEND_REMOTE) {
                appendLine("Endpoint: $endpoint")
                appendLine("Model: $model")
                val hasKey = Prefs.getSecret(this@HelpActivity, Prefs.KEY_LLM_API_KEY).isNotBlank()
                appendLine("API Key: ${if (hasKey) "configured" else "not set"}")
            }
            appendLine("Search: $searchProvider")
            val hasSearchKey = Prefs.getSecret(this@HelpActivity, Prefs.KEY_SEARCH_API_KEY).isNotBlank()
            appendLine("Search Key: ${if (hasSearchKey) "configured" else "not set"}")
            appendLine()
            appendLine("MCP Servers: ${mcpServers.size}")
            mcpServers.forEach { s ->
                val status = when (s.lastStatus) {
                    true -> "OK"
                    false -> "ERROR"
                    null -> "untested"
                }
                appendLine("  • ${s.name}: $status${if (!s.enabled) " (disabled)" else ""}")
            }
        }
        binding.tvDiagnostics.text = diag
    }

    private fun copyDiagnostics() {
        val clip = ClipData.newPlainText("Diagnostics", binding.tvDiagnostics.text)
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
        Snackbar.make(binding.root, R.string.diagnostics_copied, Snackbar.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        ThemeHelper.recreateIfNeeded(this, appliedThemeIndex)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
