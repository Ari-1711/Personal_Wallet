package com.personalwallet.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.personalwallet.app.core.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE status = 'PENDING' ORDER BY timestamp DESC")
    fun getPendingDrafts(): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE is_leak_risk = 1 ORDER BY timestamp DESC")
    fun getLeakRisks(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("UPDATE transactions SET status = 'CONFIRMED' WHERE id = :transactionId")
    suspend fun confirmTransaction(transactionId: Long)
    
    @Query("UPDATE transactions SET status = 'REJECTED' WHERE id = :transactionId")
    suspend fun rejectTransaction(transactionId: Long)

    @Query("SELECT * FROM transactions WHERE deduplication_hash = :hash LIMIT 1")
    suspend fun getTransactionByHash(hash: String): TransactionEntity?
    
    // Analytics Queries
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND status = 'CONFIRMED' AND timestamp BETWEEN :startDate AND :endDate")
    fun getTotalExpense(startDate: Long, endDate: Long): Flow<Long?>
    
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND status = 'CONFIRMED' AND timestamp BETWEEN :startDate AND :endDate")
    fun getTotalIncome(startDate: Long, endDate: Long): Flow<Long?>
}
