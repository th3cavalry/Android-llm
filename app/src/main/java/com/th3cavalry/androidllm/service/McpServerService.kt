package com.th3cavalry.androidllm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.th3cavalry.androidllm.R
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Foreground service that hosts a local MCP server, allowing other apps
 * or devices on the same network to use this app's tools.
 */
class McpServerService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var server: NettyApplicationEngine? = null

    companion object {
        private const val CHANNEL_ID = "mcp_server_channel"
        private const val NOTIFICATION_ID = 101
        private const val PORT = 3000

        fun start(context: Context) {
            val intent = Intent(context, McpServerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, McpServerService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        startServer()
    }

    private fun startServer() {
        server = embeddedServer(Netty, port = PORT) {
            install(ContentNegotiation) {
                gson()
            }
            routing {
                get("/") {
                    call.respond(mapOf("status" to "SovereignDroid MCP Server Running"))
                }
                
                // Simple SSE-like tool discovery endpoint
                get("/mcp/v1/tools") {
                    val tools = LLMService.builtInTools().map { tool ->
                        mapOf(
                            "name" to tool.function.name,
                            "description" to tool.function.description,
                            "inputSchema" to tool.function.parameters
                        )
                    }
                    call.respond(mapOf("tools" to tools))
                }

                post("/mcp/v1/call/{name}") {
                    val toolName = call.parameters["name"] ?: ""
                    // In a full implementation, we would parse arguments and call ToolExecutor here.
                    // For now, return a placeholder.
                    call.respond(mapOf("result" to "Tool $toolName execution from remote not yet fully implemented."))
                }
            }
        }.start(wait = false)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "MCP Server"
            val descriptionText = "Notifications for local MCP server"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SovereignDroid MCP Server")
            .setContentText("Listening on port $PORT")
            .setSmallIcon(R.drawable.ic_terminal)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stop(1000, 2000)
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
