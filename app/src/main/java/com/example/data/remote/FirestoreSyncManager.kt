package com.example.data.remote

import android.util.Log
import com.example.data.local.ChatMessage
import com.example.data.local.FinancialTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirestoreSyncManager {

    private const val TAG = "FirestoreSyncManager"
    private const val COLLECTION_FAMILY = "pasutri_families"
    private var FAMILY_DOC_ID = "keluarga_utama"
    
    fun setWorkspaceId(pin: String) {
        FAMILY_DOC_ID = pin
    }

    val userEmail: String?
        get() = try {
            FirebaseAuth.getInstance().currentUser?.email ?: "sipencil@gmail.com"
        } catch (e: Exception) {
            "sipencil@gmail.com"
        }

    fun isUserSignedIn(): Boolean {
        return try {
            FirebaseAuth.getInstance().currentUser != null
        } catch (e: Exception) {
            false
        }
    }

    suspend fun syncChatMessage(message: ChatMessage) {
        runCatching {
            val firestore = FirebaseFirestore.getInstance()
            val docRef = firestore.collection(COLLECTION_FAMILY)
                .document(FAMILY_DOC_ID)
                .collection("messages")
                .document(message.id.toString())

            val data = mapOf(
                "id" to message.id,
                "sender" to message.sender,
                "messageText" to message.messageText,
                "timestamp" to message.timestamp,
                "isFinancial" to message.isFinancial,
                "detectedAmount" to message.detectedAmount,
                "detectedCategory" to message.detectedCategory,
                "detectedType" to message.detectedType,
                "syncedBy" to userEmail
            )

            docRef.set(data).await()
            Log.d(TAG, "Chat message synced to Firestore: ${message.id}")
        }.onFailure {
            Log.w(TAG, "Firestore message sync bypassed/failed (offline mode active): ${it.message}")
        }
    }

    suspend fun syncTransaction(transaction: FinancialTransaction) {
        runCatching {
            val firestore = FirebaseFirestore.getInstance()
            val docRef = firestore.collection(COLLECTION_FAMILY)
                .document(FAMILY_DOC_ID)
                .collection("transactions")
                .document(transaction.id.toString())

            val data = mapOf(
                "id" to transaction.id,
                "type" to transaction.type,
                "category" to transaction.category,
                "amount" to transaction.amount,
                "description" to transaction.description,
                "loggedBy" to transaction.loggedBy,
                "timestamp" to transaction.timestamp,
                "chatMessageId" to transaction.chatMessageId,
                "syncedBy" to userEmail
            )

            docRef.set(data).await()
            Log.d(TAG, "Transaction synced to Firestore: ${transaction.id}")
        }.onFailure {
            Log.w(TAG, "Firestore transaction sync bypassed/failed (offline mode active): ${it.message}")
        }
    }

    suspend fun deleteTransactionFromCloud(transactionId: Long) {
        runCatching {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection(COLLECTION_FAMILY)
                .document(FAMILY_DOC_ID)
                .collection("transactions")
                .document(transactionId.toString())
                .delete()
                .await()
        }.onFailure {
            Log.w(TAG, "Firestore delete transaction bypassed: ${it.message}")
        }
    }
}
