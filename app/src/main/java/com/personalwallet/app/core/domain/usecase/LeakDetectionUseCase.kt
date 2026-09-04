package com.personalwallet.app.core.domain.usecase

import com.personalwallet.app.core.domain.repository.AccountRepository
import com.personalwallet.app.core.domain.repository.TransactionRepository
import com.personalwallet.app.core.model.ExpenseCategoryType
import com.personalwallet.app.core.model.LeakDetectionConfig
import com.personalwallet.app.core.model.LeakTriggerType
import com.personalwallet.app.core.model.TransactionEntity
import com.personalwallet.app.core.model.TransactionStatus
import com.personalwallet.app.core.model.TransactionType
import com.personalwallet.app.core.model.WalletType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class LeakAlert(
    val type: LeakTriggerType,
    val title: String,
    val description: String,
    val actionSuggestion: String,
    val totalAmount: Long,
    val affectedTransactions: List<TransactionEntity> = emptyList()
)

data class LeakAnalyticsReport(
    val alerts: List<LeakAlert> = emptyList(),
    val totalLeakAmount: Long = 0L,
    val payLaterDiscretionaryRatio: Float = 0f,
    val isPayLaterOverused: Boolean = false,
    val allLeakTransactions: List<TransactionEntity> = emptyList()
)

class LeakDetectionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    private val config = LeakDetectionConfig()

    /**
     * Evaluasi menyeluruh 5 aturan deteksi kebocoran keuangan secara offline
     * Sesuai aturan Performance: Berjalan di Dispatchers.Default
     */
    suspend fun evaluateSpendingLeaks(year: Int, month: Int): LeakAnalyticsReport = withContext(Dispatchers.Default) {
        val zoneId = ZoneId.systemDefault()
        val startOfMonth = LocalDate.of(year, month, 1)
        val endOfMonth = startOfMonth.plusMonths(1).minusDays(1)
        val startMillis = startOfMonth.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = endOfMonth.atTime(23, 59, 59).atZone(zoneId).toInstant().toEpochMilli()

        val allTransactions = transactionRepository.getAllTransactions().firstOrNull() ?: emptyList()
        val confirmedMonthTransactions = allTransactions.filter { 
            it.status == TransactionStatus.CONFIRMED &&
            it.timestamp in startMillis..endMillis 
        }

        val accounts = accountRepository.getAllAccounts().firstOrNull() ?: emptyList()
        val creditWalletIds = accounts.filter { it.walletType == WalletType.CREDIT_OR_PAYLATER }.map { it.id }.toSet()

        val alerts = mutableListOf<LeakAlert>()

        // -----------------------------------------------------------------
        // RULE 1: The Micro-Expense Leak (Latte Factor)
        // -----------------------------------------------------------------
        val microExpenses = confirmedMonthTransactions.filter {
            it.type == TransactionType.EXPENSE &&
            it.categoryType == ExpenseCategoryType.DISCRETIONARY &&
            it.amount < config.thresholdMicroExpenseAmount
        }

        if (microExpenses.size >= config.thresholdMicroExpenseWeeklyCount) {
            val totalMicroAmount = microExpenses.sumOf { it.amount }
            alerts.add(
                LeakAlert(
                    type = LeakTriggerType.MICRO_EXPENSE,
                    title = "Bocor Halus (Latte Factor)",
                    description = "${microExpenses.size}x pengeluaran kecil berulang < Rp 25.000 dalam bulan ini.",
                    actionSuggestion = "Pertimbangkan membawa botol minum/kopi sendiri dari rumah untuk menghemat hingga Rp ${totalMicroAmount}.",
                    totalAmount = totalMicroAmount,
                    affectedTransactions = microExpenses
                )
            )
        }

        // -----------------------------------------------------------------
        // RULE 2: Zombie Subscriptions (Langganan Pasif)
        // -----------------------------------------------------------------
        val subscriptionKeywords = listOf("netflix", "spotify", "gym", "youtube", "icloud", "google", "patreon", "disney", "prime")
        val zombieSubs = confirmedMonthTransactions.filter { tx ->
            tx.type == TransactionType.EXPENSE &&
            (tx.subCategory.equals("ENTERTAINMENT_SUBSCRIPTIONS", ignoreCase = true) ||
             subscriptionKeywords.any { tx.merchantOrTitle.lowercase().contains(it) })
        }

        if (zombieSubs.isNotEmpty()) {
            val totalSubAmount = zombieSubs.sumOf { it.amount }
            alerts.add(
                LeakAlert(
                    type = LeakTriggerType.ZOMBIE_SUB,
                    title = "Zombie Subscriptions",
                    description = "Terdeteksi ${zombieSubs.size} tagihan langganan otomatis digital.",
                    actionSuggestion = "Evaluasi apakah layanan digital ini masih aktif Anda gunakan bulan ini.",
                    totalAmount = totalSubAmount,
                    affectedTransactions = zombieSubs
                )
            )
        }

        // -----------------------------------------------------------------
        // RULE 3: Impulse & Emotional Spikes
        // -----------------------------------------------------------------
        val discretionaryExpenses = confirmedMonthTransactions.filter {
            it.type == TransactionType.EXPENSE && it.categoryType == ExpenseCategoryType.DISCRETIONARY
        }

        val totalDiscretionaryAmount = discretionaryExpenses.sumOf { it.amount }
        val daysInMonth = endOfMonth.dayOfMonth
        val dailyAverage = if (daysInMonth > 0) totalDiscretionaryAmount / daysInMonth else 0L

        val impulseSpikes = discretionaryExpenses.filter { tx ->
            val txDate = Instant.ofEpochMilli(tx.timestamp).atZone(zoneId).toLocalDate()
            val isSpike = tx.amount >= (dailyAverage * config.thresholdImpulseDevMultiplier).toLong() && dailyAverage > 0
            val isVulnerableDate = txDate.dayOfMonth in 1..3 || txDate.dayOfWeek.value >= 6 || txDate.dayOfMonth == 10 || txDate.dayOfMonth == 11
            isSpike && isVulnerableDate
        }

        if (impulseSpikes.isNotEmpty()) {
            val totalImpulseAmount = impulseSpikes.sumOf { it.amount }
            alerts.add(
                LeakAlert(
                    type = LeakTriggerType.IMPULSE_BUY,
                    title = "Impulse & Emotional Spikes",
                    description = "Lonjakan belanja non-esensial $\\ge 2.0\\times$ di atas rata-rata harian pada tanggal gajian/akhir pekan.",
                    actionSuggestion = "Terapkan aturan jeda 24 jam sebelum melakukan pembelian impulsif berikutnya.",
                    totalAmount = totalImpulseAmount,
                    affectedTransactions = impulseSpikes
                )
            )
        }

        // -----------------------------------------------------------------
        // RULE 4: Lifestyle Creep
        // -----------------------------------------------------------------
        val totalExpensesMonth = confirmedMonthTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val discretionaryRatio = if (totalExpensesMonth > 0) totalDiscretionaryAmount.toFloat() / totalExpensesMonth else 0f

        if (discretionaryRatio > 0.40f) { // Jika pengeluaran gaya hidup > 40% total pengeluaran
            alerts.add(
                LeakAlert(
                    type = LeakTriggerType.LIFESTYLE_CREEP,
                    title = "Lifestyle Creep (Inflasi Gaya Hidup)",
                    description = "Porsi belanja gaya hidup mencapai ${(discretionaryRatio * 100).toInt()}% dari total pengeluaran.",
                    actionSuggestion = "Idealnya pos gaya hidup berada di bawah 30% untuk menjaga rasio tabungan yang sehat.",
                    totalAmount = totalDiscretionaryAmount
                )
            )
        }

        // -----------------------------------------------------------------
        // RULE 5: PayLater / Debt Spiral Dependency
        // -----------------------------------------------------------------
        val payLaterDiscretionary = discretionaryExpenses.filter { it.sourceAccountId in creditWalletIds }
        val totalPayLaterDiscretionaryAmount = payLaterDiscretionary.sumOf { it.amount }
        val payLaterRatio = if (totalExpensesMonth > 0) totalPayLaterDiscretionaryAmount.toFloat() / totalExpensesMonth else 0f
        val isPayLaterOverused = payLaterRatio > config.thresholdPayLaterRatio

        if (isPayLaterOverused) {
            alerts.add(
                LeakAlert(
                    type = LeakTriggerType.PAYLATER_OVERUSE,
                    title = "Ketergantungan PayLater High-Risk",
                    description = "${(payLaterRatio * 100).toInt()}% belanja gaya hidup didanai via PayLater/Kredit (Batas aman: 20%).",
                    actionSuggestion = "Utamakan pelunasan tagihan PayLater menggunakan saldo kas likuid sebelum melakukan kredit baru.",
                    totalAmount = totalPayLaterDiscretionaryAmount,
                    affectedTransactions = payLaterDiscretionary
                )
            )
        }

        // Kumpulkan semua transaksi yang kena flag bocor
        val allLeakTx = (microExpenses + zombieSubs + impulseSpikes + payLaterDiscretionary).distinctBy { it.id }
        val totalLeakSum = alerts.sumOf { it.totalAmount }

        LeakAnalyticsReport(
            alerts = alerts,
            totalLeakAmount = totalLeakSum,
            payLaterDiscretionaryRatio = payLaterRatio,
            isPayLaterOverused = isPayLaterOverused,
            allLeakTransactions = allLeakTx
        )
    }
}
