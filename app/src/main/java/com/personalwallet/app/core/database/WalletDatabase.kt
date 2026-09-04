package com.personalwallet.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.personalwallet.app.core.database.dao.AccountDao
import com.personalwallet.app.core.database.dao.TransactionDao
import com.personalwallet.app.core.database.dao.UnparsedNotificationDao
import com.personalwallet.app.core.model.AccountEntity
import com.personalwallet.app.core.model.TransactionEntity
import com.personalwallet.app.core.model.UnparsedNotificationEntity

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        UnparsedNotificationEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class WalletDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun unparsedNotificationDao(): UnparsedNotificationDao
}
