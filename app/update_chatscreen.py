import re

file_path = "app/src/main/java/com/example/ui/screens/ChatScreen.kt"

with open(file_path, "r") as f:
    content = f.read()

# Update signature
content = content.replace(
"""fun ChatScreen(
    messages: List<ChatMessage>,
    activeSender: String,
    isAiThinking: Boolean,
    onSenderChanged: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onAskAiClicked: (String) -> Unit
)""", 
"""fun ChatScreen(
    messages: List<ChatMessage>,
    activeSender: String,
    isAiThinking: Boolean,
    quickSuggestions: List<String>,
    onSenderChanged: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onAskAiClicked: (String) -> Unit
)""")

# Update QuickSuggestionRow call
content = content.replace(
"""        AnimatedVisibility(visible = inputText.isBlank()) {
            QuickSuggestionRow(
            onSuggestionClicked = { inputText = it }
        )
        }""",
"""        AnimatedVisibility(visible = inputText.isBlank() && quickSuggestions.isNotEmpty()) {
            QuickSuggestionRow(
                suggestions = quickSuggestions,
                onSuggestionClicked = { inputText = it }
            )
        }""")

# Update QuickSuggestionRow definition
content = content.replace(
"""fun QuickSuggestionRow(
    onSuggestionClicked: (String) -> Unit
) {
    val suggestions = listOf(
        "Iuran kas 100.000",
        "Beli ATK 250rb",
        "Bayar tagihan listrik 350.000",
        "Pemasukan kas 5.000.000",
        "Konsumsi 85k"
    )

    Column(""",
"""fun QuickSuggestionRow(
    suggestions: List<String>,
    onSuggestionClicked: (String) -> Unit
) {
    Column(""")

with open(file_path, "w") as f:
    f.write(content)

