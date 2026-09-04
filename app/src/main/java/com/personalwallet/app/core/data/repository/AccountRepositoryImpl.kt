package com.personalwallet.app.core.data.repository

import com.personalwallet.app.core.database.dao.AccountDao
import com.personalwallet.app.core.domain.repository.AccountRepository
import com.personalwallet.app.core.model.AccountEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {

    override fun getAllAccounts(): Flow<List<AccountEntity>> {
        return accountDao.getAllAccounts()
    }

    override suspend fun getAccountById(accountId: Long): AccountEntity? {
        return accountDao.getAccountById(accountId)
    }

    override suspend fun addAccount(account: AccountEntity): Long {
        return accountDao.insertAccount(account)
    }

    override suspend fun updateAccount(account: AccountEntity) {
        accountDao.updateAccount(account)
    }

    override suspend fun adjustBalance(accountId: Long, amount: Long) {
        accountDao.adjustBalance(accountId, amount)
    }
}
