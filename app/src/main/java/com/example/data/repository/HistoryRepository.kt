package com.example.data.repository

import android.content.Context
import com.example.data.model.FileHistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class HistoryRepository(context: Context) {
    private val prefs = context.getSharedPreferences("recent_files_prefs", Context.MODE_PRIVATE)
    private val _recentFiles = MutableStateFlow<List<FileHistoryItem>>(emptyList())
    val recentFiles: StateFlow<List<FileHistoryItem>> = _recentFiles.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        val raw = prefs.getString("history_json", null) ?: return
        try {
            val arr = JSONArray(raw)
            val list = mutableListOf<FileHistoryItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    FileHistoryItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        filePath = obj.getString("filePath"),
                        uriString = obj.getString("uriString"),
                        mimeType = obj.getString("mimeType"),
                        sizeBytes = obj.getLong("sizeBytes"),
                        formattedSize = obj.getString("formattedSize"),
                        timestamp = obj.getLong("timestamp"),
                        toolTag = obj.getString("toolTag")
                    )
                )
            }
            _recentFiles.value = list
        } catch (_: Exception) {
            _recentFiles.value = emptyList()
        }
    }

    fun addHistoryItem(item: FileHistoryItem) {
        val current = _recentFiles.value.toMutableList()
        // Remove existing item if same path exists
        current.removeAll { it.filePath == item.filePath }
        current.add(0, item)
        // Keep top 30
        val trimmed = if (current.size > 30) current.take(30) else current
        _recentFiles.value = trimmed

        try {
            val arr = JSONArray()
            for (i in trimmed) {
                val obj = JSONObject().apply {
                    put("id", i.id)
                    put("title", i.title)
                    put("filePath", i.filePath)
                    put("uriString", i.uriString)
                    put("mimeType", i.mimeType)
                    put("sizeBytes", i.sizeBytes)
                    put("formattedSize", i.formattedSize)
                    put("timestamp", i.timestamp)
                    put("toolTag", i.toolTag)
                }
                arr.put(obj)
            }
            prefs.edit().putString("history_json", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun clearHistory() {
        _recentFiles.value = emptyList()
        prefs.edit().remove("history_json").apply()
    }
}
