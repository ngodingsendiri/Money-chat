package com.startupmini.nyachat.data.remote

import com.startupmini.nyachat.data.local.ChatMessage
import com.startupmini.nyachat.data.local.ChatMessageDao
import com.startupmini.nyachat.data.local.FinancialTransaction
import com.startupmini.nyachat.data.local.TransactionDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Sprint-1 fix:
 * 1) Upsert sync harus menyelesaikan konflik edit berbasis WAKTU
 *    (last-writer-by-time), bukan urutan tibanya snapshot listener.
 * 2) Restore backup harus menghapus dokumen cloud yang tidak ada di backup
 *    ([FirestoreSyncManager.idsAbsentFromBackup] — bagian murninya).
 */
class FirestoreSyncManagerConflictTest {

    // ---------- Fake DAO in-memory (semantik REPLACE + index unik cloudId) ----------

    private class FakeChatMessageDao : ChatMessageDao {
        val store = mutableMapOf<Long, ChatMessage>()
        private var nextId = 1L

        override fun getAllMessages(): Flow<List<ChatMessage>> =
            flowOf(store.values.sortedBy { it.timestamp })

        override suspend fun insertMessage(message: ChatMessage): Long {
            val id = if (message.id == 0L) nextId++ else message.id
            val saved = message.copy(id = id)
            // Index unik cloudId: REPLACE menyingkirkan baris lain dengan cloudId sama.
            store.values.filter { it.id != id && it.cloudId != null && it.cloudId == saved.cloudId }
                .forEach { store.remove(it.id) }
            store[id] = saved
            return id
        }

        override suspend fun updateMessage(message: ChatMessage) { store[message.id] = message }
        override suspend fun getById(id: Long): ChatMessage? = store[id]
        override suspend fun getByCloudId(cloudId: String): ChatMessage? =
            store.values.firstOrNull { it.cloudId == cloudId }
        override suspend fun deleteMessage(id: Long) { store.remove(id) }
        override suspend fun deleteByCloudId(cloudId: String) {
            store.values.removeAll { it.cloudId == cloudId }
        }
        override suspend fun deleteAllMessages() { store.clear() }
    }

    private class FakeTransactionDao : TransactionDao {
        val store = mutableMapOf<Long, FinancialTransaction>()
        private var nextId = 1L

        override fun getAllTransactions(): Flow<List<FinancialTransaction>> =
            flowOf(store.values.sortedByDescending { it.timestamp })

        override suspend fun insertTransaction(transaction: FinancialTransaction): Long {
            val id = if (transaction.id == 0L) nextId++ else transaction.id
            val saved = transaction.copy(id = id)
            store.values.filter { it.id != id && it.cloudId != null && it.cloudId == saved.cloudId }
                .forEach { store.remove(it.id) }
            store[id] = saved
            return id
        }

        override suspend fun updateTransaction(transaction: FinancialTransaction) { store[transaction.id] = transaction }
        override suspend fun deleteTransaction(transaction: FinancialTransaction) { store.remove(transaction.id) }
        override suspend fun getByCloudId(cloudId: String): FinancialTransaction? =
            store.values.firstOrNull { it.cloudId == cloudId }
        override suspend fun getByChatMessageId(chatMessageId: Long): FinancialTransaction? =
            store.values.firstOrNull { it.chatMessageId == chatMessageId }
        override suspend fun deleteByCloudId(cloudId: String) {
            store.values.removeAll { it.cloudId == cloudId }
        }
        override suspend fun deleteAllTransactions() { store.clear() }
    }

    private fun message(cloudId: String, text: String, timestamp: Long, editedAt: Long? = null) =
        ChatMessage(
            id = 1L,
            sender = "Suami",
            messageText = text,
            timestamp = timestamp,
            editedAt = editedAt,
            cloudId = cloudId
        )

    private fun transaction(cloudId: String, amount: Double, timestamp: Long, editedAt: Long? = null) =
        FinancialTransaction(
            id = 1L,
            type = "PENGELUARAN",
            category = "Makanan & Minuman",
            amount = amount,
            description = "kopi",
            loggedBy = "Suami",
            timestamp = timestamp,
            editedAt = editedAt,
            cloudId = cloudId
        )

    // ---------- effectiveSortTime ----------

    @Test
    fun waktuEfektifPakaiEditedAtKalauAda() {
        assertEquals(500L, FirestoreSyncManager.effectiveSortTime(500L, 100L))
    }

    @Test
    fun waktuEfektifJatuhKeTimestampKalauBelumPernahDiedit() {
        assertEquals(100L, FirestoreSyncManager.effectiveSortTime(null, 100L))
    }

    // ---------- Konflik pesan: last-writer-by-time ----------

    @Test
    fun pesanCloudLebihTuaTidakMenimpaEditLokal() = runBlocking {
        val dao = FakeChatMessageDao()
        dao.insertMessage(message("m1", "edit lokal terbaru", timestamp = 100, editedAt = 200))

        FirestoreSyncManager.upsertMessage(
            dao,
            CloudMessage(cloudId = "m1", sender = "Suami", messageText = "versi lama dari cloud", timestamp = 100, editedAt = 150)
        )

        assertEquals("edit lokal terbaru", dao.getByCloudId("m1")?.messageText)
    }

