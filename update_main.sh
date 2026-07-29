#!/bin/bash
FILE="app/src/main/java/com/example/MainActivity.kt"

# Replace top bar container color
sed -i 's/containerColor = IndigoPrimary,/containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),/g' "$FILE"
sed -i 's/titleContentColor = Color.White,/titleContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,/g' "$FILE"
sed -i 's/actionIconContentColor = Color.White/actionIconContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface/g' "$FILE"

# Replace tint = Color.White in TopBar
sed -i 's/tint = Color.White/tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface/g' "$FILE"

# Replace Text colors in TopBar
sed -i 's/color = Color.White.copy(alpha = 0.8f)/color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)/g' "$FILE"

# Add Box with GlowingBackground
sed -i '/if (workspacePin == null || userName == null) {/a \                    Box(modifier = Modifier.fillMaxSize()) {\n                        GlowingBackground()' "$FILE"

# Wrap else
sed -i '/} else {/a \                    Box(modifier = Modifier.fillMaxSize()) {\n                        GlowingBackground()' "$FILE"

# Make Scaffold transparent
sed -i 's/Scaffold(/Scaffold(containerColor = Color.Transparent,/' "$FILE"

# Close the new Box at the end of both blocks
sed -i 's/                    ) {/                    )\n                    }/g' "$FILE" # Only in PinConnectScreen? Wait, let's just use perl or manual replace
