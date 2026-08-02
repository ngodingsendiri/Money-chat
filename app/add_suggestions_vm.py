import re

file_path = "app/src/main/java/com/example/ui/MainViewModel.kt"

with open(file_path, "r") as f:
    content = f.read()

# Add quickSuggestions StateFlow
content = content.replace("    val totalExpense: StateFlow<Double>", 
"""    val totalExpense: StateFlow<Double>

    private val _quickSuggestions = MutableStateFlow<List<String>>(emptyList())
    val quickSuggestions: StateFlow<List<String>> = _quickSuggestions.asStateFlow()""")

# Add repository method in FinanceRepository
repo_path = "app/src/main/java/com/example/data/repository/FinanceRepository.kt"
with open(repo_path, "r") as f:
    repo_content = f.read()

new_repo_method = """
    suspend fun getFrequentTransactionSuggestions(transactions: List<FinancialTransaction>): List<String> {
        return GeminiService.generateFrequentTransactionSuggestions(transactions)
    }

    suspend fun generateAuditReport("""

repo_content = repo_content.replace("    suspend fun generateAuditReport(", new_repo_method)

with open(repo_path, "w") as f:
    f.write(repo_content)


# Add update mechanism in MainViewModel
# In init block, we can launch a coroutine to collect transactions, skip initial empty, and update.
init_addition = """
        totalExpense = transactions.map { list ->
            list.filter { it.type == "PENGELUARAN" }.sumOf { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        viewModelScope.launch {
            transactions.collect { list ->
                if (list.isNotEmpty()) {
                    try {
                        _quickSuggestions.value = repository.getFrequentTransactionSuggestions(list)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    _quickSuggestions.value = listOf("Makan siang 25.000", "Bensin 20.000", "Beli token listrik 50.000")
                }
            }
        }
"""

content = content.replace("""        totalExpense = transactions.map { list ->
            list.filter { it.type == "PENGELUARAN" }.sumOf { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)""", init_addition)

with open(file_path, "w") as f:
    f.write(content)

