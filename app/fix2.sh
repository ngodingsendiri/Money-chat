#!/bin/bash

# Fix ExitToApp deprecation
FILE_MAIN="src/main/java/com/example/MainActivity.kt"
sed -i 's/Icons.Rounded.ExitToApp/Icons.AutoMirrored.Rounded.ExitToApp/g' "$FILE_MAIN"

# Fix MenuAnchor warning
FILE_ADD="src/main/java/com/example/ui/screens/AddTransactionDialog.kt"
sed -i 's/modifier = Modifier.menuAnchor()/modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)/g' "$FILE_ADD"

# Fix ReceiptLong deprecation
FILE_REKAP="src/main/java/com/example/ui/screens/RekapScreen.kt"
sed -i 's/Icons.Rounded.ReceiptLong/Icons.AutoMirrored.Rounded.ReceiptLong/g' "$FILE_REKAP"

