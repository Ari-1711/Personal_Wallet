package com.personalwallet.app.core.domain.repository

import com.personalwallet.app.core.model.AccountEntity
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAllAccounts(): Flow<List<AccountEntity>>
    suspend fun getAccountById(accountId: Long): AccountEntity?
    suspend fun addAccount(account: AccountEntity): Long
    suspend fun updateAccount(account: AccountEntity)
    suspend fun adjustBalance(accountId: Long, amount: Long)
}
