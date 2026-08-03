package com.ngodingsendiri.moneychat.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.ngodingsendiri.moneychat.data.local.ChatMessage
import com.ngodingsendiri.moneychat.data.local.ChatMessageDao
import com.ngodingsendiri.moneychat.data.local.FinancialTransaction
import com.ngodingsendiri.moneychat.data.local.TransactionDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Representasi dokumen Firestore (data tanpa id lokal Room). */
data class CloudMessage(
    val cloudId: String = "",
    val sender: String = "",
    val messageText: String = "",
    val timestamp: Long = 0L,
    val isFinancial: Boolean = false,
    val detectedAmount: Double? = null,
    val detectedCategory: String? = null,
    val detectedType: String? = null
)

/** Representasi dokumen Firestore untuk transaksi. */
data class CloudTransaction(
    val cloudId: String = "",
    val type: String = "",
    val category: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val loggedBy: String = "",
    val timestamp: Long = 0L,
    val chatMessageId: Long? = null
)

/**
 * Sinkronisasi dua arah dengan Firestore:
 * - Tulis: setiap pesan/transaksi baru (dan hapus/clear) dipush ke cloud.
 * - Baca: snapshot listener realtime — perubahan dari perangkat lain langsung
 *   dimerge ke Room lokal (offline-first tetap jalan kalau gak ada internet).
 * Workspace diidentifikasi oleh PIN keluarga (document id di koleksi "families").
 */
object FirestoreSyncManager {

    private const val TAG = "FirestoreSync"
    private const val COLLECTION_FAMILIES = "families"

    @Volatile private var familyId: String = ""
    @Volatile private var chatDao: ChatMessageDao? = null
    @Volatile private var transDao: TransactionDao? = null
    @Volatile private var messagesListener: ListenerRegistration? = null
    @Volatile private var transactionsListener: ListenerRegistration? = null

    /**
     * Cek apakah sudah login dengan akun Google (wajib sebelum sync).
     * Login Google dilakukan dari UI (PinConnectScreen) via Credential Manager —
     * di sini kita tidak pernah login anonim lagi.
     */
    fun isSignedIn(): Boolean = FirebaseAuth.getInstance().currentUser != null

    /** Aktifkan sinkronisasi untuk workspace PIN tertentu. */
    fun start(pin: String, chatMessageDao: ChatMessageDao, transactionDao: TransactionDao) {
        familyId = pin
        chatDao = chatMessageDao
        transDao = transactionDao
        stop()
        listenMessages()
        listenTransactions()
        Log.d(TAG, "Cloud sync aktif untuk keluarga: $pin")
    }

    /** Hentikan semua listener (misal saat logout). */
    fun stop() {
        messagesListener?.remove(); messagesListener = null
        transactionsListener?.remove(); transactionsListener = null
    }

    private fun db() = FirebaseFirestore.getInstance()

    private fun messagesRef() =
        db().collection(COLLECTION_FAMILIES).document(familyId).collection("messages")

    private fun transactionsRef() =
        db().collection(COLLECTION_FAMILIES).document(familyId).collection("transactions")

    // ---------- Baca: snapshot listener realtime ----------

