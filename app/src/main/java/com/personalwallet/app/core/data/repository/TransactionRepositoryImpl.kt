package com.personalwallet.app.core.data.repository

import androidx.room.withTransaction
import com.personalwallet.app.core.database.WalletDatabase
import com.personalwallet.app.core.database.dao.AccountDao
import com.personalwallet.app.core.database.dao.TransactionDao
import com.personalwallet.app.core.domain.repository.TransactionRepository
import com.personalwallet.app.core.model.TransactionEntity
import com.personalwallet.app.core.model.TransactionType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val db: WalletDatabase,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    override fun getPendingDrafts(): Flow<List<TransactionEntity>> = transactionDao.getPendingDrafts()

    override fun getLeakRisks(): Flow<List<TransactionEntity>> = transactionDao.getLeakRisks()

    override suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return transactionDao.insertTransaction(transaction)
    }

    // Menggunakan Room Transaction untuk memastikan atomicity
    // Sesuai prinsip Single Ledger Balance Consistency: Perubahan status memengaruhi saldo secara konsisten
    override suspend fun confirmTransaction(
        transactionId: Long,
        amount: Long,
        type: TransactionType,
        accountId: Long,
        destinationAccountId: Long?
    ) {
        db.withTransaction {
            transactionDao.confirmTransaction(transactionId)
            
            when (type) {
                TransactionType.EXPENSE -> {
                    // Pengeluaran: Saldo kas berkurang ATAU utang PayLater bertambah (ditangani dengan nilai amount negatif)
                    accountDao.adjustBalance(accountId, -amount)
                }
                TransactionType.INCOME -> {
                    // Pemasukan: Saldo kas bertambah
                    accountDao.adjustBalance(accountId, amount)
                }
                TransactionType.TRANSFER -> {
                    // Transfer / Pelunasan utang:
                    // 1. Saldo sumber (kas) berkurang
                    accountDao.adjustBalance(accountId, -amount)
                    // 2. Saldo tujuan (kas penerima atau PayLater) bertambah
                    if (destinationAccountId != null) {
                        accountDao.adjustBalance(destinationAccountId, amount)
                    }
                }
            }
        }
    }

    override suspend fun rejectTransaction(transactionId: Long) {
        transactionDao.rejectTransaction(transactionId)
    }

    override suspend fun getTransactionByHash(hash: String): TransactionEntity? {
        return transactionDao.getTransactionByHash(hash)
    }

    override fun getTotalExpense(startDate: Long, endDate: Long): Flow<Long?> {
        return transactionDao.getTotalExpense(startDate, endDate)
    }

    override fun getTotalIncome(startDate: Long, endDate: Long): Flow<Long?> {
        return transactionDao.getTotalIncome(startDate, endDate)
    }
}
