package com.example.note.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// 再次增加数据库版本号，例如从 2 增加到 3
@Database(entities = [Note::class], version = 3, exportSchema = false)
abstract class NoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "note_database"
                )
                    // 保持破坏性迁移（开发用）
                    .fallbackToDestructiveMigration()
                    // 或者添加实际的迁移逻辑
                    // .addMigrations(MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // 示例：如果需要，可以定义迁移逻辑
        // val MIGRATION_2_3 = object : Migration(2, 3) {
        //     override fun migrate(db: SupportSQLiteDatabase) {
        //         // 从 version 2 迁移到 version 3 的 SQL 语句
        //         // 例如：ALTER TABLE notes ADD COLUMN drawingOverlayData TEXT
        //         db.execSQL("ALTER TABLE notes ADD COLUMN drawingOverlayData TEXT")
        //         // 注意：之前的 drawing BLOB 列如果存在，需要处理或删除
        //     }
        // }
    }
}
