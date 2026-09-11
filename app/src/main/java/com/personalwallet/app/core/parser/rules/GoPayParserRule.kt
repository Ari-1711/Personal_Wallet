package com.personalwallet.app.core.parser.rules

import com.personalwallet.app.core.model.TransactionType
import com.personalwallet.app.core.parser.NotificationParser
import com.personalwallet.app.core.parser.ParsedTransaction

/**
 * Aturan regex independen untuk aplikasi Gojek / GoPay / GoPayLater
 */
class GoPayParserRule : NotificationParser {
    override val targetPackageName = "com.gojek.app"

    private val expenseRegex = Regex("Pembayaran ke (.+) sebesar Rp([\\d.]+) berhasil", RegexOption.IGNORE_CASE)
    private val topUpRegex = Regex("berhasil top up GoPay sebesar Rp([\\d.]+)", RegexOption.IGNORE_CASE)

    override fun parse(title: String, text: String): ParsedTransaction? {
        val isPayLater = text.contains("paylater", ignoreCase = true) || text.contains("gopaylater", ignoreCase = true)

        val expenseMatch = expenseRegex.find(text)
        if (expenseMatch != null) {
            val merchant = expenseMatch.groupValues[1].trim()
            val amountStr = expenseMatch.groupValues[2].replace(".", "")
            val amount = amountStr.toLongOrNull() ?: return null
            return ParsedTransaction(
                amount = amount,
                merchantOrTitle = merchant,
                type = TransactionType.EXPENSE,
                isPayLater = isPayLater
            )
        }

        val topUpMatch = topUpRegex.find(text)
        if (topUpMatch != null) {
            val amountStr = topUpMatch.groupValues[1].replace(".", "")
            val amount = amountStr.toLongOrNull() ?: return null
            return ParsedTransaction(
                amount = amount,
                merchantOrTitle = "Top Up GoPay",
                type = TransactionType.INCOME,
                isPayLater = false
            )
        }

        return null
    }
}
