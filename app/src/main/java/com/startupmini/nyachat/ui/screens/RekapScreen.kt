package com.startupmini.nyachat.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.HomeWork
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.startupmini.nyachat.Constants
import com.startupmini.nyachat.R
import com.startupmini.nyachat.data.local.FinancialTransaction
import com.startupmini.nyachat.data.remote.SyncStatus
import com.startupmini.nyachat.ui.theme.AiBlue
import com.startupmini.nyachat.ui.theme.CategoryColorsDark
import com.startupmini.nyachat.ui.theme.CategoryColorsLight
import com.startupmini.nyachat.ui.theme.ExpenseRed
import com.startupmini.nyachat.ui.theme.ExpenseRedDark
import com.startupmini.nyachat.ui.theme.ExpenseRedLight
import com.startupmini.nyachat.ui.theme.HusbandBlue
import com.startupmini.nyachat.ui.theme.HusbandBlueDark
import com.startupmini.nyachat.ui.theme.IncomeGreen
import com.startupmini.nyachat.ui.theme.IncomeGreenDark
import com.startupmini.nyachat.ui.theme.IncomeGreenLight
import com.startupmini.nyachat.ui.theme.MoneyTagExpenseDark
import com.startupmini.nyachat.ui.theme.MoneyTagIncomeDark
import com.startupmini.nyachat.ui.theme.WifePink
import com.startupmini.nyachat.ui.theme.WifePinkDark
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Warna kategori dipilih sesuai tema — dipanggil di sisi Composable

