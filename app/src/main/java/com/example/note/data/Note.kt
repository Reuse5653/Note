package com.example.note.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import kotlinx.serialization.Serializable

/**
 * 代表一个笔记的数据实体类。
 * 使用 `@Entity(tableName = "notes")` 注解，表示这个类对应数据库中的 "notes" 表。
 */
@Entity(tableName = "notes")
@TypeConverters(Converters::class) // 指定使用 Converters 类来处理 Room 不直接支持的类型
@Serializable
data class Note(
    /**
     * 笔记的唯一标识符。
     * `@PrimaryKey(autoGenerate = true)` 表示这是主键，并且由 Room 自动生成。
     */
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /**
     * 笔记的标题。
     */
    val title: String = "",

    /**
     * 笔记的主要内容。
     * 这个字段现在存储的是一个 JSON 字符串。
     * 这个 JSON 字符串代表一个 `List<ContentBlock>` 对象。
     * `ContentBlock` 可以是 `TextBlock` 或 `ImageBlock` (或其他未来定义的类型)。
     * 这种结构允许笔记包含格式化的文本和嵌入的图片（或其他媒体）。
     * 使用 `contentJson` (在 ContentBlock.kt 中定义) 来序列化和反序列化这个字段。
     */
    val content: String = "",

    /**
     * 笔记最后修改的时间戳 (以毫秒为单位的长整型)。
     * 用于排序和显示修改时间。
     */
    val timestamp: Long = 0L,

    /**
     * (可选) 存储绘图覆盖层的数据。
     * 这可以是一个 JSON 字符串、Base64 编码的图片或其他格式，取决于绘图功能的实现。
     */
    val drawingOverlayData: String? = null,

    /**
     * (已弃用/冗余) 存储与此笔记关联的图片 URI 列表。
     * 这个字段最初用于直接存储图片 URI 列表。
     * 现在，图片信息通过 `ImageBlock` 嵌入在 `content` JSON 字符串中。
     * 保留此字段可能是为了兼容旧数据或特定查询，但新逻辑应主要依赖 `content` 字段。
     * Room 使用 `Converters` 类来处理 `List<String>` 和数据库存储之间的转换。
     */
    val imageUris: List<String> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Note

        if (id != other.id) return false
        if (title != other.title) return false
        if (content != other.content) return false
        if (timestamp != other.timestamp) return false
        if (drawingOverlayData != other.drawingOverlayData) return false

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
