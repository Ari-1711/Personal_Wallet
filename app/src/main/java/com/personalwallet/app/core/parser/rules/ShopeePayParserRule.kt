package com.personalwallet.app.core.parser.rules

import com.personalwallet.app.core.model.TransactionType
import com.personalwallet.app.core.parser.NotificationParser
import com.personalwallet.app.core.parser.ParsedTransaction

/**
 * Aturan regex independen untuk aplikasi ShopeePay & SPayLater
 */
class ShopeePayParserRule : NotificationParser {
    override val targetPackageName = "com.shopee.id"

    // Contoh: "Pembayaran SPayLater sebesar Rp450.000 berhasil disetujui"
    private val spayLaterRegex = Regex("SPayLater sebesar Rp\\s*([\\d.]+)", RegexOption.IGNORE_CASE)

    // Contoh: "Pembayaran Rp85.000 di ShopeePay berhasil"
    private val shopeePayRegex = Regex("Pembayaran Rp\\s*([\\d.]+)\\s*di (.+?)(?:\\s*berhasil|$)", RegexOption.IGNORE_CASE)

    override fun parse(title: String, text: String): ParsedTransaction? {
        val isPayLater = text.contains("spaylater", ignoreCase = true) || title.contains("spaylater", ignoreCase = true)

        val payLaterMatch = spayLaterRegex.find(text)
        if (payLaterMatch != null) {
            val amountStr = payLaterMatch.groupValues[1].replace(".", "")
            val amount = amountStr.toLongOrNull() ?: return null
            return ParsedTransaction(
                amount = amount,
                merchantOrTitle = "Belanja SPayLater",
                type = TransactionType.EXPENSE,
                isPayLater = true
            )
        }

        val shopeePayMatch = shopeePayRegex.find(text)
        if (shopeePayMatch != null) {
            val amountStr = shopeePayMatch.groupValues[1].replace(".", "")
            val merchant = shopeePayMatch.groupValues[2].trim()
            val amount = amountStr.toLongOrNull() ?: return null
            return ParsedTransaction(
                amount = amount,
                merchantOrTitle = if (merchant.contains("ShopeePay", ignoreCase = true)) "Belanja Shopee" else merchant,
                type = TransactionType.EXPENSE,
                isPayLater = isPayLater
            )
        }

        return null
    }
}
