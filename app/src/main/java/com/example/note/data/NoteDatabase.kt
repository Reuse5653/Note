package com.example.note.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters // 导入 TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Note::class], version = 4, exportSchema = false) // 版本号增加到 4
@TypeConverters(Converters::class) // 添加类型转换器
abstract class NoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        // 迁移：从版本 3 到版本 4，添加 imageUris 列
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN imageUris TEXT DEFAULT '[]' NOT NULL") // 添加新列，默认值设为 '[]' (空列表的 JSON)
            }
        }

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "note_database"
                )
                .addMigrations(MIGRATION_3_4) // 添加迁移 MIGRATION_3_4
                // 如果需要回退策略（不推荐用于生产）
                // .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
