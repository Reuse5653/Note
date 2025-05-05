package com.example.note.data

import kotlinx.coroutines.flow.Flow

// Repository 类，封装了对数据源（NoteDao）的访问
// 通常构造函数接收 DAO 作为参数
class NoteRepository(private val noteDao: NoteDao) {

  // 获取所有笔记的 Flow，直接从 DAO 获取
  val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

  // 根据 ID 获取单个笔记的 Flow，直接从 DAO 获取
  fun getNoteById(id: Int): Flow<Note?> {
    return noteDao.getNoteById(id)
  }

  // 插入笔记，这是一个挂起函数，因为它调用了 DAO 中的挂起函数
  suspend fun insert(note: Note) {
    noteDao.upsert(note.copy(id = 0)) // 或者使用 insertIgnore 等
  }

  // 新增：插入并返回 ID
  suspend fun insertAndGetId(note: Note): Long {
    return noteDao.insertAndGetId(note)
  }

  // 保留 upsert (如果需要)
  suspend fun upsert(note: Note) {
    noteDao.upsert(note)
  }

  // 更新笔记
  suspend fun update(note: Note) {
    noteDao.updateNote(note)
  }

  // 删除笔记
  suspend fun delete(note: Note) {
    noteDao.deleteNote(note)
  }

  // 根据 ID 列表删除笔记
  suspend fun deleteNotesByIds(ids: List<Int>) {
    noteDao.deleteNotesByIds(ids)
  }
}
