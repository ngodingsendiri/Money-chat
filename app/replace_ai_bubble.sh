#!/bin/bash
FILE="src/main/java/com/example/ui/screens/ChatScreen.kt"

perl -i -0777 -pe 's/(fun AiThinkingBubble\(\) \{)/$1\n    val isDark = androidx.compose.foundation.isSystemInDarkTheme()/g' "$FILE"

perl -i -0777 -pe 's/(color = )AiPurpleLight,/$1if (isDark) Color(0xFF331650) else AiPurpleLight,/g' "$FILE"

perl -i -0777 -pe 's/(border = androidx\.compose\.foundation\.BorderStroke\(1\.dp, )AiPurple(\.copy\(alpha = 0\.3f\)\))/$1if (isDark) Color(0xFFD0BCFF) else AiPurple$2/g' "$FILE"

perl -i -0777 -pe 's/(CircularProgressIndicator\([\s\S]*?color = )AiPurple,/$1if (isDark) Color(0xFFD0BCFF) else AiPurple,/g' "$FILE"

perl -i -0777 -pe 's/(Text\([\s\S]*?text = "AI sedang memproses\.\.\.",[\s\S]*?color = )AiPurple,/$1if (isDark) Color(0xFFD0BCFF) else AiPurple,/g' "$FILE"

