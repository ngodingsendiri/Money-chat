#!/bin/bash
FILE="src/main/java/com/example/ui/screens/RekapScreen.kt"

# Update items
perl -i -0777 -pe 's/(items\(filteredTransactions, key = \{ it\.id \}\) \{ trans ->\n\s*)(TransactionItemCard\(\n\s*transaction = trans,\n\s*onDelete = \{ onDeleteTransaction\(trans\) \})/$1$2,\n                        modifier = Modifier.animateItem()/g' "$FILE"

# Add empty state
perl -i -0777 -pe 's/(items\(filteredTransactions, key = \{ it\.id \}\) \{ trans ->)/if (filteredTransactions.isEmpty()) {\n                    item {\n                        Column(\n                            modifier = Modifier\n                                .fillMaxWidth()\n                                .padding(32.dp),\n                            horizontalAlignment = Alignment.CenterHorizontally\n                        ) {\n                            Icon(\n                                imageVector = Icons.Rounded.ReceiptLong,\n                                contentDescription = null,\n                                modifier = Modifier.size(64.dp),\n                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)\n                            )\n                            Spacer(modifier = Modifier.height(16.dp))\n                            Text(\n                                "Tidak ada transaksi",\n                                style = MaterialTheme.typography.titleMedium,\n                                color = MaterialTheme.colorScheme.onSurfaceVariant\n                            )\n                        }\n                    }\n                }\n                $1/g' "$FILE"

# Update TransactionItemCard definition
perl -i -pe 's/(fun TransactionItemCard\(\n\s*transaction: FinancialTransaction,\n\s*onDelete: \(\) -> Unit)/$1,\n    modifier: Modifier = Modifier/g' "$FILE"

# Apply modifier to Card in TransactionItemCard
perl -i -0777 -pe 's/(Card\(\n\s*colors = CardDefaults\.cardColors\(containerColor = MaterialTheme\.colorScheme\.surface\),\n\s*elevation = CardDefaults\.cardElevation\(defaultElevation = 1\.dp\),\n\s*shape = RoundedCornerShape\(16\.dp\),\n\s*modifier = )Modifier(\n\s*\.fillMaxWidth\(\)\n\s*\.testTag)/$1modifier$2/g' "$FILE"

