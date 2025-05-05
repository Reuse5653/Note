package com.example.note.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.json.Json
import java.util.UUID // Import UUID

/**
 * 定义笔记内容的结构块。
 * 这是一个密封类 (sealed class)，意味着笔记内容可以由预定义类型的块组成。
 * 使用 `@Serializable` 注解，使其可以被 kotlinx.serialization 库序列化/反序列化为 JSON。
 */
@Serializable
sealed class ContentBlock {
    // 添加 ID 属性
    abstract val id: String

    /**
     * 代表一个文本块。
     * @property text 文本块的内容字符串。默认为空字符串。
     * @property id 块的唯一标识符。
     */
    @Serializable
    data class TextBlock(
        var text: String = "",
        // Override id and provide default value using UUID
        override val id: String = UUID.randomUUID().toString()
    ) : ContentBlock()

    /**
     * 代表一个图片块。
     * @property uri 图片的 URI (统一资源标识符) 字符串。
     *             这个 URI 指向应用内部存储中的图片文件 (通过 FileProvider 访问)。
     * @property id 块的唯一标识符。
     */
    @Serializable
    data class ImageBlock(
        val uri: String,
        // Override id and provide default value using UUID
        override val id: String = UUID.randomUUID().toString()
    ) : ContentBlock()

    // 未来可以添加其他类型的块，例如：
    // @Serializable data class AudioBlock(val uri: String, val duration: Long) : ContentBlock()
    // @Serializable data class TodoBlock(val text: String, var isChecked: Boolean = false) : ContentBlock()
}

/**
 * 配置 kotlinx.serialization 的 Json 实例，用于处理 ContentBlock 的多态性。
 * - `ignoreUnknownKeys = true`: 允许在解析 JSON 时忽略未在类中定义的字段。
 * - `encodeDefaults = true`: 确保在序列化时包含具有默认值的属性。
 * - `serializersModule`: 定义了 ContentBlock 是一个多态基类，并指定了它的子类 TextBlock 和 ImageBlock。
 *   这使得 Json 库能够正确地序列化和反序列化包含不同类型 ContentBlock 的列表。
 */
val contentJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true // Ensure default values (like empty string in TextBlock) are encoded
    serializersModule = SerializersModule {
        polymorphic(ContentBlock::class) {
            subclass(ContentBlock.TextBlock::class)
            subclass(ContentBlock.ImageBlock::class)
        }
    }
}
