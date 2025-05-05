package com.example.note.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.example.note.data.Note // 添加 Note 类的导入

@Dao // 标记为 DAO 接口
interface NoteDao {

    // 插入笔记，如果冲突则替换旧笔记
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note) // 使用 suspend 关键字，表示在协程中执行

    // 更新笔记
    @Update
    suspend fun updateNote(note: Note)

    // 删除笔记
    @Delete
    suspend fun deleteNote(note: Note)

    // 根据 ID 列表删除笔记
    @Query("DELETE FROM notes WHERE id IN (:ids)")
    suspend fun deleteNotesByIds(ids: List<Int>)

    // 获取所有笔记，按时间戳降序排列，并返回 Flow<List<Note>>
    // Flow 使得数据变化时能自动通知观察者
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    // 根据 ID 获取单个笔记
    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteById(id: Int): Flow<Note?> // 返回 Flow<Note?>，因为可能找不到对应 ID 的笔记

    // 修改 Insert，让其返回生成的 Long ID
    @Insert // 默认冲突策略为 ABORT，但因为我们传入 id=0，所以总是插入
    suspend fun insertAndGetId(note: Note): Long

    // 保留 Upsert (如果其他地方需要)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: Note)
}
