#!/bin/bash
FILE="src/main/java/com/example/ui/screens/ChatScreen.kt"
perl -i -pe 's/Locale\("id", "ID"\)/Locale.forLanguageTag("id-ID")/g' "$FILE"

FILE_REKAP="src/main/java/com/example/ui/screens/RekapScreen.kt"
perl -i -pe 's/Locale\("id", "ID"\)/Locale.forLanguageTag("id-ID")/g' "$FILE_REKAP"
