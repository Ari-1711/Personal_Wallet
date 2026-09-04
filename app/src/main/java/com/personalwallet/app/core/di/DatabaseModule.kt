package com.personalwallet.app.core.di

import android.content.Context
import androidx.room.Room
import com.personalwallet.app.core.database.WalletDatabase
import com.personalwallet.app.core.database.dao.AccountDao
import com.personalwallet.app.core.database.dao.TransactionDao
import com.personalwallet.app.core.database.dao.UnparsedNotificationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideWalletDatabase(
        @ApplicationContext context: Context
    ): WalletDatabase {
        return Room.databaseBuilder(
            context,
            WalletDatabase::class.java,
            "personal_wallet.db"
        ).build()
    }

    @Provides
    fun provideAccountDao(database: WalletDatabase): AccountDao {
        return database.accountDao()
    }

    @Provides
    fun provideTransactionDao(database: WalletDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    fun provideUnparsedNotificationDao(database: WalletDatabase): UnparsedNotificationDao {
        return database.unparsedNotificationDao()
    }
}
