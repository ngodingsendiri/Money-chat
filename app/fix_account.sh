#!/bin/bash
perl -i -0777 -pe 's/"sipencil\@gmail\.com"/"anonymous\@offline.com"/g' app/src/main/java/com/example/data/remote/FirestoreSyncManager.kt
perl -i -0777 -pe 's/userEmail = "sipencil\@gmail\.com"/userEmail = "user_\$\{Random.nextInt(1000, 9999)\}\@offline.com"/g' app/src/main/java/com/example/ui/screens/PinConnectScreen.kt
perl -i -0777 -pe 's/myName = "Pengguna Google"/myName = "Pengguna"/g' app/src/main/java/com/example/ui/screens/PinConnectScreen.kt
