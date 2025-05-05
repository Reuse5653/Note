package com.example.note.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.note.data.Note
import com.example.note.data.NoteDatabase
import com.example.note.data.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException
import kotlinx.coroutines.withContext

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    val allNotes: StateFlow<List<Note>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun getNoteById(id: Int): Flow<Note?> {
        return repository.getNoteById(id)
    }

    fun insert(note: Note) = viewModelScope.launch { repository.insert(note) }

    fun update(note: Note) = viewModelScope.launch { repository.update(note) }

    fun delete(note: Note) = viewModelScope.launch { repository.delete(note) }

    fun deleteNotesByIds(ids: List<Int>) = viewModelScope.launch {
        repository.deleteNotesByIds(ids)
    }

    /**
     * 导出笔记为 JSON 字符串。
     * @param idsToExport 可选的笔记 ID 列表。如果为 null 或空，则导出所有笔记。
     * @param includeDrawings 是否在导出的 JSON 中包含绘图数据。
     */
    suspend fun exportNotesToJsonString(
        idsToExport: List<Int>? = null,
        includeDrawings: Boolean // 新增参数
    ): String = withContext(Dispatchers.IO) {
        val currentNotes = allNotes.first()
        val notesToFilter = if (idsToExport.isNullOrEmpty()) {
            currentNotes
        } else {
            val idsSet = idsToExport.toSet()
            currentNotes.filter { note -> note.id in idsSet }
        }

        // 根据 includeDrawings 标志处理绘图数据
        val notesToSerialize = if (includeDrawings) {
            notesToFilter // 直接序列化，包含 drawingOverlayData
        } else {
            // 创建不包含绘图数据的新列表进行序列化
            notesToFilter.map { it.copy(drawingOverlayData = null) }
        }

        json.encodeToString(notesToSerialize)
    }

    /**
     * 从 JSON 字符串导入笔记，始终创建新笔记（即使内容重复）。
     * 保留原始时间戳，生成新 ID。
     * @return 成功导入的新笔记的 ID 列表，如果解析失败则返回 null。
     */
    suspend fun importNotesFromJsonString(jsonString: String): List<Int>? = withContext(Dispatchers.IO) {
        try {
            val notesToImport = json.decodeFromString<List<Note>>(jsonString)
            val newIds = mutableListOf<Long>()
            notesToImport.forEach { note ->
                // 始终插入新笔记，强制 id=0，并获取返回的新 ID
                val newId = repository.insertAndGetId(note.copy(id = 0))
                newIds.add(newId)
            }
            newIds.map { it.toInt() } // 转换为 List<Int> 返回
        } catch (e: SerializationException) {
            e.printStackTrace()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

class NoteViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            val database = NoteDatabase.getDatabase(application)
            val noteDao = database.noteDao()
            val repository = NoteRepository(noteDao)
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
