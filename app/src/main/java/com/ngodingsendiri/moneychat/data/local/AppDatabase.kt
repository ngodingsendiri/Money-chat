package com.ngodingsendiri.moneychat.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ChatMessage::class, FinancialTransaction::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // v1 -> v2: tambah index timestamp di chat_messages (performa query urut waktu)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_timestamp ON chat_messages(timestamp)")
            }
        }

        // v2 -> v3: kolom cloudId (ID dokumen Firestore) untuk sinkronisasi antar perangkat
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN cloudId TEXT")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_chat_messages_cloudId ON chat_messages(cloudId)")
                db.execSQL("ALTER TABLE financial_transactions ADD COLUMN cloudId TEXT")
            }
        }

        // v3 -> v4: kolom imagePath (path foto lampiran nota belanja di chat)
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN imagePath TEXT")
            }
        }

        // v4 -> v5: balasan (reply), file dokumen (PDF), dan penanda pesan diedit
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN replyToSender TEXT")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN replyToText TEXT")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN filePath TEXT")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN fileName TEXT")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN editedAt INTEGER")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "keuangan_pasutri_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
