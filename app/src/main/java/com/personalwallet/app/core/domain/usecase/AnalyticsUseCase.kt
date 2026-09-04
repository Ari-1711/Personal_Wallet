package com.personalwallet.app.core.domain.usecase

import com.personalwallet.app.core.domain.repository.TransactionRepository
import com.personalwallet.app.core.model.PeriodSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class AnalyticsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    /**
     * Mengambil ringkasan bulanan.
     * Logika ini mencerminkan Single Ledger Balance Consistency di mana
     * total pengeluaran dan total pemasukan dihitung independen.
     */
    fun getMonthlySummary(year: Int, month: Int): Flow<PeriodSummary> {
        val startOfMonth = LocalDate.of(year, month, 1)
        val endOfMonth = startOfMonth.plusMonths(1).minusDays(1)
        
        val zoneId = ZoneId.systemDefault()
        val startMillis = startOfMonth.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = endOfMonth.atTime(23, 59, 59).atZone(zoneId).toInstant().toEpochMilli()

        val expenseFlow = transactionRepository.getTotalExpense(startMillis, endMillis)
        val incomeFlow = transactionRepository.getTotalIncome(startMillis, endMillis)

        // TODO: Kita dapat menambahkan perhitungan untuk totalDebtAccumulated 
        // dengan melakukan filter transaksi PayLater/Kredit jika dibutuhkan
        
        return combine(expenseFlow, incomeFlow) { totalExpense, totalIncome ->
            val expense = totalExpense ?: 0L
            val income = totalIncome ?: 0L
            
            PeriodSummary(
                totalExpense = expense,
                totalIncome = income,
                totalDebtAccumulated = 0L, // Implementasi penuh menyusul berdasarkan wallet type
                netBalance = income - expense,
                periodLabel = "${startOfMonth.month.name} $year"
            )
        }
    }
}
