package com.th3cavalry.androidllm.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.th3cavalry.androidllm.R
import com.th3cavalry.androidllm.data.ChatSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionListAdapter(
    private val sessions: List<ChatSession>,
    private val onSessionClick: (ChatSession) -> Unit,
    private val onSessionLongClick: (ChatSession) -> Unit
) : RecyclerView.Adapter<SessionListAdapter.SessionViewHolder>() {

    inner class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.sessionTitle)
        val timestampText: TextView = itemView.findViewById(R.id.sessionTimestamp)
        val previewText: TextView = itemView.findViewById(R.id.sessionPreview)
        val menuButton: ImageButton = itemView.findViewById(R.id.sessionMenuButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = sessions[position]
        
        holder.titleText.text = session.title
        holder.timestampText.text = formatTimestamp(session.timestampMs)
        
        // Show preview of first message or "No messages yet"
        val preview = if (session.messages.isNotEmpty()) {
            val firstMsg = session.messages.first()
            "${firstMsg.role}: ${firstMsg.content.take(50)}${if (firstMsg.content.length > 50) "..." else ""}"
        } else {
            "No messages yet"
        }
        holder.previewText.text = preview
        
        holder.itemView.setOnClickListener {
            onSessionClick(session)
        }
        
        holder.menuButton.setOnClickListener {
            onSessionLongClick(session)
        }
        
        holder.itemView.setOnLongClickListener {
            onSessionLongClick(session)
            true
        }
    }

    override fun getItemCount(): Int = sessions.size

    private fun formatTimestamp(timestampMs: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }
}
