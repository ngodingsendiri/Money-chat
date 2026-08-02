import re

file_path = "app/src/main/java/com/example/ui/MainViewModel.kt"

with open(file_path, "r") as f:
    content = f.read()

content = content.replace("import kotlinx.coroutines.flow.stateIn", "import kotlinx.coroutines.flow.stateIn\nimport kotlinx.coroutines.flow.debounce\nimport kotlinx.coroutines.flow.distinctUntilChanged")

# find the transactions.collect block and add debounce
# wait, I can just replace the whole block

old_block = """        viewModelScope.launch {
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
        }"""

new_block = """        viewModelScope.launch {
            transactions
                .debounce(3000)
                .collect { list ->
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
        }"""

content = content.replace(old_block, new_block)

with open(file_path, "w") as f:
    f.write(content)
