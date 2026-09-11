package com.personalwallet.app.core.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.personalwallet.app.core.database.dao.UnparsedNotificationDao
import com.personalwallet.app.core.domain.matcher.SmartWalletMatcher
import com.personalwallet.app.core.domain.repository.TransactionRepository
import com.personalwallet.app.core.model.TransactionEntity
import com.personalwallet.app.core.model.TransactionSource
import com.personalwallet.app.core.model.TransactionStatus
import com.personalwallet.app.core.model.UnparsedNotificationEntity
import com.personalwallet.app.core.parser.NotificationParserEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

@AndroidEntryPoint
class WalletNotificationService : NotificationListenerService() {

    @Inject lateinit var parserEngine: NotificationParserEngine
    @Inject lateinit var transactionRepository: TransactionRepository
    @Inject lateinit var smartWalletMatcher: SmartWalletMatcher
    @Inject lateinit var unparsedNotificationDao: UnparsedNotificationDao

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        
        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getString("android.text") ?: ""
        val timestamp = sbn.postTime

        if (text.isBlank()) return

        scope.launch {
            try {
                // Anti-Duplikasi Hash: SHA256(packageName + rawText + (timestamp / 60000))
                val timeWindow = timestamp / 60000
                val rawInput = "$packageName$text$timeWindow"
                val hash = hashSHA256(rawInput)

                if (transactionRepository.getTransactionByHash(hash) != null) {
                    Log.w("[Core/Parser]", "Duplikasi notifikasi diabaikan (Hash: $hash)")
                    return@launch
                }

                // Teruskan ke Mesin Parser
                val parsed = parserEngine.parse(packageName, title, text)
                
                if (parsed != null) {
                    // Pencocokan Dompet Cerdas (Smart Wallet Matcher)
                    // Mencocokkan nama aplikasi notifikasi (Gojek -> "GoPay", BCA -> "BCA", SPayLater -> "SPayLater")
                    val matchedAccountId = smartWalletMatcher.findOrCreateMatchingAccountId(packageName, parsed)

                    val transaction = TransactionEntity(
                        amount = parsed.amount,
                        type = parsed.type,
                        source = TransactionSource.NOTIFICATION,
                        status = TransactionStatus.PENDING, // Draft-First
                        sourceAccountId = matchedAccountId, // ID Dompet Hasil Pencocokan Cerdas
                        merchantOrTitle = parsed.merchantOrTitle,
                        timestamp = timestamp,
                        categoryType = parsed.categoryType,
                        subCategory = "Auto-Parsed",
                        deduplicationHash = hash,
                        rawNotificationText = text
                    )
                    
                    transactionRepository.insertTransaction(transaction)
                    Log.i("[Core/Parser]", "Berhasil mengekstrak notifikasi $packageName ke dompet ID: $matchedAccountId")
                } else {
                    // Graceful Fallback
                    val unparsed = UnparsedNotificationEntity(
                        packageName = packageName,
                        rawText = text,
                        receivedTimestamp = timestamp
                    )
                    unparsedNotificationDao.insertUnparsedNotification(unparsed)
                    Log.w("[Core/Parser]", "Gagal memproses notifikasi. Teks disimpan ke unparsed fallback.")
                }
            } catch (e: Exception) {
                Log.e("[Core/Parser]", "Terjadi kesalahan sistem saat parsing", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun hashSHA256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
