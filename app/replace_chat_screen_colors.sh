#!/bin/bash
FILE="src/main/java/com/example/ui/screens/ChatScreen.kt"

perl -i -0777 -pe 's/(fun ChatScreen\([\s\S]*?\) \{)/$1\n    val isDark = androidx.compose.foundation.isSystemInDarkTheme()/g' "$FILE"

perl -i -0777 -pe 's/(tint = if \(inputText\.isNotBlank\(\)\) )AiPurple( else )/$1if (isDark) Color(0xFFD0BCFF) else AiPurple$2/g' "$FILE"

perl -i -0777 -pe 's/(\.background\(if \(inputText\.isNotBlank\(\)\) )IndigoPrimary( else )/$1if (isDark) MaterialTheme.colorScheme.primary else IndigoPrimary$2/g' "$FILE"

perl -i -0777 -pe 's/(tint = if \(inputText\.isNotBlank\(\)\) )Color\.White( else )/$1if (isDark) MaterialTheme.colorScheme.onPrimary else Color.White$2/g' "$FILE"

