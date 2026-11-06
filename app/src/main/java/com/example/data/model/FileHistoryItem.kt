package com.example.data.model

data class FileHistoryItem(
    val id: String,
    val title: String,
    val filePath: String,
    val uriString: String,
    val mimeType: String,
    val sizeBytes: Long,
    val formattedSize: String,
    val timestamp: Long = System.currentTimeMillis(),
    val toolTag: String
)
