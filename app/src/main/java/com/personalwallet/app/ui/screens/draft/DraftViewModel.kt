package com.personalwallet.app.ui.screens.draft

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalwallet.app.core.database.dao.UnparsedNotificationDao
import com.personalwallet.app.core.domain.matcher.SmartWalletMatcher
import com.personalwallet.app.core.domain.repository.AccountRepository
import com.personalwallet.app.core.domain.repository.TransactionRepository
import com.personalwallet.app.core.model.AccountEntity
import com.personalwallet.app.core.model.ExpenseCategoryType
import com.personalwallet.app.core.model.TransactionEntity
import com.personalwallet.app.core.model.TransactionSource
import com.personalwallet.app.core.model.TransactionStatus
import com.personalwallet.app.core.model.TransactionType
import com.personalwallet.app.core.model.UnparsedNotificationEntity
import com.personalwallet.app.core.parser.ParsedTransaction
import com.personalwallet.app.core.util.NotificationPermissionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DraftTab {
    PENDING_DRAFTS,
    UNPARSED_NOTIFICATIONS
}

data class DraftUiState(
    val selectedTab: DraftTab = DraftTab.PENDING_DRAFTS,
    val pendingDrafts: List<TransactionEntity> = emptyList(),
    val unparsedNotifications: List<UnparsedNotificationEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val isNotificationPermissionGranted: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface DraftUiEvent {
    data class ShowSnackbar(val message: String) : DraftUiEvent
}

@HiltViewModel
class DraftViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val unparsedNotificationDao: UnparsedNotificationDao,
    private val smartWalletMatcher: SmartWalletMatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(DraftUiState())
    val uiState: StateFlow<DraftUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<DraftUiEvent>()
    val uiEvent: SharedFlow<DraftUiEvent> = _uiEvent

    init {
        loadData()
    }

    fun checkPermission(context: Context) {
        val granted = NotificationPermissionHelper.isNotificationListenerEnabled(context)
        _uiState.update { it.copy(isNotificationPermissionGranted = granted) }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            combine(
                transactionRepository.getPendingDrafts(),
                unparsedNotificationDao.getUnresolvedNotifications(),
                accountRepository.getAllAccounts()
            ) { drafts, unparsed, accounts ->
                Triple(drafts, unparsed, accounts)
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, error = "Gagal memuat draft: ${e.message}") }
            }.collect { (drafts, unparsed, accounts) ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        pendingDrafts = drafts,
                        unparsedNotifications = unparsed,
                        accounts = accounts
                    ) 
                }
            }
        }
    }

    fun selectTab(tab: DraftTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun confirmDraft(transaction: TransactionEntity, overrideAccountId: Long?) {
        viewModelScope.launch {
            try {
                var accountId = overrideAccountId ?: transaction.sourceAccountId
                
                if (accountId == 0L) {
                    val firstAccount = _uiState.value.accounts.firstOrNull()
                    if (firstAccount != null) {
                        accountId = firstAccount.id
                    } else {
                        _uiEvent.emit(DraftUiEvent.ShowSnackbar("Harap buat dompet terlebih dahulu sebelum mengonfirmasi."))
                        return@launch
                    }
                }

                transactionRepository.confirmTransaction(
                    transactionId = transaction.id,
                    amount = transaction.amount,
                    type = transaction.type,
                    accountId = accountId,
                    destinationAccountId = transaction.destinationAccountId
                )
                
                _uiEvent.emit(DraftUiEvent.ShowSnackbar("Draft dikonfirmasi & masuk ke Transaksi Terkini!"))
            } catch (e: Exception) {
                _uiEvent.emit(DraftUiEvent.ShowSnackbar("Gagal mengonfirmasi: ${e.message}"))
            }
        }
    }

    fun rejectDraft(transactionId: Long) {
        viewModelScope.launch {
            try {
                transactionRepository.rejectTransaction(transactionId)
                _uiEvent.emit(DraftUiEvent.ShowSnackbar("Draft diabaikan."))
            } catch (e: Exception) {
                _uiEvent.emit(DraftUiEvent.ShowSnackbar("Gagal mengabaikan draft: ${e.message}"))
            }
        }
    }

    /**
     * Konversi notifikasi mentah (Mentah) menjadi Draft Transaksi PENDING dengan ekstraksi nominal presisi
     */
    fun convertUnparsedToDraft(unparsed: UnparsedNotificationEntity) {
        viewModelScope.launch {
            try {
                // Ekstraksi nominal berformat RpXX.XXX secara presisi
                val rpMatch = Regex("Rp\\s*([\\d.,]+)", RegexOption.IGNORE_CASE).find(unparsed.rawText)
                val amountStr = if (rpMatch != null) {
                    rpMatch.groupValues[1].split(",")[0].replace(".", "").replace(" ", "")
                } else {
                    unparsed.rawText.replace(".", "").replace(",", "").filter { it.isDigit() }
                }
                
                val amount = amountStr.toLongOrNull() ?: 0L

                // Ekstraksi nama merchant / keterangan singkat
                val merchantName = if (unparsed.rawText.contains("ke ", ignoreCase = true)) {
                    unparsed.rawText.substringAfter("ke ", "Notifikasi Mentah").substringBefore(" pakai", "Notifikasi Mentah").trim()
                } else {
                    "Notifikasi Mentah (${unparsed.packageName})"
                }

                // Cari dompet pencocokan cerdas
                val matchedAccountId = smartWalletMatcher.findOrCreateMatchingAccountId(
                    unparsed.packageName,
                    ParsedTransaction(amount = amount, merchantOrTitle = merchantName, type = TransactionType.EXPENSE)
                )

                val newDraft = TransactionEntity(
                    amount = amount,
                    type = TransactionType.EXPENSE,
                    source = TransactionSource.NOTIFICATION,
                    status = TransactionStatus.PENDING,
                    sourceAccountId = matchedAccountId,
                    merchantOrTitle = merchantName,
                    timestamp = unparsed.receivedTimestamp,
                    categoryType = ExpenseCategoryType.DISCRETIONARY,
                    subCategory = "Manual Convert",
                    rawNotificationText = unparsed.rawText
                )

                transactionRepository.insertTransaction(newDraft)
                unparsedNotificationDao.markAsResolved(unparsed.id)

                _uiState.update { it.copy(selectedTab = DraftTab.PENDING_DRAFTS) }
                _uiEvent.emit(DraftUiEvent.ShowSnackbar("Notifikasi berhasil diekstrak (Rp $amount) & dipindahkan ke Tab Draft Transaksi!"))
            } catch (e: Exception) {
                _uiEvent.emit(DraftUiEvent.ShowSnackbar("Gagal memindahkan: ${e.message}"))
            }
        }
    }

    fun resolveUnparsed(id: Long) {
        viewModelScope.launch {
            try {
                unparsedNotificationDao.markAsResolved(id)
                _uiEvent.emit(DraftUiEvent.ShowSnackbar("Notifikasi ditandai selesai."))
            } catch (e: Exception) {
                _uiEvent.emit(DraftUiEvent.ShowSnackbar("Gagal memperbarui: ${e.message}"))
            }
        }
    }
}