    @Test
    fun pesanCloudLebihBaruMenimpaLokal() = runBlocking {
        val dao = FakeChatMessageDao()
        dao.insertMessage(message("m1", "versi lokal lama", timestamp = 100, editedAt = 150))

        FirestoreSyncManager.upsertMessage(
            dao,
            CloudMessage(cloudId = "m1", sender = "Suami", messageText = "edit terbaru dari perangkat lain", timestamp = 100, editedAt = 200)
        )

        assertEquals("edit terbaru dari perangkat lain", dao.getByCloudId("m1")?.messageText)
    }

    @Test
    fun pesanCloudTanpaEditDibandingTimestampLokal() = runBlocking {
        // Dokumen lama tanpa editedAt: perbandingan jatuh ke timestamp.
        val dao = FakeChatMessageDao()
        dao.insertMessage(message("m1", "versi lokal", timestamp = 300))

        FirestoreSyncManager.upsertMessage(
            dao,
            CloudMessage(cloudId = "m1", sender = "Suami", messageText = "versi cloud tua", timestamp = 100)
        )

        assertEquals("versi lokal", dao.getByCloudId("m1")?.messageText)
    }

    @Test
    fun pesanBaruDariCloudDiinsert() = runBlocking {
        val dao = FakeChatMessageDao()

        FirestoreSyncManager.upsertMessage(
            dao,
            CloudMessage(cloudId = "m-baru", sender = "Istri", messageText = "halo", timestamp = 50)
        )

        assertEquals("halo", dao.getByCloudId("m-baru")?.messageText)
    }

    @Test
    fun waktuSamaDiterimaSupayaKeduaPerangkatKonvergen() = runBlocking {
        val dao = FakeChatMessageDao()
        dao.insertMessage(message("m1", "teks lokal", timestamp = 100, editedAt = 200))

        FirestoreSyncManager.upsertMessage(
            dao,
            CloudMessage(cloudId = "m1", sender = "Suami", messageText = "teks cloud", timestamp = 100, editedAt = 200)
        )

        assertEquals("teks cloud", dao.getByCloudId("m1")?.messageText)
    }

    // ---------- Konflik transaksi: last-writer-by-time ----------

    @Test
    fun transaksiCloudLebihTuaTidakMenimpaEditLokal() = runBlocking {
        val dao = FakeTransactionDao()
        dao.insertTransaction(transaction("t1", amount = 30000.0, timestamp = 100, editedAt = 200))

        FirestoreSyncManager.upsertTransaction(
            dao,
            CloudTransaction(cloudId = "t1", type = "PENGELUARAN", category = "Makanan & Minuman",
                amount = 20000.0, description = "versi lama", loggedBy = "Suami", timestamp = 100, editedAt = 150)
        )

        assertEquals(30000.0, dao.getByCloudId("t1")!!.amount, 0.001)
    }

    @Test
    fun transaksiCloudLebihBaruMenimpaLokal() = runBlocking {
        val dao = FakeTransactionDao()
        dao.insertTransaction(transaction("t1", amount = 20000.0, timestamp = 100, editedAt = 150))

        FirestoreSyncManager.upsertTransaction(
            dao,
            CloudTransaction(cloudId = "t1", type = "PENGELUARAN", category = "Makanan & Minuman",
                amount = 35000.0, description = "edit perangkat lain", loggedBy = "Istri", timestamp = 100, editedAt = 250)
        )

        val merged = dao.getByCloudId("t1")!!
        assertEquals(35000.0, merged.amount, 0.001)
        assertEquals(250L, merged.editedAt)
    }

    @Test
    fun transaksiBaruDariCloudMenyimpanEditedAt() = runBlocking {
        val dao = FakeTransactionDao()

        FirestoreSyncManager.upsertTransaction(
            dao,
            CloudTransaction(cloudId = "t-baru", type = "PEMASUKAN", category = "Gaji & Pemasukan",
                amount = 5000000.0, description = "gaji", loggedBy = "Suami", timestamp = 75, editedAt = null)
        )

        val saved = dao.getByCloudId("t-baru")!!
        assertEquals(5000000.0, saved.amount, 0.001)
        assertNull(saved.editedAt)
    }

    // ---------- Restore backup: diff dokumen cloud vs isi backup ----------

    @Test
    fun dokumenDiLuarBackupTeridentifikasiUntukDihapus() {
        val absent = FirestoreSyncManager.idsAbsentFromBackup(
            cloudDocIds = listOf("a", "b", "c"),
            keptCloudIds = setOf("a", "c")
        )
        assertEquals(listOf("b"), absent)
    }

    @Test
    fun backupKosongBerartiSemuaDokumenCloudDihapus() {
        val absent = FirestoreSyncManager.idsAbsentFromBackup(
            cloudDocIds = listOf("a", "b"),
            keptCloudIds = emptySet()
        )
        assertEquals(listOf("a", "b"), absent)
    }

    @Test
    fun cloudKosongTidakAdaYangPerluDihapus() {
        val absent = FirestoreSyncManager.idsAbsentFromBackup(
            cloudDocIds = emptyList(),
            keptCloudIds = setOf("a")
        )
        assertEquals(emptyList<String>(), absent)
    }
}
