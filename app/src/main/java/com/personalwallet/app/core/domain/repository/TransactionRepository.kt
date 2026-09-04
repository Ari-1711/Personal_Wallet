package com.personalwallet.app.core.domain.repository

import com.personalwallet.app.core.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    fun getPendingDrafts(): Flow<List<TransactionEntity>>
    fun getLeakRisks(): Flow<List<TransactionEntity>>
    
    suspend fun insertTransaction(transaction: TransactionEntity): Long
    suspend fun confirmTransaction(transactionId: Long, amount: Long, type: com.personalwallet.app.core.model.TransactionType, accountId: Long, destinationAccountId: Long?)
    suspend fun rejectTransaction(transactionId: Long)
    suspend fun getTransactionByHash(hash: String): TransactionEntity?
    
    fun getTotalExpense(startDate: Long, endDate: Long): Flow<Long?>
    fun getTotalIncome(startDate: Long, endDate: Long): Flow<Long?>
}
