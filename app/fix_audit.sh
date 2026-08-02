#!/bin/bash

perl -i -0777 -pe 's/(try \{\n\s*val currentTrans[\s\S]*?_auditReport\.value = report\n\s*)(\} finally)/$1} catch (e: Exception) {\n                e.printStackTrace()\n                _auditReport.value = "Gagal memuat laporan, silakan coba lagi."\n            $2/g' app/src/main/java/com/example/ui/MainViewModel.kt
