package com.personalwallet.app.core.parser.rules

import com.personalwallet.app.core.model.TransactionType
import com.personalwallet.app.core.parser.NotificationParser
import com.personalwallet.app.core.parser.ParsedTransaction

/**
 * Aturan regex independen untuk aplikasi DANA
 */
class DanaParserRule : NotificationParser {
    override val targetPackageName = "id.dana"

    // Contoh: "Berhasil bayar Rp15.000 ke Kopi Janji Jiwa pakai DANA"
    private val expenseRegex1 = Regex("bayar Rp\\s*([\\d.,]+)\\s*ke (.+?)(?:\\s*pakai|\\s*berhasil|$)", RegexOption.IGNORE_CASE)
    
    // Contoh: "Pembayaran Rp25.000 di Indomaret berhasil"
    private val expenseRegex2 = Regex("Pembayaran Rp\\s*([\\d.,]+)\\s*di (.+?)(?:\\s*berhasil|$)", RegexOption.IGNORE_CASE)
    
    // Contoh: "Isi saldo DANA sebesar Rp100.000 berhasil"
    private val topUpRegex = Regex("Isi saldo DANA sebesar Rp\\s*([\\d.,]+)", RegexOption.IGNORE_CASE)

    override fun parse(title: String, text: String): ParsedTransaction? {
        val topUpMatch = topUpRegex.find(text)
        if (topUpMatch != null) {
            val amountStr = topUpMatch.groupValues[1].split(",")[0].replace(".", "").replace(" ", "")
            val amount = amountStr.toLongOrNull() ?: return null
            return ParsedTransaction(
                amount = amount,
                merchantOrTitle = "Top Up DANA",
                type = TransactionType.INCOME
            )
        }

        val match1 = expenseRegex1.find(text)
        if (match1 != null) {
            val amountStr = match1.groupValues[1].split(",")[0].replace(".", "").replace(" ", "")
            val merchant = match1.groupValues[2].trim()
            val amount = amountStr.toLongOrNull() ?: return null
            return ParsedTransaction(
                amount = amount,
                merchantOrTitle = merchant,
                type = TransactionType.EXPENSE
            )
        }

        val match2 = expenseRegex2.find(text)
        if (match2 != null) {
            val amountStr = match2.groupValues[1].split(",")[0].replace(".", "").replace(" ", "")
            val merchant = match2.groupValues[2].trim()
            val amount = amountStr.toLongOrNull() ?: return null
            return ParsedTransaction(
                amount = amount,
                merchantOrTitle = merchant,
                type = TransactionType.EXPENSE
            )
        }

        return null
    }
}
