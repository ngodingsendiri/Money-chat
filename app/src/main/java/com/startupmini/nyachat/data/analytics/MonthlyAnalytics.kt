package com.startupmini.nyachat.data.analytics

import com.startupmini.nyachat.Constants
import com.startupmini.nyachat.data.local.FinancialTransaction
import java.util.Calendar

/** Rekap satu bulan (income/pengeluaran/saldo) untuk analisis bulanan. */
data class MonthlySummary(
    val year: Int,
    val month: Int, // 1..12
    val income: Double,
    val expense: Double
) {
    val balance: Double get() = income - expense
    val label: String get() = "%02d/%d".format(month, year)
}

/** Perhitungan ringkasan bulanan murni — mudah diuji tanpa Android/firebase. */
object MonthlyAnalytics {

    /** Kelompokkan transaksi per bulan, urut bulan terbaru dulu. */
    fun groupByMonth(transactions: List<FinancialTransaction>): List<MonthlySummary> {
        return transactions
            .groupBy { yearMonth(it.timestamp) }
            .map { (ym, list) ->
                MonthlySummary(
                    year = ym.first,
                    month = ym.second,
                    income = list.filter { it.type == Constants.TransactionTypes.INCOME }.sumOf { it.amount },
                    expense = list.filter { it.type == Constants.TransactionTypes.EXPENSE }.sumOf { it.amount }
                )
            }
            .sortedBy { it.year * 100 + it.month }
            .reversed()
    }

    /** Kategori teratas pengeluaran pada suatu bulan (untuk rekomendasi fokus). */
    fun topExpenseCategory(month: MonthlySummary, transactions: List<FinancialTransaction>): String? {
        return transactions
            .filter { it.type == Constants.TransactionTypes.EXPENSE && yearMonth(it.timestamp) == (month.year to month.month) }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .maxByOrNull { it.value }
            ?.key
    }

    /** Apakah [timestamp] jatuh pada tahun/[month] (bulan 1..12) yang diminta. */
    fun isSameMonth(timestamp: Long, year: Int, month: Int): Boolean =
        yearMonth(timestamp) == (year to month)

    private fun yearMonth(timestamp: Long): Pair<Int, Int> {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return cal.get(Calendar.YEAR) to (cal.get(Calendar.MONTH) + 1)
    }
}
