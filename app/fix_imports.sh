#!/bin/bash
sed -i '/import androidx\.compose\.material\.icons\.rounded\.ExitToApp/d' /app/applet/app/src/main/java/com/example/MainActivity.kt
sed -i '/import androidx\.compose\.material\.icons\.rounded\.ReceiptLong/d' /app/applet/app/src/main/java/com/example/ui/screens/RekapScreen.kt
