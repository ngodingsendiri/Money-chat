package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (type: String, category: String, amount: Double, description: String, loggedBy: String) -> Unit
) {
    var type by remember { mutableStateOf("PENGELUARAN") }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var loggedBy by remember { mutableStateOf("Bendahara") }

    val categories = listOf(
        "Groceries & Sembako",
        "Makanan & Minuman",
        "Tagihan & Utilitas",
        "Kebutuhan Anak",
        "Transportasi",
        "Kesehatan & Skincare",
        "Hiburan & Belanja",
        "Lain-lain",
        "Gaji & Pemasukan"
    )

    var selectedCategory by remember(type) {
        mutableStateOf(if (type == "PEMASUKAN") "Gaji & Pemasukan" else "Groceries & Sembako")
    }
    var expandedCategoryMenu by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Catat Transaksi Manual",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Type Toggle (Pengeluaran vs Pemasukan)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == "PENGELUARAN",
                        onClick = {
                            type = "PENGELUARAN"
                            selectedCategory = "Groceries & Sembako"
                        },
                        label = { Text("Pengeluaran") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_dialog_expense_chip")
                    )

                    FilterChip(
                        selected = type == "PEMASUKAN",
                        onClick = {
                            type = "PEMASUKAN"
                            selectedCategory = "Gaji & Pemasukan"
                        },
                        label = { Text("Pemasukan") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_dialog_income_chip")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Amount Input
                val amountVal = amountText.toDoubleOrNull() ?: 0.0
                val isAmountInvalid = amountText.isNotBlank() && amountVal <= 0
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Nominal (Rp)") },
                    placeholder = { Text("Contoh: 150000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isAmountInvalid,
                    supportingText = if (isAmountInvalid) {
                        { Text("Nominal harus lebih dari 0") }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_dialog_amount_field"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Keterangan Transaksi") },
                    placeholder = { Text("Contoh: Belanja bahan pokok harian") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_dialog_desc_field"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedCategoryMenu,
                    onExpandedChange = { expandedCategoryMenu = !expandedCategoryMenu },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoryMenu) },
                        modifier = Modifier
                            .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedCategoryMenu,
                        onDismissRequest = { expandedCategoryMenu = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    expandedCategoryMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Logged By Toggle
                Text(
                    text = "Dicatat Oleh:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val roles = listOf("Bendahara", "Anggota", "Ketua")
                    roles.forEach { r ->
                        FilterChip(
                            selected = loggedBy == r,
                            onClick = { loggedBy = r },
                            label = { Text(r) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Batal")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val amountVal = amountText.toDoubleOrNull() ?: 0.0
                            onConfirm(type, selectedCategory, amountVal, description, loggedBy)
                            onDismiss()
                        },
                        enabled = description.isNotBlank() && amountText.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("add_dialog_save_button")
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}
