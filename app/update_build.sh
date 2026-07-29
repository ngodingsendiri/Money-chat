#!/bin/bash
FILE="build.gradle.kts"

# Delete the bad line
sed -i '/buildConfigField("String", "GOOGLE_WEB_CLIENT_ID"/d' "$FILE"

# Insert inside defaultConfig
perl -i -0777 -pe 's/(defaultConfig \{[^}]+)\n  \}/$1\n    buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\\\"" + (System.getenv("GOOGLE_WEB_CLIENT_ID") ?: "YOUR_CLIENT_ID_HERE") + "\\\"")\n  }/g' "$FILE"

# Add credentials dependencies
perl -i -0777 -pe 's/(implementation\(libs.firebase.auth\))/$1\n  implementation(libs.androidx.credentials)\n  implementation(libs.androidx.credentials.play.services)\n  implementation(libs.googleid)/g' "$FILE"

