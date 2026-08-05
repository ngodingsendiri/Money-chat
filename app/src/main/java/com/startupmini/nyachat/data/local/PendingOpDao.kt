package com.startupmini.nyachat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PendingOpDao {
    @Insert
    suspend fun insert(op: PendingOp): Long

    /** Semua ops, urut dari yang tertua. */
    @Query("SELECT * FROM pending_ops ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingOp>

    @Query("SELECT COUNT(*) FROM pending_ops")
    suspend fun count(): Int

    @Query("DELETE FROM pending_ops WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pending_ops")
    suspend fun deleteAll()
}
