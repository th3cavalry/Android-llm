package com.th3cavalry.androidllm.data

import io.objectbox.annotation.Entity
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id

@Entity
data class KnowledgeEntry(
    @Id var id: Long = 0,
    var source: String? = null,
    var content: String? = null,
    var timestamp: Long = System.currentTimeMillis(),
    
    // HNSW index for vector search. 
    // 384 is a common dimension for small embedding models like all-MiniLM-L6-v2
    @HnswIndex(dimensions = 384)
    var embedding: FloatArray? = null
)
