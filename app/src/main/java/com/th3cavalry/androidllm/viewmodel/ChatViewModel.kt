package com.th3cavalry.androidllm.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.th3cavalry.androidllm.Prefs
import com.th3cavalry.androidllm.data.ChatMessage
import com.th3cavalry.androidllm.data.ChatSession
import com.th3cavalry.androidllm.data.ConnectionError
import com.th3cavalry.androidllm.data.FunctionCallData
import com.th3cavalry.androidllm.data.MCPServer
import com.th3cavalry.androidllm.data.MessageRole
import com.th3cavalry.androidllm.data.NetworkError
import com.th3cavalry.androidllm.data.ResponseInfo
import com.th3cavalry.androidllm.data.TimeoutError
import com.th3cavalry.androidllm.data.ToolCallData
import com.th3cavalry.androidllm.data.toAppError
import com.th3cavalry.androidllm.db.ChatRepository
import com.th3cavalry.androidllm.network.dto.ToolDto
import com.th3cavalry.androidllm.service.GeminiNanoBackend
import com.th3cavalry.androidllm.service.InferenceBackend
import com.th3cavalry.androidllm.service.LLMService
import com.th3cavalry.androidllm.service.LiteRtLmBackend
import com.th3cavalry.androidllm.service.MCPClient
import com.th3cavalry.androidllm.service.OnDeviceInferenceService
import com.th3cavalry.androidllm.service.ToolExecutor
import com.th3cavalry.androidllm.service.VoiceService
import com.th3cavalry.androidllm.service.VectorDatabaseService
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

import android.app.ActivityManager
import android.content.Context
import java.util.concurrent.atomic.AtomicInteger

