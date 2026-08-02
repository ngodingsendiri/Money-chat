#!/bin/bash
FILE="src/main/java/com/example/ui/screens/ChatScreen.kt"

perl -i -0777 -pe 's/(fun ChatMessageBubble.*?\n\s*val isAi = message\.sender == "AI"\n\s*val isMe = message\.sender == currentActiveSender)/$1\n    val isDark = androidx.compose.foundation.isSystemInDarkTheme()/g' "$FILE"

perl -i -0777 -pe 's/(val bubbleColor = when \{\n\s*isAi -> )AiPurpleLight(\n\s*isMe -> )IndigoPrimary/$1if (isDark) Color(0xFF331650) else AiPurpleLight$2if (isDark) MaterialTheme.colorScheme.primary else IndigoPrimary/g' "$FILE"

perl -i -0777 -pe 's/(val textColor = when \{\n\s*isMe -> )Color\.White/$1if (isDark) MaterialTheme.colorScheme.onPrimary else Color.White/g' "$FILE"

perl -i -0777 -pe 's/(val timeColor = when \{\n\s*isMe -> )Color\.White\.copy\(alpha = 0\.7f\)/$1if (isDark) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f)/g' "$FILE"

perl -i -0777 -pe 's/(val senderColor = when \{\n\s*isMe -> )IndigoPrimary(\n\s*isAi -> )AiPurple/$1if (isDark) MaterialTheme.colorScheme.primary else IndigoPrimary$2if (isDark) Color(0xFFD0BCFF) else AiPurple/g' "$FILE"

