package com.example.note.data

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString // 确保导入 decodeFromString

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let { Json.decodeFromString<List<String>>(it) }
    }

    // 如果将来添加了 TodoItem 等其他复杂类型，可以在这里添加更多转换器
    // @TypeConverter
    // fun fromTodoItemList(value: List<TodoItem>?): String? { ... }
    // @TypeConverter
    // fun toTodoItemList(value: String?): List<TodoItem>? { ... }
}

// 如果需要 TodoItem，定义数据类
// @Serializable
// data class TodoItem(val text: String, val isChecked: Boolean)
