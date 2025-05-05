package com.example.note.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {

    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    fun getNoteById(id: Int): Flow<Note?> {
        return noteDao.getNoteById(id)
    }

    suspend fun insert(note: Note): Long { // 修改返回类型为 Long
        return noteDao.insert(note)
    }

    suspend fun update(note: Note) {
        noteDao.update(note)
    }

    suspend fun delete(note: Note) {
        noteDao.delete(note)
    }

    suspend fun deleteNotesByIds(ids: List<Int>) {
        noteDao.deleteNotesByIds(ids)
    }

    // 添加 insertAndGetId 的调用
    suspend fun insertAndGetId(note: Note): Long {
        return noteDao.insertAndGetId(note)
    }
}