@Composable
fun RekapScreen(
    transactions: List<FinancialTransaction>,
    totalIncome: Double,
    totalExpense: Double,
    isAuditLoading: Boolean,
    onGenerateAudit: () -> Unit,
    isMonthlyLoading: Boolean,
    onGenerateMonthly: () -> Unit,
    onAddTransactionClicked: () -> Unit,
    onDeleteTransaction: (FinancialTransaction) -> Unit,
    onEditTransaction: (FinancialTransaction) -> Unit,
    syncStatus: SyncStatus = SyncStatus.SYNCED
) {
    val balance = totalIncome - totalExpense
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val categoryColors = if (isDark) CategoryColorsDark else CategoryColorsLight

    var selectedFilterTab by remember { mutableStateOf(0) } // 0: Semua, 1: Pengeluaran, 2: Pemasukan
    var pendingDelete by remember { mutableStateOf<FinancialTransaction?>(null) }

    val filteredTransactions = remember(transactions, selectedFilterTab) {
        when (selectedFilterTab) {
            1 -> transactions.filter { it.type == Constants.TransactionTypes.EXPENSE }
            2 -> transactions.filter { it.type == Constants.TransactionTypes.INCOME }
            else -> transactions
        }
    }

    val categoryTotals = remember(transactions) {
        transactions.filter { it.type == Constants.TransactionTypes.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 90.dp, top = 16.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Balance Summary Banner Card
            item {
                BalanceBannerCard(
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    balance = balance,
                    syncStatus = syncStatus
                )
            }

            // 2. AI Financial Audit Action Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(AiBlue.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = AiBlue,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.rekap_ai_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.rekap_ai_subtitle),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = onGenerateMonthly,
                                    enabled = !isMonthlyLoading,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("ai_monthly_button")
                                ) {
                                    if (isMonthlyLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            stringResource(R.string.rekap_monthly_action),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = onGenerateAudit,
                                    colors = ButtonDefaults.buttonColors(containerColor = AiBlue),
                                    enabled = !isAuditLoading,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("ai_audit_button")
                                ) {
                                    if (isAuditLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(stringResource(R.string.rekap_ai_action), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Category Breakdown Section (Visual Analytics)
            if (categoryTotals.isNotEmpty()) {
                item {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.rekap_category_header),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Donut Chart here
                                DonutChart(
                                    categoryTotals = categoryTotals,
                                    totalExpense = totalExpense,
                                    colors = categoryColors
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))

                                categoryTotals.forEachIndexed { index, (category, amount) ->
                                    val percentage = if (totalExpense > 0) (amount / totalExpense).toFloat() else 0f
                                    val accentColor = categoryColors[index % categoryColors.size]
                                    
                                    CategoryProgressRow(
                                        category = category,
                                        amount = amount,
                                        percentage = percentage,
                                        accentColor = accentColor
                                    )
                                    if (index < categoryTotals.size - 1) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Transactions Filter & Header Section
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.rekap_history_header),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedButton(
                            onClick = onAddTransactionClicked,
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("manual_add_button")
                        ) {
                            Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.rekap_add), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Custom Segmented Control
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val filterOptions = listOf(
                                0 to stringResource(R.string.filter_all),
                                1 to stringResource(R.string.filter_expense),
                                2 to stringResource(R.string.filter_income)
                            )

                            filterOptions.forEach { (index, label) ->
                                val isSelected = selectedFilterTab == index
                                val segBg by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                    animationSpec = tween(220),
                                    label = "segBg"
                                )
                                val segText by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    animationSpec = tween(220),
                                    label = "segText"
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(segBg)
                                        // P3-2: selectable + Role.Tab — TalkBack mengumumkan
                                        // "Terpilih"/"Tidak terpilih" (sebelumnya hanya clickable
                                        // tanpa status pilihan).
                                        .selectable(
                                            selected = isSelected,
                                            role = Role.Tab,
                                            onClick = { selectedFilterTab = index }
                                        )
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = segText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Transactions List or Empty State
            if (filteredTransactions.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.rekap_empty_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.rekap_empty_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredTransactions, key = { it.id }) { trans ->
                    TransactionItemCard(
                        transaction = trans,
                        onDelete = { pendingDelete = trans },
                        onEdit = { onEditTransaction(trans) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        // Konfirmasi hapus transaksi
        pendingDelete?.let { tx ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text(stringResource(R.string.rekap_delete_title)) },
                text = { Text(stringResource(R.string.rekap_delete_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteTransaction(tx)
                            pendingDelete = null
                        }
                    ) {
                        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        // Floating Action Button for Fast Entry
        FloatingActionButton(
            onClick = onAddTransactionClicked,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_add_transaction")
        ) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = stringResource(R.string.rekap_fab_desc))
        }
    }
}

@Composable
fun BalanceBannerCard(
    totalIncome: Double,
    totalExpense: Double,
    balance: Double,
    syncStatus: SyncStatus = SyncStatus.SYNCED
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).apply {
            maximumFractionDigits = 0
        }
    }
    // Warna balance: hijau jika surplus, merah jika defisit, default jika nol
    val balanceColor = when {
        balance > 0 -> if (isDark) IncomeGreenDark else IncomeGreen
        balance < 0 -> if (isDark) ExpenseRedDark else ExpenseRed
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Wallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.balance_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }

                // Indikator sinkronisasi JUJUR (P2-16): hijau = tersinkron, abu = offline,
                // kuning = sinkronisasi sedang berjalan, merah = error. Menunjukkan status
                // sebenarnya, bukan asumsi "selalu tersinkron".
                SyncIndicator(syncStatus = syncStatus)
            }

            // Main Balance Amount
            Text(
                text = currencyFormat.format(balance),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = balanceColor,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            // Income and Expense Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Income Summary
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = IncomeGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.income_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currencyFormat.format(totalIncome),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Expense Summary
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowUpward,
                            contentDescription = null,
                            tint = ExpenseRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.expense_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currencyFormat.format(totalExpense),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncIndicator(syncStatus: SyncStatus) {
    val label = stringResource(
        when (syncStatus) {
            SyncStatus.SYNCED -> R.string.sync_status_synced
            SyncStatus.SYNCING -> R.string.sync_status_syncing
            SyncStatus.OFFLINE -> R.string.sync_status_offline
            SyncStatus.ERROR -> R.string.sync_status_error
        }
    )
    // Dot mode-aware (audit WCAG): light pakai warna gelap (>=4.7:1 di atas putih),
    // dark pakai warna cerah (>=3:1 di atas surface gelap).
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val dotColor = when (syncStatus) {
        SyncStatus.SYNCED -> if (isDark) Color(0xFF34A853) else Color(0xFF188038)
        SyncStatus.SYNCING -> if (isDark) Color(0xFFFBBC04) else Color(0xFF8D6E00)
        SyncStatus.OFFLINE -> if (isDark) Color(0xFF9AA0A6) else Color(0xFF5F6368)
        SyncStatus.ERROR -> if (isDark) Color(0xFFEA4335) else Color(0xFFC5221F)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics { contentDescription = label }
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CategoryProgressRow(
    category: String,
    amount: Double,
    percentage: Float,
    accentColor: Color
) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).apply {
            maximumFractionDigits = 0
        }
    }

    val categoryIcon = getCategoryIcon(category)
    val percentText = (percentage * 100).toInt()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currencyFormat.format(amount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = accentColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$percentText%",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = accentColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TransactionItemCard(
    transaction: FinancialTransaction,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isIncome = transaction.type == Constants.TransactionTypes.INCOME
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).apply {
            maximumFractionDigits = 0
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.forLanguageTag("id-ID")) }

    val amountColor = when {
        isIncome -> if (MaterialTheme.colorScheme.background.luminance() < 0.5f) IncomeGreenDark else IncomeGreen
        else     -> if (MaterialTheme.colorScheme.background.luminance() < 0.5f) ExpenseRedDark else ExpenseRed
    }
    val amountPrefix = if (isIncome) "+ " else "- "

    val loggedByTag = when (transaction.loggedBy) {
        "Bendahara" -> stringResource(R.string.tag_bendahara)
        "Anggota" -> stringResource(R.string.tag_anggota)
        "Ketua" -> stringResource(R.string.tag_ketua)
        "ISTRI" -> stringResource(R.string.tag_istri)
        "SUAMI" -> stringResource(R.string.tag_suami)
        else -> stringResource(R.string.tag_other, transaction.loggedBy)
    }

    // SwipeToDismissBox: swipe kiri → Delete, swipe kanan → Edit
    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    false // false = jangan auto-dismiss, konfirmasi via dialog
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onEdit()
                    false
                }
                else -> false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.35f }
    )

    // Reset state setelah aksi dipicu (biar item tidak menghilang)
    val scope = rememberCoroutineScope()
    LaunchedEffect(swipeState.currentValue) {
        if (swipeState.currentValue != SwipeToDismissBoxValue.Settled) {
            scope.launch { swipeState.reset() }
        }
    }

    SwipeToDismissBox(
        state = swipeState,
        modifier = modifier,
        enableDismissFromStartToEnd = true,  // kanan → Edit
        enableDismissFromEndToStart = true,  // kiri → Delete
        backgroundContent = {
            // Latar belakang yang terungkap saat swipe
            val direction = swipeState.dismissDirection
            val isToDelete = direction == SwipeToDismissBoxValue.EndToStart
            val isToEdit = direction == SwipeToDismissBoxValue.StartToEnd
            val bgColor = when {
                isToDelete -> ExpenseRed.copy(alpha = 0.12f)
                isToEdit -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                else -> Color.Transparent
            }
            val icon = if (isToDelete) Icons.Rounded.Delete else Icons.Rounded.Edit
            val iconTint = if (isToDelete) ExpenseRed else MaterialTheme.colorScheme.primary
            val align = if (isToDelete) Alignment.CenterEnd else Alignment.CenterStart

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor),
                contentAlignment = align
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .size(24.dp)
                )
            }
        }
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("transaction_item_${transaction.id}")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                if (isIncome) {
                                    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) MoneyTagIncomeDark else IncomeGreenLight
                                } else {
                                    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) MoneyTagExpenseDark else ExpenseRedLight
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(transaction.category),
                            contentDescription = null,
                            tint = if (isIncome) {
                                if (MaterialTheme.colorScheme.background.luminance() < 0.5f) IncomeGreenDark else IncomeGreen
                            } else {
                                if (MaterialTheme.colorScheme.background.luminance() < 0.5f) ExpenseRedDark else ExpenseRed
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = transaction.description,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = transaction.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = " · ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = loggedByTag,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (transaction.loggedBy == "ISTRI" || transaction.loggedBy == "Anggota") {
                                    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) WifePinkDark else WifePink
                                } else {
                                    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) HusbandBlueDark else HusbandBlue
                                },
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$amountPrefix${currencyFormat.format(transaction.amount)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = amountColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 150.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dateFormat.format(Date(transaction.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    // ⋮ Menu aksi (audit touch/keyboard): swipe tetap ada untuk power
                    // user, tapi Edit/Hapus juga punya tombol — keyboard (Enter) &
                    // TalkBack bisa menjangkau tanpa gestur swipe.
                    var menuOpen by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreHoriz,
                                contentDescription = stringResource(R.string.rekap_more_actions),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_edit)) },
                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.action_delete),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        Constants.Categories.GROCERIES -> Icons.Rounded.ShoppingCart
        Constants.Categories.FOOD -> Icons.Rounded.Fastfood
        Constants.Categories.UTILITIES -> Icons.Rounded.HomeWork
        Constants.Categories.KIDS -> Icons.Rounded.ShoppingBag
        Constants.Categories.TRANSPORT -> Icons.Rounded.DirectionsCar
        Constants.Categories.HEALTH -> Icons.Rounded.MedicalServices
        Constants.Categories.ENTERTAINMENT -> Icons.Rounded.SportsEsports
        Constants.Categories.SALARY -> Icons.Rounded.Payments
        else -> Icons.Rounded.MoreHoriz
    }
}

