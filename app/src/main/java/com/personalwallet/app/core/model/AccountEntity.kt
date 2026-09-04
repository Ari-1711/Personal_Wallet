package com.personalwallet.app.core.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String, // Contoh: "BCA", "SPayLater", "Dompet Tunai"
    @ColumnInfo(name = "wallet_type") val walletType: WalletType,
    @ColumnInfo(name = "initial_balance") val initialBalance: Long = 0L, // Saldo modal awal
    @ColumnInfo(name = "current_balance") val currentBalance: Long = 0L // Saldo akumulasi aktif
)
