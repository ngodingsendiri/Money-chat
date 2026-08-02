#!/bin/bash
FILE="src/main/java/com/example/ui/screens/ChatScreen.kt"

perl -i -0777 -pe 's/(trailingIcon = \{\n\s*IconButton\(\n\s*onClick = \{)\n\s*enabled = inputText\.isNotBlank\(\),/$1/g' "$FILE"

perl -i -0777 -pe 's/(trailingIcon = \{\n\s*IconButton\()/$1\n                            enabled = inputText.isNotBlank(),/g' "$FILE"

