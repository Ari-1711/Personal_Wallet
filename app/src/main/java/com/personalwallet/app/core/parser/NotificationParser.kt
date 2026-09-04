package com.personalwallet.app.core.parser

import com.personalwallet.app.core.model.ExpenseCategoryType
import com.personalwallet.app.core.model.TransactionType

data class ParsedTransaction(
    val amount: Long,
    val merchantOrTitle: String,
    val type: TransactionType,
    val categoryType: ExpenseCategoryType = ExpenseCategoryType.DISCRETIONARY, // Sesuai aturan, default ke pengeluaran diskresioner
    val isPayLater: Boolean = false // Penanda agar UI Draft dapat menyarankan dompet kredit yang sesuai
)

interface NotificationParser {
    val targetPackageName: String
    
    /**
     * Memproses teks notifikasi menjadi objek transaksi.
     * Mengembalikan null jika regex tidak ada yang cocok.
     */
    fun parse(title: String, text: String): ParsedTransaction?
}
