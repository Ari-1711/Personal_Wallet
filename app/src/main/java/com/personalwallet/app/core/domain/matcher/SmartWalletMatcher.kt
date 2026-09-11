package com.personalwallet.app.core.domain.matcher

import com.personalwallet.app.core.domain.repository.AccountRepository
import com.personalwallet.app.core.model.AccountEntity
import com.personalwallet.app.core.model.WalletType
import com.personalwallet.app.core.parser.ParsedTransaction
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartWalletMatcher @Inject constructor(
    private val accountRepository: AccountRepository
) {
    /**
     * Mencocokkan notifikasi secara cerdas dengan dompet pengguna di database.
     * Contoh:
     * - Notifikasi Gojek -> Dipasangkan ke dompet "GoPay"
     * - Notifikasi BCA -> Dipasangkan ke dompet "BCA"
     * - Notifikasi ShopeePayLater -> Dipasangkan ke dompet kredit "SPayLater"
     *
     * Jika dompet belum pernah ada di database, fungsi ini akan otomatis membuatkan
     * dompet baru yang sesuai sehingga data tidak pernah error.
     */
    suspend fun findOrCreateMatchingAccountId(
        packageName: String,
        parsedTransaction: ParsedTransaction
    ): Long {
        val existingAccounts = accountRepository.getAllAccounts().firstOrNull() ?: emptyList()
        
        // 1. Tentukan nama dompet sasaran berdasarkan packageName & jenis transaksi
        val targetKeyword = when {
            parsedTransaction.isPayLater -> {
                if (packageName == "com.shopee.id") "SPayLater" else "PayLater"
            }
            packageName == "com.gojek.app" -> "GoPay"
            packageName == "m.banking.bca" || packageName.contains("bca") -> "BCA"
            packageName == "id.dana" -> "DANA"
            packageName == "com.shopee.id" -> "ShopeePay"
            packageName.contains("mandiri") -> "Mandiri"
            packageName.contains("bri") -> "BRI"
            packageName.contains("bni") -> "BNI"
            else -> "Dompet Tunai"
        }

        // 2. Cari di database apakah dompet tersebut sudah pernah dibuat
        val matchedAccount = existingAccounts.find { account ->
            account.name.equals(targetKeyword, ignoreCase = true) ||
            account.name.contains(targetKeyword, ignoreCase = true) ||
            targetKeyword.contains(account.name, ignoreCase = true)
        }

        if (matchedAccount != null) {
            return matchedAccount.id
        }

        // 3. Jika dompet belum pernah ada, buatkan dompet baru secara otomatis
        val isCreditWallet = parsedTransaction.isPayLater || targetKeyword.contains("paylater", ignoreCase = true)
        val walletType = if (isCreditWallet) {
            WalletType.CREDIT_OR_PAYLATER
        } else {
            WalletType.CASH_OR_DEBIT
        }

        val newAccount = AccountEntity(
            name = targetKeyword,
            walletType = walletType,
            initialBalance = 0L,
            currentBalance = 0L
        )

        return accountRepository.addAccount(newAccount)
    }
}
