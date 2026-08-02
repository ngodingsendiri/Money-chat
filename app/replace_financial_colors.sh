#!/bin/bash
FILE="src/main/java/com/example/ui/screens/ChatScreen.kt"

perl -i -0777 -pe 's/(val tagBg = if \(isIncome\) )IncomeGreenLight( else )ExpenseRedLight/$1if (isDark) Color(0xFF0F5223) else IncomeGreenLight$2if (isDark) Color(0xFF8C1D18) else ExpenseRedLight/g' "$FILE"

perl -i -0777 -pe 's/(val tagColor = if \(isIncome\) )IncomeGreen( else )ExpenseRed/$1if (isDark) Color(0xFFC4EED0) else IncomeGreen$2if (isDark) Color(0xFFF9DEDC) else ExpenseRed/g' "$FILE"

