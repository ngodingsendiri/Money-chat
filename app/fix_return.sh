#!/bin/bash
perl -i -0777 -pe 's/(return aiResult\.aiReply\n\s*\})/$1/g' app/src/main/java/com/example/data/repository/FinanceRepository.kt
# Wait, simply removing `return` from `return aiResult.aiReply`
perl -i -0777 -pe 's/return aiResult\.aiReply\n\s*\}/aiResult.aiReply\n        }/g' app/src/main/java/com/example/data/repository/FinanceRepository.kt
