package com.personalwallet.app.core.model

enum class TransactionType {
    EXPENSE,  // Mengurangi saldo kas atau menambah utang paylater
    INCOME,   // Menambah saldo kas
    TRANSFER  // Pemindahan dana (misal: Pelunasan tagihan PayLater dari Kas)
}

enum class WalletType {
    CASH_OR_DEBIT,       // Rekening Bank, Tunai, Saldo GoPay/ShopeePay biasa
    CREDIT_OR_PAYLATER   // SPayLater, GoPay Pinjam, Kartu Kredit, Kredivo
}

enum class TransactionSource { NOTIFICATION, MANUAL }
enum class TransactionStatus { PENDING, CONFIRMED, REJECTED }

enum class ExpenseCategoryType {
    FIXED,
    VARIABLE_ESSENTIAL,
    DISCRETIONARY,
    INCOME_CATEGORY,
    TRANSFER_CATEGORY // Khusus pelunasan utang / mutasi antar-dompet
}

enum class LeakTriggerType {
    NONE,
    MICRO_EXPENSE,
    ZOMBIE_SUB,
    IMPULSE_BUY,
    LIFESTYLE_CREEP,
    PAYLATER_OVERUSE
}
