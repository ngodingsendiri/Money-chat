#!/bin/bash
FILE="src/main/java/com/example/ui/screens/ChatScreen.kt"

perl -i -0777 -pe 's/(\.clip\(CircleShape\)\n\s*\.background\()IndigoPrimary(\))/$1if (inputText.isNotBlank()) IndigoPrimary else MaterialTheme.colorScheme.surfaceVariant$2/g' "$FILE"

