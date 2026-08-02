#!/bin/bash
FILE="src/main/java/com/example/ui/screens/ChatScreen.kt"

# Add imePadding to the root Column of ChatScreen
perl -i -0777 -pe 's/(Column\(\n\s*modifier = Modifier\n\s*\.fillMaxSize\(\)\n\s*\.background\(MaterialTheme\.colorScheme\.background\))/$1\n            .imePadding()/g' "$FILE"

# Make sure Modifier.imePadding is imported
perl -i -pe 's/(import androidx\.compose\.foundation\.layout\.padding)/$1\nimport androidx.compose.foundation.layout.imePadding/g' "$FILE"

# Wrap QuickSuggestionRow in AnimatedVisibility
perl -i -0777 -pe 's/(\/\/ Quick Suggestion Chips \(placed above input field\)\n\s*)(QuickSuggestionRow\(\n\s*onSuggestionClicked = \{ inputText = it \}\n\s*\))/$1AnimatedVisibility(visible = inputText.isBlank()) {\n            $2\n        }/g' "$FILE"

