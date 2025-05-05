package com.example.note.viewmodel

import android.app.Application
import android.content.Context // 导入 Context
import android.net.Uri
import android.util.Base64 // 导入 Base64
import androidx.core.content.FileProvider // 导入 FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.note.data.Note
import com.example.note.data.NoteDatabase
import com.example.note.data.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable // 导入 Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File // 导入 File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID // 用于生成唯一文件名
import java.io.InputStream // 导入 InputStream
import kotlinx.serialization.builtins.ListSerializer // Ensure this is imported
import com.example.note.data.ContentBlock // Import ContentBlock
import com.example.note.data.contentJson // Import contentJson

// --- 辅助数据类用于导出/导入 ---
@Serializable
data class ExportedImage(
    val filename: String, // 用于保存时的文件名建议
    val base64Data: String
)

@Serializable
data class NoteForExport(
    // 包含 Note 的所有字段，除了 imageUris
    val id: Int = 0,
    val title: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val drawingOverlayData: String? = null,
    // 使用新的 ExportedImage 列表
    val images: List<ExportedImage> = emptyList()
    // 可以添加其他需要导出的字段，如录音等
)
// --- 结束辅助数据类 ---


class NoteViewModel(
    private val repository: NoteRepository,
    private val application: Application // 添加 Application 成员变量以获取 Context
) : ViewModel() {

    val allNotes: StateFlow<List<Note>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    // Helper function to get Context
    private fun getContext(): Context = application.applicationContext

    // --- Helper function to copy image URI content to internal storage ---
    suspend fun copyImageToInternalStorage(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = getContext().contentResolver.openInputStream(uri)
            if (inputStream == null) {
                println("Failed to open input stream for URI: $uri")
                return@withContext null
            }

            // 创建存储目录
            val imageDir = File(getContext().filesDir, "note_images")
            if (!imageDir.exists()) {
                imageDir.mkdirs()
            }
            // 创建唯一文件名
            val extension = getUriExtension(uri) // 尝试获取扩展名
            val outputFile = File(imageDir, "${UUID.randomUUID()}.$extension")

            // 复制文件
            FileOutputStream(outputFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close() // 关闭输入流

            // 获取新文件的 FileProvider URI
            val newUri = FileProvider.getUriForFile(
                getContext(),
                "${getContext().packageName}.provider",
                outputFile
            )
            println("Copied image to internal URI: $newUri")
            newUri.toString() // 返回 String 类型的 URI
        } catch (e: IOException) {
            println("Error copying image: ${e.message}")
            e.printStackTrace()
            null
        } catch (e: SecurityException) {
            println("Security error accessing URI: $uri, ${e.message}")
            e.printStackTrace()
            null
        } catch (e: Exception) {
            println("Generic error copying image: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    // Helper to try and get a file extension from URI
    private fun getUriExtension(uri: Uri): String {
        val mimeType = getContext().contentResolver.getType(uri)
        return when (mimeType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            // 添加更多 MIME 类型判断
            else -> "jpg" // 默认扩展名
        }
    }
    // --- End Helper function ---

    fun getNoteById(id: Int): Flow<Note?> {
        return repository.getNoteById(id)
    }

    fun delete(note: Note) = viewModelScope.launch { repository.delete(note) }

    fun deleteNotesByIds(ids: List<Int>) = viewModelScope.launch {
        repository.deleteNotesByIds(ids)
    }

    /**
     * Saves a note. Inserts if id is 0, updates otherwise.
     * Returns the ID of the saved note.
     */
    suspend fun saveNote(note: Note): Long {
        return if (note.id == 0) {
            val noteToInsert = if (note.timestamp == 0L) {
                note.copy(timestamp = System.currentTimeMillis())
            } else {
                note
            }
            repository.insert(noteToInsert)
        } else {
            // 更新时也确保更新时间戳
            val noteToUpdate = note.copy(timestamp = System.currentTimeMillis())
            repository.update(noteToUpdate)
            noteToUpdate.id.toLong()
        }
    }

    /**
     * 导出笔记为 JSON 字符串。现在包含 Base64 编码的图片数据。
     * @param idsToExport 要导出的笔记 ID 列表，如果为 null 或空，则导出所有笔记。
     * @param includeImages 是否包含图片数据。
     * @param includeDrawings 是否包含绘图数据。
     * @param includeRecordings 是否包含录音数据 (待实现)。
     */
    suspend fun exportNotesToJsonString(
        idsToExport: List<Int>? = null,
        includeImages: Boolean,
        includeDrawings: Boolean,
        includeRecordings: Boolean // 参数保留，但暂不使用
    ): String = withContext(Dispatchers.IO) { // 使用 IO 调度器处理文件操作
        val notesToExport = if (idsToExport.isNullOrEmpty()) {
            allNotes.first()
        } else {
            allNotes.first().filter { it.id in idsToExport }
        }

        val notesForExportList = notesToExport.map { note ->
            val exportedImages = if (includeImages) {
                note.imageUris.mapNotNull { uriString ->
                    try {
                        val uri = Uri.parse(uriString) // URI 现在指向内部文件
                        // 直接从内部文件 URI 读取
                        getContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                            // ... (读取、Base64编码，与之前类似) ...
                            val buffer = ByteArrayOutputStream()
                            inputStream.copyTo(buffer)
                            val byteArray = buffer.toByteArray()
                            val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)
                            val filename = uri.lastPathSegment ?: "image_${UUID.randomUUID()}.jpg"
                            ExportedImage(filename, base64String)
                        }
                    } catch (e: Exception) {
                        println("Error reading internal image for export: $uriString, ${e.message}")
                        e.printStackTrace()
                        null
                    }
                }
            } else {
                emptyList()
            }
            // ... (创建 NoteForExport) ...
            NoteForExport(
                id = note.id,
                title = note.title,
                content = note.content,
                timestamp = note.timestamp,
                drawingOverlayData = if (includeDrawings) note.drawingOverlayData else null,
                images = exportedImages
            )
        }
        json.encodeToString(notesForExportList)
    }

    /**
     * 从 JSON 字符串导入笔记。现在处理 Base64 编码的图片数据。
     * @return 导入成功的新笔记 ID 列表，如果失败则返回 null。
     */
    suspend fun importNotesFromJsonString(jsonString: String): List<Int>? = withContext(Dispatchers.IO) {
        try {
            // 反序列化为 NoteForExport 列表
            val notesToImport = json.decodeFromString(ListSerializer(NoteForExport.serializer()), jsonString)
            val newIds = mutableListOf<Long>()

            // 使用统一的目录名
            val imageDir = File(getContext().filesDir, "note_images") // Changed to "note_images"
            if (!imageDir.exists()) {
                imageDir.mkdirs()
            }

            notesToImport.forEach { noteData ->
                val importedImageUris = noteData.images.mapNotNull { exportedImage ->
                    try {
                        // Base64 解码
                        val imageData = Base64.decode(exportedImage.base64Data, Base64.DEFAULT)
                        // 使用 UUID 和原始文件名（如果可用）
                        val filename = exportedImage.filename.takeUnless { it.isBlank() } ?: "${UUID.randomUUID()}.jpg"
                        val imageFile = File(imageDir, "${UUID.randomUUID()}_${filename}") // Ensure unique name
                        FileOutputStream(imageFile).use { outputStream ->
                            outputStream.write(imageData)
                        }
                        FileProvider.getUriForFile(
                            getContext(),
                            "${getContext().packageName}.provider",
                            imageFile
                        ).toString()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }

                // 创建新的 Note 对象，使用新的本地 URI
                val noteToInsert = Note(
                    id = 0, // 强制 id=0 以便 Room 生成新 ID
                    title = noteData.title,
                    content = noteData.content,
                    timestamp = if (noteData.timestamp == 0L) System.currentTimeMillis() else noteData.timestamp,
                    drawingOverlayData = noteData.drawingOverlayData,
                    imageUris = importedImageUris // 使用新生成的 URI 列表
                )

                val newId = repository.insertAndGetId(noteToInsert)
                newIds.add(newId)
            }
            newIds.map { it.toInt() }
        } catch (e: SerializationException) {
            e.printStackTrace()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 复制现有笔记或基于当前标题/内容创建新笔记（如果 noteId 为 null）。
     * 现在需要处理 content 字段的 JSON 格式。
     */
    suspend fun duplicateNote(noteId: Int?, currentTitle: String, currentContentJson: String) { // Parameter changed to currentContentJson
        val originalNote = if (noteId != null) repository.getNoteById(noteId).first() else null

        // 如果是复制现有笔记，直接使用其 content (已经是 JSON)
        // 如果是基于当前编辑状态创建（noteId 为 null），则使用传入的 currentContentJson
        val contentToDuplicate = originalNote?.content ?: currentContentJson

        // 提取图片 URI (如果需要复制图片文件，逻辑会更复杂，目前仅复制 URI 字符串)
        // 注意：直接复制内部存储的 URI 在同一设备上是有效的。
        val imageUrisToDuplicate: List<String> = try { // Explicit type List<String>
            // Use imported contentJson
            val blocks = contentJson.decodeFromString<List<ContentBlock>>(contentToDuplicate)
            // Specify types for filterIsInstance and map
            blocks.filterIsInstance<ContentBlock.ImageBlock>().map { it: ContentBlock.ImageBlock -> it.uri }
        } catch (e: Exception) {
            // Specify type for emptyList
            originalNote?.imageUris ?: emptyList<String>()
        }


        val noteToDuplicate = Note(
            id = 0, // Ensure new ID is generated
            title = originalNote?.title ?: currentTitle,
            content = contentToDuplicate, // Save the JSON content
            timestamp = System.currentTimeMillis(),
            drawingOverlayData = originalNote?.drawingOverlayData, // Copy drawing data if exists
            imageUris = imageUrisToDuplicate // Type should now match
        )
        saveNote(noteToDuplicate) // saveNote handles insertion
    }
}

class NoteViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            val database = NoteDatabase.getDatabase(application)
            val noteDao = database.noteDao()
            val repository = NoteRepository(noteDao)
            @Suppress("UNCHECKED_CAST")
            // 将 application 实例传递给 ViewModel
            return NoteViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