@Composable
fun DonutChart(
    categoryTotals: List<Pair<String, Double>>,
    totalExpense: Double,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    if (totalExpense <= 0 || categoryTotals.isEmpty()) return

    // P2-18: ringkasan aksesibel — pembaca layar membacakan proporsi kategori
    // daripada "grafik tanpa deskripsi".
    val chartSummary = categoryTotals
        .joinToString(", ") { (category, amount) ->
            val pct = (amount / totalExpense * 100).toInt()
            "$category $pct persen"
        }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .semantics {
                contentDescription = "Ringkasan pengeluaran per kategori: $chartSummary"
            },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(160.dp)) {
            var startAngle = -90f
            val strokeWidth = 24.dp.toPx()
            // Gap tetap 2° tapi DIPOTONG di slice kecil: `sweepAngle - gap` tidak
            // boleh negatif (drawArc dengan sweep negatif melukis ke arah salah).
            val gap = 2f

            categoryTotals.forEachIndexed { index, (_, amount) ->
                val sweepAngle = ((amount / totalExpense) * 360).toFloat()
                // Skip slice yang hampir tak terlihat (<0.5°) — tapi tetap majukan
                // startAngle sebesar angle aslinya supaya posisi slice berikutnya
                // tetap akurat (bug sebelumnya: startAngle tidak maju saat skip).
                if (sweepAngle >= 0.5f) {
                    val drawnSweep = (sweepAngle - gap).coerceAtLeast(0.5f)
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = drawnSweep,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = strokeWidth,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        ),
                        size = androidx.compose.ui.geometry.Size(size.width, size.height),
                        topLeft = androidx.compose.ui.geometry.Offset(0f, 0f)
                    )
                }
                startAngle += sweepAngle
            }
        }
        
        // Inner text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.donut_total),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.donut_expense),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}