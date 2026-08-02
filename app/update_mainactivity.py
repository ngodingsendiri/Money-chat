import re

file_path = "app/src/main/java/com/example/MainActivity.kt"

with open(file_path, "r") as f:
    content = f.read()

content = content.replace("val isAuditLoading by viewModel.isAuditLoading.collectAsStateWithLifecycle()", 
"val isAuditLoading by viewModel.isAuditLoading.collectAsStateWithLifecycle()\n                val quickSuggestions by viewModel.quickSuggestions.collectAsStateWithLifecycle()")

content = content.replace("0 -> ChatScreen(", 
"""0 -> ChatScreen(
                                        quickSuggestions = quickSuggestions,""")

with open(file_path, "w") as f:
    f.write(content)
