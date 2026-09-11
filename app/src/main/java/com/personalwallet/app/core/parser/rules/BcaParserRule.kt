package com.personalwallet.app.core.parser.rules

import com.personalwallet.app.core.model.TransactionType
import com.personalwallet.app.core.parser.NotificationParser
import com.personalwallet.app.core.parser.ParsedTransaction

/**
 * Aturan regex independen untuk Bank BCA / m-BCA
 */
class BcaParserRule : NotificationParser {
    override val targetPackageName = "m.banking.bca"

    // Contoh: "m-Transfer: 03/09 14:20:10 Ke 1234567890 a/n INDOMARET Rp 85.000,00 DIBAYAR"
    private val expenseRegex = Regex("a/n (.+?) Rp\\s*([\\d.,]+)", RegexOption.IGNORE_CASE)
    
    // Contoh: "m-Transfer: 03/09 10:15:22 Dari 9876543210 a/n GAJI PT ABC Rp 15.000.000,00 MASUK"
    private val incomeRegex = Regex("Dari .+? a/n (.+?) Rp\\s*([\\d.,]+)\\s*MASUK", RegexOption.IGNORE_CASE)

    override fun parse(title: String, text: String): ParsedTransaction? {
        val incomeMatch = incomeRegex.find(text)
        if (incomeMatch != null) {
            val sender = incomeMatch.groupValues[1].trim()
            val amountRaw = incomeMatch.groupValues[2].split(",")[0].replace(".", "").replace(" ", "")
            val amount = amountRaw.toLongOrNull() ?: return null
            return ParsedTransaction(
                amount = amount,
                merchantOrTitle = "Transfer dari $sender",
                type = TransactionType.INCOME
            )
        }

        val expenseMatch = expenseRegex.find(text)
        if (expenseMatch != null) {
            val recipient = expenseMatch.groupValues[1].trim()
            val amountRaw = expenseMatch.groupValues[2].split(",")[0].replace(".", "").replace(" ", "")
            val amount = amountRaw.toLongOrNull() ?: return null
            return ParsedTransaction(
                amount = amount,
                merchantOrTitle = recipient,
                type = TransactionType.EXPENSE
            )
        }

        return null
    }
}
