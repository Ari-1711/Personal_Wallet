package com.personalwallet.app.core.domain.usecase

import com.personalwallet.app.core.domain.repository.TransactionRepository
import com.personalwallet.app.core.model.ExpenseCategoryType
import com.personalwallet.app.core.model.LeakDetectionConfig
import com.personalwallet.app.core.model.LeakTriggerType
import com.personalwallet.app.core.model.TransactionEntity
import com.personalwallet.app.core.model.TransactionType
import com.personalwallet.app.core.model.WalletType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LeakDetectionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    private val config = LeakDetectionConfig()

    /**
     * Dieksekusi secara asinkron setiap kali transaksi baru dikonfirmasi atau dimasukkan
     * untuk mengevaluasi apakah ada kebocoran halus atau anomali.
     */
    suspend fun analyzeTransactionForLeaks(
        newTransaction: TransactionEntity,
        recentTransactions: List<TransactionEntity>, // Misal: 7 hari terakhir
        sourceWalletType: WalletType
    ): LeakTriggerType = withContext(Dispatchers.Default) {
        
        // Aturan 1: The Micro-Expense Leak (Latte Factor)
        if (newTransaction.type == TransactionType.EXPENSE &&
            newTransaction.categoryType == ExpenseCategoryType.DISCRETIONARY &&
            newTransaction.amount < config.thresholdMicroExpenseAmount
        ) {
            val recentMicroCount = recentTransactions.count {
                it.type == TransactionType.EXPENSE &&
                it.categoryType == ExpenseCategoryType.DISCRETIONARY &&
                it.amount < config.thresholdMicroExpenseAmount
            }
            // Jika dalam riwayat (misal 7 hari) sudah melebihi batas, flag transaksi ini
            if (recentMicroCount >= config.thresholdMicroExpenseWeeklyCount) {
                return@withContext LeakTriggerType.MICRO_EXPENSE
            }
        }

        // TODO: Aturan 2: Zombie Subscriptions (Perlu pengecekan merchant/waktu berulang)
        // TODO: Aturan 3: Impulse & Emotional Spikes
        // TODO: Aturan 4: Lifestyle Creep

        // Aturan 5: PayLater / Debt Spiral Dependency
        if (newTransaction.type == TransactionType.EXPENSE &&
            newTransaction.categoryType == ExpenseCategoryType.DISCRETIONARY &&
            sourceWalletType == WalletType.CREDIT_OR_PAYLATER
        ) {
            // Evaluasi lebih lanjut: apakah melebihi rasio bulanan > 20% ?
            // Jika iya, beri flag PAYLATER_OVERUSE
            // Sementara kita kembalikan NONE sampai kalkulasi proporsi diimplementasikan penuh.
        }

        LeakTriggerType.NONE
    }
}
