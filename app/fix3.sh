#!/bin/bash

# Fix ExitToApp deprecation correctly
FILE_MAIN="src/main/java/com/example/MainActivity.kt"
sed -i 's/Icons.AutoMirrored.Rounded.ExitToApp/Icons.AutoMirrored.Rounded.ExitToApp/g' "$FILE_MAIN"
perl -i -pe 's/(import androidx.compose.material.icons.Icons)/$1\nimport androidx.compose.material.icons.automirrored.rounded.ExitToApp/g' "$FILE_MAIN"

# Fix ReceiptLong deprecation correctly
FILE_REKAP="src/main/java/com/example/ui/screens/RekapScreen.kt"
sed -i 's/Icons.AutoMirrored.Rounded.ReceiptLong/Icons.AutoMirrored.Rounded.ReceiptLong/g' "$FILE_REKAP"
perl -i -pe 's/(import androidx.compose.material.icons.Icons)/$1\nimport androidx.compose.material.icons.automirrored.rounded.ReceiptLong/g' "$FILE_REKAP"

