package com.example.note.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "notes")
@Serializable
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    // content 现在存储 Markdown 文本
    val content: String,
    val timestamp: Long,

    // 修改：存储绘图叠加数据（例如，序列化的路径 JSON）
    // 不再使用 @Transient，将在 ViewModel 中处理选择性序列化
    val drawingOverlayData: String? = null
) {
    // equals 和 hashCode 需要更新以反映字段变化
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Note

        if (id != other.id) return false
        if (title != other.title) return false
        if (content != other.content) return false
        if (timestamp != other.timestamp) return false
        if (drawingOverlayData != other.drawingOverlayData) return false // String 比较

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + title.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + (drawingOverlayData?.hashCode() ?: 0)
        return result
    }
}
