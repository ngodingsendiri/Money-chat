#!/bin/bash

# Fix FinanceRepository.kt to use Dispatchers.IO
perl -i -0777 -pe 's/import kotlinx\.coroutines\.flow\.first/import kotlinx.coroutines.flow.first\nimport kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.withContext/g' app/src/main/java/com/example/data/repository/FinanceRepository.kt

perl -i -0777 -pe 's/(suspend fun sendMessage\([\s\S]*?\{)/$1\n        withContext(Dispatchers.IO) {/g' app/src/main/java/com/example/data/repository/FinanceRepository.kt
perl -i -0777 -pe 's/(\/\/ NO AUTOMATIC AI CHAT BUBBLE HERE! Chat stays clean between Husband & Wife\.\n    })/$1\n        }/g' app/src/main/java/com/example/data/repository/FinanceRepository.kt

perl -i -0777 -pe 's/(suspend fun askAiInChat\([\s\S]*?\{)\n(.*?)(\n    })/$1\n        return withContext(Dispatchers.IO) {\n$2\n        }\n    }/s' app/src/main/java/com/example/data/repository/FinanceRepository.kt

perl -i -0777 -pe 's/(suspend fun addManualTransaction\([\s\S]*?\{)\n(.*?)(\n    })/$1\n        withContext(Dispatchers.IO) {\n$2\n        }\n    }/s' app/src/main/java/com/example/data/repository/FinanceRepository.kt

perl -i -0777 -pe 's/(suspend fun deleteTransaction\([\s\S]*?\{)\n(.*?)(\n    })/$1\n        withContext(Dispatchers.IO) {\n$2\n        }\n    }/s' app/src/main/java/com/example/data/repository/FinanceRepository.kt

perl -i -0777 -pe 's/(suspend fun clearAllData\([\s\S]*?\{)\n(.*?)(\n    })/$1\n        withContext(Dispatchers.IO) {\n$2\n        }\n    }/s' app/src/main/java/com/example/data/repository/FinanceRepository.kt

# Fix MainViewModel.kt error handling
perl -i -0777 -pe 's/(try \{\n\s*repository\.sendMessage.*?)\n\s*\} finally/$1\n            } catch (e: Exception) {\n                e.printStackTrace()\n            } finally/g' app/src/main/java/com/example/ui/MainViewModel.kt

perl -i -0777 -pe 's/(try \{\n\s*repository\.askAiInChat.*?)\n\s*\} finally/$1\n            } catch (e: Exception) {\n                e.printStackTrace()\n            } finally/g' app/src/main/java/com/example/ui/MainViewModel.kt

perl -i -0777 -pe 's/(try \{\n\s*val currentTrans = transactions\.value.*?)\n\s*\} finally/$1\n            } catch (e: Exception) {\n                e.printStackTrace()\n                _auditReport.value = "Gagal memuat laporan, silakan coba lagi."\n            } finally/g' app/src/main/java/com/example/ui/MainViewModel.kt

perl -i -0777 -pe 's/(repository\.addManualTransaction\(trans\)\n\s*\})/$1/g' app/src/main/java/com/example/ui/MainViewModel.kt
# Wait, add try-catch for addManualTransaction:
perl -i -0777 -pe 's/(repository\.addManualTransaction\(trans\))/\n                try {\n                    $1\n                } catch (e: Exception) {\n                    e.printStackTrace()\n                }/g' app/src/main/java/com/example/ui/MainViewModel.kt

# Wait, add try-catch for deleteTransaction:
perl -i -0777 -pe 's/(repository\.deleteTransaction\(transaction\))/\n            try {\n                $1\n            } catch (e: Exception) {\n                e.printStackTrace()\n            }/g' app/src/main/java/com/example/ui/MainViewModel.kt

# Add try-catch for clearAllData:
perl -i -0777 -pe 's/(repository\.clearAllData\(\))/\n            try {\n                $1\n            } catch (e: Exception) {\n                e.printStackTrace()\n            }/g' app/src/main/java/com/example/ui/MainViewModel.kt


