#!/bin/bash
FILE="src/main/java/com/example/ui/screens/ChatScreen.kt"

# 1. Add AnimatedVisibility for QuickSuggestionRow
perl -i -0777 -pe 's/(\/\/ Quick Suggestion Chips \(placed above input field\)\n\s*)(QuickSuggestionRow\(\n\s*onSuggestionClicked = \{ inputText = it \}\n\s*\))/$1AnimatedVisibility(visible = inputText.isBlank()) {\n            $2\n        }/g' "$FILE"

# 2. Improve Ask AI button: change container and animate tint/color or just make it look more like a primary action when text is present.
# We will use AnimatedVisibility for Ask AI vs Send Button depending on text?
# Actually, Ask AI is the trailingIcon, Send Button is outside. Wait.
# Let's check how the input field layout is.
