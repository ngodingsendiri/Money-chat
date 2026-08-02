#!/bin/bash
FILE="src/main/java/com/example/ui/screens/ChatScreen.kt"

# Ask AI Button
perl -i -0777 -pe 's/(trailingIcon = \{\n\s*IconButton\(\n\s*onClick = \{)/$1\n                            enabled = inputText.isNotBlank(),/g' "$FILE"

# Send Button
perl -i -0777 -pe 's/(IconButton\(\n\s*onClick = \{\n\s*if \(inputText.isNotBlank\(\)\) \{\n\s*onSendMessage\(inputText\)\n\s*inputText = ""\n\s*\}\n\s*\},\n\s*modifier = Modifier)/IconButton(\n                        enabled = inputText.isNotBlank(),\n                        onClick = {\n                            if (inputText.isNotBlank()) {\n                                onSendMessage(inputText)\n                                inputText = ""\n                            }\n                        },\n                        modifier = Modifier/g' "$FILE"

