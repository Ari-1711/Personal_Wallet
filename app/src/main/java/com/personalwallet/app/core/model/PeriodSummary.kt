package com.personalwallet.app.core.model

data class PeriodSummary(
    val totalExpense: Long,       // Hanya menjumlahkan TransactionType.EXPENSE
    val totalIncome: Long,        // Hanya menjumlahkan TransactionType.INCOME
    val totalDebtAccumulated: Long, // Akumulasi belanja via CREDIT_OR_PAYLATER yang belum lunas
    val netBalance: Long,
    val periodLabel: String
)
