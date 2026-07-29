package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessage
import com.example.data.local.FinancialTransaction
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository

    val messages: StateFlow<List<ChatMessage>>
    val transactions: StateFlow<List<FinancialTransaction>>

    // Sender state
    private val _activeSender = MutableStateFlow("")
    val activeSender: StateFlow<String> = _activeSender.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _auditReport = MutableStateFlow<String?>(null)
    val auditReport: StateFlow<String?> = _auditReport.asStateFlow()

    private val _isAuditLoading = MutableStateFlow(false)
    val isAuditLoading: StateFlow<Boolean> = _isAuditLoading.asStateFlow()

    val totalIncome: StateFlow<Double>
    val totalExpense: StateFlow<Double>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FinanceRepository(db.chatMessageDao(), db.transactionDao())

        messages = repository.allMessages.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        transactions = repository.allTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        totalIncome = transactions.map { list ->
            list.filter { it.type == "PEMASUKAN" }.sumOf { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        totalExpense = transactions.map { list ->
            list.filter { it.type == "PENGELUARAN" }.sumOf { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    }

    fun setSender(sender: String) {
        _activeSender.value = sender
    }

    fun toggleSender() {
        _activeSender.value = if (_activeSender.value == "ISTRI") "SUAMI" else "ISTRI"
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val currentSender = _activeSender.value
        viewModelScope.launch {
            _isAiThinking.value = true
            try {
                repository.sendMessage(currentSender, text.trim())
            } finally {
                _isAiThinking.value = false
            }
        }
    }

    fun askAiInChat(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _isAiThinking.value = true
            try {
                repository.askAiInChat(prompt.trim())
            } finally {
                _isAiThinking.value = false
            }
        }
    }

    fun addManualTransaction(
        type: String,
        category: String,
        amount: Double,
        description: String,
        loggedBy: String
    ) {
        viewModelScope.launch {
            val trans = FinancialTransaction(
                type = type,
                category = category,
                amount = amount,
                description = description,
                loggedBy = loggedBy
            )
            repository.addManualTransaction(trans)
        }
    }

    fun deleteTransaction(transaction: FinancialTransaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun generateAiAuditReport() {
        viewModelScope.launch {
            _isAuditLoading.value = true
            try {
                val currentTrans = transactions.value
                val inc = totalIncome.value
                val exp = totalExpense.value
                val report = repository.generateAuditReport(currentTrans, inc, exp)
                _auditReport.value = report
            } finally {
                _isAuditLoading.value = false
            }
        }
    }

    fun dismissAuditReport() {
        _auditReport.value = null
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }
}