    private fun listenMessages() {
        messagesListener = messagesRef().addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Listen messages gagal: ${error.message}")
                return@addSnapshotListener
            }
            snapshot ?: return@addSnapshotListener
            CoroutineScope(Dispatchers.IO).launch {
                val dao = chatDao ?: return@launch
                for (change in snapshot.documentChanges) {
                    val cloud = change.document.toObject(CloudMessage::class.java)
                    try {
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED ->
                                upsertMessage(dao, cloud)
                            DocumentChange.Type.REMOVED -> dao.deleteByCloudId(cloud.cloudId)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Merge pesan gagal: ${e.message}")
                    }
                }
            }
        }
    }

    private fun listenTransactions() {
        transactionsListener = transactionsRef().addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Listen transactions gagal: ${error.message}")
                return@addSnapshotListener
            }
            snapshot ?: return@addSnapshotListener
            CoroutineScope(Dispatchers.IO).launch {
                val dao = transDao ?: return@launch
                for (change in snapshot.documentChanges) {
                    val cloud = change.document.toObject(CloudTransaction::class.java)
                    try {
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED ->
                                upsertTransaction(dao, cloud)
                            DocumentChange.Type.REMOVED -> dao.deleteByCloudId(cloud.cloudId)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Merge transaksi gagal: ${e.message}")
                    }
                }
            }
        }
    }

    private suspend fun upsertMessage(dao: ChatMessageDao, c: CloudMessage) {
        val existing = dao.getByCloudId(c.cloudId)
        val local = if (existing != null) {
            ChatMessage(
                id = existing.id,
                sender = c.sender,
                messageText = c.messageText,
                timestamp = c.timestamp,
                isFinancial = c.isFinancial,
                detectedAmount = c.detectedAmount,
                detectedCategory = c.detectedCategory,
                detectedType = c.detectedType,
                // Foto lampiran (imagePath) TIDAK dikirim ke cloud — file foto
                // hanya ada di perangkat yang mengirimnya. Pertahankan path
                // lokal supaya bubble foto nota tidak hilang saat listener
                // Firestore mem-merge dokumen yang sama (mis. setelah restore).
                imagePath = existing.imagePath,
                cloudId = c.cloudId
            )
        } else {
            ChatMessage(
                sender = c.sender,
                messageText = c.messageText,
                timestamp = c.timestamp,
                isFinancial = c.isFinancial,
                detectedAmount = c.detectedAmount,
                detectedCategory = c.detectedCategory,
                detectedType = c.detectedType,
                cloudId = c.cloudId
            )
        }
        dao.insertMessage(local)
    }

    private suspend fun upsertTransaction(dao: TransactionDao, c: CloudTransaction) {
        val existing = dao.getByCloudId(c.cloudId)
        val local = if (existing != null) {
            FinancialTransaction(
                id = existing.id,
                type = c.type,
                category = c.category,
                amount = c.amount,
                description = c.description,
                loggedBy = c.loggedBy,
                timestamp = c.timestamp,
                chatMessageId = c.chatMessageId,
                cloudId = c.cloudId
            )
        } else {
            FinancialTransaction(
                type = c.type,
                category = c.category,
                amount = c.amount,
                description = c.description,
                loggedBy = c.loggedBy,
                timestamp = c.timestamp,
                chatMessageId = c.chatMessageId,
                cloudId = c.cloudId
            )
        }
        dao.insertTransaction(local)
    }

    // ---------- Tulis: push perubahan lokal ke cloud ----------

    suspend fun syncMessage(message: ChatMessage) {
        val cid = message.cloudId ?: return
        runCatching {
            messagesRef().document(cid).set(
                mapOf(
                    "cloudId" to cid,
                    "sender" to message.sender,
                    "messageText" to message.messageText,
                    "timestamp" to message.timestamp,
                    "isFinancial" to message.isFinancial,
                    "detectedAmount" to message.detectedAmount,
                    "detectedCategory" to message.detectedCategory,
                    "detectedType" to message.detectedType
                )
            ).await()
        }.onFailure { Log.w(TAG, "Sync pesan gagal: ${it.message}") }
    }

    suspend fun deleteMessage(cloudId: String) {
        runCatching { messagesRef().document(cloudId).delete().await() }
            .onFailure { Log.w(TAG, "Hapus pesan cloud gagal: ${it.message}") }
    }

    suspend fun syncTransaction(transaction: FinancialTransaction) {
        val cid = transaction.cloudId ?: return
        runCatching {
            transactionsRef().document(cid).set(
                mapOf(
                    "cloudId" to cid,
                    "type" to transaction.type,
                    "category" to transaction.category,
                    "amount" to transaction.amount,
                    "description" to transaction.description,
                    "loggedBy" to transaction.loggedBy,
                    "timestamp" to transaction.timestamp,
                    "chatMessageId" to transaction.chatMessageId
                )
            ).await()
        }.onFailure { Log.w(TAG, "Sync transaksi gagal: ${it.message}") }
    }

    suspend fun deleteTransaction(cloudId: String) {
        runCatching { transactionsRef().document(cloudId).delete().await() }
            .onFailure { Log.w(TAG, "Hapus transaksi cloud gagal: ${it.message}") }
    }

    /** Hapus semua data workspace keluarga dari cloud. */
    suspend fun clearFamilyData() {
        runCatching {
            val msgs = messagesRef().get().await()
            msgs.documents.forEach { it.reference.delete().await() }
            val trans = transactionsRef().get().await()
            trans.documents.forEach { it.reference.delete().await() }
        }.onFailure { Log.w(TAG, "Bersihkan cloud gagal: ${it.message}") }
    }
}
