package com.th3cavalry.androidllm.service

import android.content.Context
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextEmbedderOptions
import com.th3cavalry.androidllm.App
import com.th3cavalry.androidllm.data.KnowledgeEntry
import com.th3cavalry.androidllm.data.KnowledgeEntry_
import io.objectbox.Box
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service that manages local vector storage and semantic retrieval using ObjectBox and MediaPipe.
 */
class VectorDatabaseService(private val context: Context) {

    private val box: Box<KnowledgeEntry> = App.instance.boxStore.boxFor(KnowledgeEntry::class.java)
    private var textEmbedder: TextEmbedder? = null

    /**
     * Initializes the MediaPipe Text Embedder.
     * @param modelPath Path to the .tflite embedding model (e.g. all-MiniLM-L6-v2.tflite)
     */
    fun initialize(modelPath: String): Result<Unit> {
        return try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(modelPath)
                .build()
            val options = TextEmbedderOptions.builder()
                .setBaseOptions(baseOptions)
                .build()
            textEmbedder = TextEmbedder.createFromOptions(context, options)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates an embedding for the given text.
     */
    private fun embed(text: String): FloatArray? {
        val embedder = textEmbedder ?: return null
        val result = embedder.embed(text)
        return result.embeddingResult().embeddings().firstOrNull()?.floatEmbedding()
    }

    /**
     * Indexes a piece of text with its source.
     */
    suspend fun addKnowledge(source: String, content: String) = withContext(Dispatchers.IO) {
        val embedding = embed(content) ?: return@withContext
        val entry = KnowledgeEntry(
            source = source,
            content = content,
            embedding = embedding
        )
        box.put(entry)
    }

    /**
     * Searches for the most relevant knowledge entries for a query.
     */
    suspend fun findRelevant(query: String, limit: Int = 3): List<KnowledgeEntry> = withContext(Dispatchers.IO) {
        val queryEmbedding = embed(query) ?: return@withContext emptyList()
        
        // ObjectBox Nearest Neighbor Search
        val results = box.query(KnowledgeEntry_.embedding.nearestNeighbors(queryEmbedding, limit))
            .build()
            .find()
        
        results
    }

    /**
     * Clears all knowledge entries from the database.
     */
    fun clearAll() {
        box.removeAll()
    }

    /**
     * Closes the text embedder.
     */
    fun close() {
        textEmbedder?.close()
        textEmbedder = null
    }
}
