#!/bin/bash
FILE="src/main/java/com/example/ui/screens/RekapScreen.kt"
perl -i -0777 -pe 's/(fun TransactionItemCard\(\n\s*transaction: FinancialTransaction,\n\s*onDelete: \(\) -> Unit\n\))/\@Composable\nfun TransactionItemCard(\n    transaction: FinancialTransaction,\n    onDelete: () -> Unit,\n    modifier: Modifier = Modifier\n)/g' "$FILE"

FILE_CHAT="src/main/java/com/example/ui/screens/ChatScreen.kt"
perl -i -pe 's/(import androidx.compose.material.icons.Icons)/$1\nimport androidx.compose.material.icons.rounded.ChatBubbleOutline/g' "$FILE_CHAT"
