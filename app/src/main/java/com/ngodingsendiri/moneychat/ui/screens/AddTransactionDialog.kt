package com.ngodingsendiri.moneychat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ngodingsendiri.moneychat.R
import com.ngodingsendiri.moneychat.data.local.FinancialTransaction
import com.ngodingsendiri.moneychat.ui.theme.ExpenseRed
import com.ngodingsendiri.moneychat.ui.theme.ExpenseRedLight
import com.ngodingsendiri.moneychat.ui.theme.IncomeGreen
import com.ngodingsendiri.moneychat.ui.theme.IncomeGreenLight
import kotlinx.coroutines.launch

/** Format nominal untuk prefill kolom edit (angka bulat tanpa desimal). */
private fun formatAmountInput(amount: Double): String =
    if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    transaction: FinancialTransaction? = null,
    initialLoggedBy: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (FinancialTransaction) -> Unit
) {
    val isEdit = transaction != null
    var type by remember { mutableStateOf(transaction?.type ?: "PENGELUARAN") }
    var amountText by remember {
        mutableStateOf(transaction?.let { formatAmountInput(it.amount) } ?: "")
    }
    var description by remember { mutableStateOf(transaction?.description ?: "") }
    val loggedBy = remember {
        transaction?.loggedBy ?: initialLoggedBy ?: "Bendahara"
    }

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

    var selectedCategory by remember(transaction?.id) {
        mutableStateOf(
            transaction?.category ?: if (type == "PEMASUKAN") "Gaji & Pemasukan" else "Groceries & Sembako"
        )
    }
    var expandedCategoryMenu by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun dismiss() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )
            }

            Text(
                text = stringResource(if (isEdit) R.string.add_dialog_edit_title else R.string.add_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Type Toggle (Pengeluaran vs Pemasukan) — chip berwarna
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = type == "PENGELUARAN",
                    onClick = {
                        type = "PENGELUARAN"
                        if (selectedCategory == "Gaji & Pemasukan") selectedCategory = "Groceries & Sembako"
                    },
                    label = { Text(stringResource(R.string.add_type_expense), fontWeight = FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ExpenseRedLight,
                        selectedLabelColor = ExpenseRed,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_dialog_expense_chip")
                )

                FilterChip(
                    selected = type == "PEMASUKAN",
                    onClick = {
                        type = "PEMASUKAN"
                        if (selectedCategory != "Gaji & Pemasukan") selectedCategory = "Gaji & Pemasukan"
                    },
                    label = { Text(stringResource(R.string.add_type_income), fontWeight = FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IncomeGreenLight,
                        selectedLabelColor = IncomeGreen,
                    ),
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
                label = { Text(stringResource(R.string.add_amount_label)) },
                placeholder = { Text(stringResource(R.string.add_amount_placeholder)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = isAmountInvalid,
                supportingText = if (isAmountInvalid) {
                    { Text(stringResource(R.string.add_amount_error)) }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_dialog_amount_field"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Description Input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.add_desc_label)) },
                placeholder = { Text(stringResource(R.string.add_desc_placeholder)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_dialog_desc_field"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

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
                    label = { Text(stringResource(R.string.add_category_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoryMenu) },
                    shape = RoundedCornerShape(12.dp),
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

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = ::dismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.add_cancel))
                }

                Button(
                    onClick = {
                        val finalAmount = amountText.toDoubleOrNull() ?: 0.0
                        onConfirm(
                            FinancialTransaction(
                                id = transaction?.id ?: 0,
                                type = type,
                                category = selectedCategory,
                                amount = finalAmount,
                                description = description,
                                loggedBy = loggedBy,
                                timestamp = transaction?.timestamp ?: System.currentTimeMillis(),
                                chatMessageId = transaction?.chatMessageId,
                                cloudId = transaction?.cloudId
                            )
                        )
                        onDismiss()
                    },
                    enabled = description.isNotBlank() && amountText.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_dialog_save_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.add_save), fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
