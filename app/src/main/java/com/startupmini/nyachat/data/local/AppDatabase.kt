package com.startupmini.nyachat.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ChatMessage::class, FinancialTransaction::class, PendingOp::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun transactionDao(): TransactionDao
    abstract fun pendingOpDao(): PendingOpDao

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

        // v5 -> v6: antrian operasi cloud yang belum tersinkron (retry offline)
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS pending_ops (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "opType TEXT NOT NULL, " +
                        "payload TEXT NOT NULL, " +
                        "createdAt INTEGER NOT NULL)"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "keuangan_pasutri_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
