#!/bin/bash
FILE="src/main/java/com/example/ui/screens/ChatScreen.kt"

# Update items
perl -i -0777 -pe 's/(items\(messages, key = \{ it\.id \}\) \{ msg ->\n\s*)(ChatMessageBubble\(message = msg, currentActiveSender = activeSender\))/$1ChatMessageBubble(message = msg, currentActiveSender = activeSender, modifier = Modifier.animateItem())/g' "$FILE"

# Add empty state
perl -i -0777 -pe 's/(items\(messages, key = \{ it\.id \}\) \{ msg ->)/if (messages.isEmpty() && !isAiThinking) {\n                item {\n                    Column(\n                        modifier = Modifier\n                            .fillParentMaxSize()\n                            .padding(32.dp),\n                        horizontalAlignment = Alignment.CenterHorizontally,\n                        verticalArrangement = Arrangement.Center\n                    ) {\n                        Icon(\n                            imageVector = Icons.Rounded.ChatBubbleOutline,\n                            contentDescription = null,\n                            modifier = Modifier.size(64.dp),\n                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)\n                        )\n                        Spacer(modifier = Modifier.height(16.dp))\n                        Text(\n                            "Belum ada diskusi",\n                            style = MaterialTheme.typography.titleMedium,\n                            color = MaterialTheme.colorScheme.onSurfaceVariant\n                        )\n                        Text(\n                            "Kirim pesan atau catat transaksi untuk memulai.",\n                            style = MaterialTheme.typography.bodyMedium,\n                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),\n                            textAlign = androidx.compose.ui.text.style.TextAlign.Center\n                        )\n                    }\n                }\n            }\n            $1/g' "$FILE"

# Update ChatMessageBubble definition
perl -i -pe 's/(fun ChatMessageBubble\(message: ChatMessage, currentActiveSender: String)\)/$1, modifier: Modifier = Modifier)/g' "$FILE"

# Apply modifier to Column in ChatMessageBubble
perl -i -0777 -pe 's/(val formattedTime = timeFormat\.format\(Date\(message\.timestamp\)\)\n\n    Column\(\n        modifier = )Modifier\.fillMaxWidth\(\)/$1modifier.fillMaxWidth()/g' "$FILE"