/**
 * ViewModel that manages the chat session and the agentic tool-calling loop.
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    /** Memory warning shown to the user when available RAM is low. */
    private val _memoryWarning = MutableLiveData<String?>(null)
    val memoryWarning: LiveData<String?> = _memoryWarning

    private val _isListening = MutableLiveData(false)
    val isListening: LiveData<Boolean> = _isListening

    /** Mutable conversation history (passed to the LLM each turn). */
    private val history: MutableList<ChatMessage> = mutableListOf()

    /** ID of the active session (set when the user first sends a message or loads a session). */
    private var activeSessionId: Long = System.currentTimeMillis()

    private val llmService = LLMService(application)

    /** Room-backed repository for chat session persistence. */
    private val chatRepo = ChatRepository(application)

    private val voiceService = VoiceService(application)
    private val vectorDbService = VectorDatabaseService(application)

    /** Optional document context injected via RAG file loading. */
    private val _documentContext = MutableLiveData<String?>(null)
    val documentContext: LiveData<String?> = _documentContext

    /** Lazily created; only one backend is alive at a time. */
    private var activeBackend: InferenceBackend? = null

    /** The currently running generation job, cancellable via [stopGeneration]. */
    private var currentJob: Job? = null

    /** Callback registered with [App] to handle low-memory events from the system. */
    private val memoryPressureHandler: () -> Unit = {
        // If we're not actively generating, release the backend to free memory
        if (_isLoading.value != true) {
            activeBackend?.let { backend ->
                android.util.Log.w("ChatViewModel", "Low memory: releasing ${backend.displayName}")
                // App.releaseBackendCache() handles closing — just clear local ref
                activeBackend = null
                _memoryWarning.postValue("Low memory — model unloaded to free resources. It will reload on next message.")
            }
        }
    }

    companion object {
        private const val MAX_ON_DEVICE_TOKENS = 1024
        private const val MAX_REACT_ITERATIONS = 10
        private val toolCallCounter = AtomicInteger(0)
        private val gson = Gson()
        private const val MIN_AVAILABLE_RAM_MB = 512
        private const val MAX_HISTORY_SIZE = 50
    }

    init {
        history.add(ChatMessage(role = MessageRole.SYSTEM, content = systemPrompt()))
        // Register for system memory-pressure callbacks
        (application as? com.th3cavalry.androidllm.App)?.addMemoryPressureListener(memoryPressureHandler)
    }

    private fun systemPrompt(): String {
        val base = Prefs.getString(getApplication(), Prefs.KEY_SYSTEM_PROMPT)
            .ifBlank { LLMService.SYSTEM_PROMPT }
        val doc = _documentContext.value
        return if (doc != null) {
            "$base\n\n--- Attached Document Context ---\n$doc"
        } else {
            base
        }
    }

    fun setDocumentContext(content: String?) {
        _documentContext.value = content
        if (history.isNotEmpty() && history[0].role == MessageRole.SYSTEM) {
            history[0] = ChatMessage(role = MessageRole.SYSTEM, content = systemPrompt())
        }
    }

    fun sendMessage(userText: String, imageUri: String? = null) {
        if (userText.isBlank() || _isLoading.value == true || currentJob?.isActive == true) return

        val userMsg = ChatMessage(role = MessageRole.USER, content = userText, imageUri = imageUri)
        history.add(userMsg)
        trimHistoryIfNeeded()
        _messages.value = history.filterVisible()

        _isLoading.value = true
        _error.value = null
        _memoryWarning.value = null

        val backendKey = Prefs.getString(
            getApplication(), Prefs.KEY_INFERENCE_BACKEND, Prefs.BACKEND_REMOTE
        )

        currentJob = viewModelScope.launch {
            try {
                when (backendKey) {
                    Prefs.BACKEND_MEDIAPIPE -> runOnDeviceLoop(userText, getOrCreateBackend<OnDeviceInferenceService>())
                    Prefs.BACKEND_LITERT_LM -> runOnDeviceLoop(userText, getOrCreateBackend<LiteRtLmBackend>())
                    Prefs.BACKEND_GEMINI_NANO -> runOnDeviceLoop(userText, getOrCreateBackend<GeminiNanoBackend>())
                    Prefs.BACKEND_OLLAMA_LOCAL -> runRemoteLoop()
                    else -> runRemoteLoop()
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                
                val appError = e.toAppError()
                val isRetryable = appError is NetworkError || appError is TimeoutError || appError is ConnectionError
                
                history.add(ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = null,
                    errorInfo = com.th3cavalry.androidllm.data.ErrorInfo(
                        message = appError.message,
                        details = e.stackTraceToString().take(500),
                        category = appError.category,
                        isRetryable = isRetryable,
                        originalMessage = userText
                    )
                ))
                _messages.postValue(history.filterVisible())
                _error.postValue("Error: ${appError.message}")
            } finally {
                _isLoading.postValue(false)
                autoSaveSession()
            }
        }
    }

    fun stopGeneration() {
        val job = currentJob ?: return
        currentJob = null
        job.invokeOnCompletion {
            _isLoading.postValue(false)
        }
        job.cancel()
    }

    // ─── Voice Interaction ───────────────────────────────────────────────────

    fun startListening() {
        _isListening.value = true
        voiceService.startListening(
            onResult = { text ->
                _isListening.postValue(false)
                sendMessage(text)
            },
            onError = { err ->
                _isListening.postValue(false)
                _error.postValue(err)
            }
        )
    }

    fun stopListening() {
        _isListening.value = false
        voiceService.stopListening()
    }

    fun speak(text: String) {
        voiceService.speak(text)
    }

    fun stopVoice() {
        voiceService.stopSpeech()
    }

    fun retryMessage(errorMessage: ChatMessage) {
        val originalText = errorMessage.errorInfo?.originalMessage ?: return
        history.removeAll { it === errorMessage }
        _messages.value = history.filterVisible()
        sendMessage(originalText)
    }

    fun clearHistory() {
        history.clear()
        history.add(ChatMessage(role = MessageRole.SYSTEM, content = systemPrompt()))
        _messages.value = emptyList()
        activeSessionId = System.currentTimeMillis()
    }

    fun saveCurrentSession() {
        val sessionMessages = history.filter { it.role != MessageRole.SYSTEM }
        if (sessionMessages.isEmpty()) return
        val session = ChatSession(
            id = activeSessionId,
            title = sessionTitle(sessionMessages),
            timestamp = activeSessionId,
            messages = sessionMessages
        )
        viewModelScope.launch {
            chatRepo.saveSession(session)
        }
    }

    fun loadSession(session: ChatSession) {
        history.clear()
        history.add(ChatMessage(role = MessageRole.SYSTEM, content = systemPrompt()))
        history.addAll(session.messages)
        activeSessionId = session.id
        _messages.value = history.filterVisible()
    }

    val savedSessions: LiveData<List<ChatSession>> = chatRepo.observeSessions()

    fun loadSessionById(id: Long) {
        viewModelScope.launch {
            chatRepo.getSession(id)?.let { loadSession(it) }
        }
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch { chatRepo.deleteSession(id) }
    }

    fun renameSession(id: Long, title: String) {
        viewModelScope.launch { chatRepo.renameSession(id, title) }
    }

    suspend fun getSessionForExport(id: Long): ChatSession? = chatRepo.getSession(id)

    fun migrateFromPrefsIfNeeded() {
        viewModelScope.launch {
            if (chatRepo.sessionCount() > 0) return@launch
            val legacy = Prefs.getSavedSessions(getApplication())
            if (legacy.isEmpty()) return@launch
            for (session in legacy) {
                chatRepo.saveSession(session)
            }
        }
    }

    private suspend fun runRemoteLoop() {
        val tools = buildToolsList()
        llmService.chat(
            history = history,
            tools = tools,
            onProgress = { updatedHistory ->
                _messages.postValue(updatedHistory.filterVisible())
            }
        )
        _messages.postValue(history.filterVisible())
    }

    private inline fun <reified T : InferenceBackend> getOrCreateBackend(): T {
        val app = getApplication<Application>() as? com.th3cavalry.androidllm.App
        val cached = app?.cachedBackend
        if (cached is T) {
            activeBackend = cached
            return cached
        }

        val current = activeBackend
        if (current is T) return current
        current?.close()

        val availableMb = getAvailableMemoryMb()
        if (availableMb < MIN_AVAILABLE_RAM_MB) {
            _memoryWarning.postValue(
                "Low memory warning: only ${availableMb} MB available. " +
                "Model loading may fail on this device."
            )
        }

        val newBackend: T = when (T::class) {
            OnDeviceInferenceService::class -> OnDeviceInferenceService(getApplication()) as T
            LiteRtLmBackend::class         -> LiteRtLmBackend(getApplication()) as T
            GeminiNanoBackend::class       -> GeminiNanoBackend(getApplication()) as T
            else -> error("Unknown backend type")
        }
        activeBackend = newBackend
        app?.cacheBackend(newBackend)
        return newBackend
    }

    private fun getAvailableMemoryMb(): Long {
        val activityManager = getApplication<Application>()
            .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.availMem / (1024 * 1024)
    }

    private fun trimHistoryIfNeeded() {
        if (history.size <= MAX_HISTORY_SIZE) return
        val systemPrompt = history.firstOrNull { it.role == MessageRole.SYSTEM }
        val recentMessages = history.takeLast(MAX_HISTORY_SIZE - 1)
        history.clear()
        if (systemPrompt != null) history.add(systemPrompt)
        history.addAll(recentMessages)
    }

    private suspend fun runOnDeviceLoop(userText: String, backend: InferenceBackend) {
        val modelPathKey = when (backend) {
            is LiteRtLmBackend          -> Prefs.KEY_LITERT_LM_MODEL_PATH
            is OnDeviceInferenceService -> Prefs.KEY_ON_DEVICE_MODEL_PATH
            is GeminiNanoBackend        -> null
            else -> error("Unhandled backend type")
        }

        if (!backend.isReady()) {
            val modelPath = if (modelPathKey != null) {
                Prefs.getString(getApplication(), modelPathKey).also { path ->
                    if (path.isBlank()) {
                        _error.postValue("No model configured.")
                        return
                    }
                }
            } else ""
            
            val result = backend.initialize(
                modelPath = modelPath,
                maxTokens = Prefs.getInt(getApplication(), Prefs.KEY_LLM_MAX_TOKENS, Prefs.DEFAULT_MAX_TOKENS).coerceAtMost(MAX_ON_DEVICE_TOKENS),
                temperature = Prefs.getFloat(getApplication(), Prefs.KEY_LLM_TEMPERATURE, Prefs.DEFAULT_TEMPERATURE)
            )
            if (result.isFailure) {
                _error.postValue("Failed to load model: ${result.exceptionOrNull()?.message}")
                return
            }
        }

        val tools = buildToolsList()
        val toolDescriptions = buildToolDescriptionsText(tools)
        val executor = ToolExecutor(getApplication())
        val context = StringBuilder()

        for (msg in history.dropLast(1)) {
            when (msg.role) {
                MessageRole.USER -> context.appendLine("User: ${msg.content}")
                MessageRole.ASSISTANT -> if (msg.toolCalls.isNullOrEmpty()) context.appendLine("Assistant: ${msg.content}")
                else -> {}
            }
        }
        context.append("User: $userText\nAssistant:")

        val toolCallStartTag = "<tool_call>"
        val toolCallEndTag = "</tool_call>"
        var iterations = 0
        var completedNormally = false
        val startMs = System.currentTimeMillis()

        while (iterations < MAX_REACT_ITERATIONS) {
            iterations++
            val fullPrompt = buildOnDeviceSystemPrompt(toolDescriptions) + context
            val streamingMessage = ChatMessage(role = MessageRole.ASSISTANT, content = "", isStreaming = true)
            history.add(streamingMessage)
            _messages.postValue(history.filterVisible())
            
            val rawResponse = StringBuilder()
            backend.generateStream(fullPrompt).collect { token ->
                rawResponse.append(token)
                val updatedMessage = streamingMessage.copy(content = rawResponse.toString())
                history[history.lastIndexOf(streamingMessage)] = updatedMessage
                _messages.postValue(history.filterVisible())
            }
            
            val finalMessage = streamingMessage.copy(content = rawResponse.toString().trim(), isStreaming = false)
            history[history.lastIndexOf(streamingMessage)] = finalMessage
            
            val trimmedResponse = rawResponse.toString().trim()
            val startIdx = trimmedResponse.indexOf(toolCallStartTag)
            val endIdx = trimmedResponse.indexOf(toolCallEndTag)
            val toolCallJson = if (startIdx >= 0 && endIdx > startIdx && trimmedResponse.startsWith(toolCallStartTag)) {
                trimmedResponse.substring(startIdx + toolCallStartTag.length, endIdx).trim()
            } else null

            if (toolCallJson != null) {
                @Suppress("UNCHECKED_CAST")
                val toolCallMap = runCatching { gson.fromJson(toolCallJson, Map::class.java) as? Map<String, Any?> }.getOrNull()
                if (toolCallMap == null) {
                    history.add(ChatMessage(role = MessageRole.ASSISTANT, content = "Invalid tool call JSON."))
                    _messages.postValue(history.filterVisible())
                    return
                }

                history.remove(finalMessage)
                val toolName = toolCallMap["name"]?.toString() ?: ""
                @Suppress("UNCHECKED_CAST")
                val toolArgs = toolCallMap["arguments"]?.let { it as? Map<String, Any?> } ?: emptyMap()
                val callId = "od_${toolCallCounter.incrementAndGet()}"
                
                history.add(ChatMessage(role = MessageRole.ASSISTANT, content = null, toolCalls = listOf(ToolCallData(id = callId, type = "function", function = FunctionCallData(toolName, toolCallJson)))))
                val executingMessage = ChatMessage(role = MessageRole.ASSISTANT, content = null, executingInfo = com.th3cavalry.androidllm.data.ExecutingInfo(toolName = toolName, status = getToolExecutingStatus(toolName)))
                history.add(executingMessage)
                _messages.postValue(history.filterVisible())

                val toolResult = runCatching { executor.execute(toolName, toolArgs) }.getOrElse { "Error: ${it.message}" }
                history.remove(executingMessage)
                history.add(ChatMessage(role = MessageRole.TOOL, content = toolResult, toolCallId = callId, toolName = toolName))
                _messages.postValue(history.filterVisible())

                context.appendLine(" $toolCallStartTag$toolCallJson$toolCallEndTag")
                context.appendLine("Tool result ($toolName): $toolResult")
                context.append("Assistant:")
            } else {
                val durationMs = System.currentTimeMillis() - startMs
                val finalText = trimmedResponse.removePrefix(":").trim()
                val updatedFinal = finalMessage.copy(content = finalText, responseInfo = ResponseInfo(backend.displayName, null, durationMs))
                history[history.lastIndexOf(finalMessage)] = updatedFinal
                _messages.postValue(history.filterVisible())
                completedNormally = true
                break
            }
        }

        if (!completedNormally) {
            history.add(ChatMessage(role = MessageRole.ASSISTANT, content = "Maximum reasoning steps reached."))
            _messages.postValue(history.filterVisible())
        }
    }

    private fun buildOnDeviceSystemPrompt(toolDescriptions: String): String = """
You are a powerful AI assistant. You have access to the following tools:

$toolDescriptions

To call a tool, output EXACTLY this and nothing else on that turn:
<tool_call>{"name":"tool_name","arguments":{"param":"value"}}</tool_call>

After a tool result is given, continue reasoning. When you have the final answer, respond normally without any <tool_call> tags.
""".trimIndent() + "\n"

    private fun buildToolDescriptionsText(tools: List<ToolDto>): String =
        tools.joinToString("\n") { tool ->
            val params = tool.function.parameters["properties"]
                ?.let { it as? Map<*, *> }
                ?.entries
                ?.joinToString(", ") { (k, v) ->
                    val desc = (v as? Map<*, *>)?.get("description")?.toString() ?: ""
                    "$k: $desc"
                } ?: ""
            "• ${tool.function.name}: ${tool.function.description}" +
                if (params.isNotEmpty()) "\n  Parameters: $params" else ""
        }

    private suspend fun buildToolsList(): List<ToolDto> {
        val tools = LLMService.builtInTools().toMutableList()
        val mcpServers: List<MCPServer> = Prefs.getMCPServers(getApplication())
        for (server in mcpServers.filter { it.enabled }) {
            try {
                val client = MCPClient(server)
                if (client.initialize()) tools.addAll(client.listTools())
            } catch (e: Exception) {}
        }
        return tools
    }

    private fun List<ChatMessage>.filterVisible(): List<ChatMessage> {
        val hideTools = Prefs.getBoolean(getApplication(), Prefs.KEY_HIDE_TOOL_MESSAGES, false)
        return filter { msg ->
            when {
                msg.role == MessageRole.SYSTEM -> false
                hideTools && msg.role == MessageRole.TOOL -> false
                hideTools && msg.role == MessageRole.ASSISTANT && msg.toolCalls != null -> false
                msg.executingInfo != null -> true
                else -> true
            }
        }
    }

    private fun getToolExecutingStatus(toolName: String): String? {
        return when {
            toolName.contains("ssh") -> "Connecting to remote server..."
            toolName.contains("github") -> "Accessing GitHub API..."
            toolName.contains("search") -> "Searching the web..."
            toolName.contains("fetch") -> "Fetching URL content..."
            toolName.contains("system_set_alarm") -> "Setting alarm..."
            toolName.contains("system_create_calendar_event") -> "Opening calendar..."
            toolName.contains("system_get_info") -> "Reading device status..."
            toolName.contains("knowledge_search") -> "Searching knowledge base..."
            toolName.contains("__") -> "Calling MCP server..."
            else -> null
        }
    }

    private fun autoSaveSession() {
        val visibleMessages = history.filterVisible()
        if (visibleMessages.none { it.role == MessageRole.USER }) return
        val session = ChatSession(
            id = activeSessionId,
            title = sessionTitle(visibleMessages),
            timestamp = activeSessionId,
            messages = visibleMessages
        )
        viewModelScope.launch { chatRepo.saveSession(session) }
    }

    private fun sessionTitle(messages: List<ChatMessage>): String =
        messages.firstOrNull { it.role == MessageRole.USER }?.content?.take(60) ?: "Chat"

    override fun onCleared() {
        super.onCleared()
        voiceService.shutdown()
        vectorDbService.close()
        activeBackend = null
        (getApplication<Application>() as? com.th3cavalry.androidllm.App)
            ?.removeMemoryPressureListener(memoryPressureHandler)
    }
}
