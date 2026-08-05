package com.startupmini.nyachat.data.analytics

import com.startupmini.nyachat.data.local.FinancialTransaction
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthlyAnalyticsTest {

    private fun tx(
        type: String,
        amount: Double,
        category: String = "Lain-lain",
        ts: Long
    ) = FinancialTransaction(
        type = type,
        category = category,
        amount = amount,
        description = "",
        loggedBy = "test",
        timestamp = ts
    )

    private fun ts(year: Int, month: Int, day: Int = 10): Long {
        val cal = Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, 12, 0, 0)
        }
        return cal.timeInMillis
    }

    @Test
    fun `empty list menghasilkan list kosong`() {
        assertTrue(MonthlyAnalytics.groupByMonth(emptyList()).isEmpty())
    }

    @Test
    fun `kelompokkan beberapa transaksi di bulan yang sama`() {
        val t1 = tx("PENGELUARAN", 50_000.0, "Makanan & Minuman", ts(2026, 5))
        val t2 = tx("PENGELUARAN", 30_000.0, "Transportasi", ts(2026, 5))
        val t3 = tx("PEMASUKAN", 5_000_000.0, ts = ts(2026, 5))

        val months = MonthlyAnalytics.groupByMonth(listOf(t1, t2, t3))

        assertEquals(1, months.size)
        val m = months[0]
        assertEquals(2026, m.year)
        assertEquals(5, m.month)
        assertEquals(5_000_000.0, m.income, 0.001)
        assertEquals(80_000.0, m.expense, 0.001)
        assertEquals(4_920_000.0, m.balance, 0.001)
    }

    @Test
    fun `urutan bulan terbaru dulu`() {
        val june = tx("PENGELUARAN", 10_000.0, ts = ts(2026, 6))
        val may = tx("PENGELUARAN", 20_000.0, ts = ts(2026, 5))

        val months = MonthlyAnalytics.groupByMonth(listOf(june, may))

        assertEquals(2, months.size)
        assertEquals(6, months[0].month) // terbaru pertama
        assertEquals(5, months[1].month)
    }

    @Test
    fun `kategori teratas hanya dihitung dari pengeluaran bulan terkait`() {
        val month = MonthlyAnalytics.groupByMonth(
            listOf(
                tx("PENGELUARAN", 100_000.0, "Makanan & Minuman", ts(2026, 5)),
                tx("PENGELUARAN", 200_000.0, "Hiburan & Belanja", ts(2026, 5)),
                tx("PENGELUARAN", 999_999.0, "Hiburan & Belanja", ts(2026, 6))
            )
        ).first()

        val top = MonthlyAnalytics.topExpenseCategory(month, listOf(
            tx("PENGELUARAN", 100_000.0, "Makanan & Minuman", ts(2026, 5)),
            tx("PENGELUARAN", 200_000.0, "Hiburan & Belanja", ts(2026, 5)),
            tx("PENGELUARAN", 999_999.0, "Hiburan & Belanja", ts(2026, 6))
        ))

        assertEquals("Hiburan & Belanja", top)
    }

    @Test
    fun `tidak ada pengeluaran bulan tsb berarti kategori kosong`() {
        val inc = tx("PEMASUKAN", 1_000_000.0, ts = ts(2026, 5))
        val month = MonthlyAnalytics.groupByMonth(listOf(inc)).first()

        assertNull(MonthlyAnalytics.topExpenseCategory(month, listOf(inc)))
    }
}