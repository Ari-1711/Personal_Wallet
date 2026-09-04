package com.personalwallet.app.ui.screens.draft

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalwallet.app.core.database.dao.UnparsedNotificationDao
import com.personalwallet.app.core.domain.repository.AccountRepository
import com.personalwallet.app.core.domain.repository.TransactionRepository
import com.personalwallet.app.core.model.AccountEntity
import com.personalwallet.app.core.model.TransactionEntity
import com.personalwallet.app.core.model.UnparsedNotificationEntity
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
    private val unparsedNotificationDao: UnparsedNotificationDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(DraftUiState())
    val uiState: StateFlow<DraftUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<DraftUiEvent>()
    val uiEvent: SharedFlow<DraftUiEvent> = _uiEvent

    init {
        loadData()
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
                
                // Jika belum ada dompet terikat, gunakan dompet pertama jika ada
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
                
                _uiEvent.emit(DraftUiEvent.ShowSnackbar("Draft dikonfirmasi & saldo tersinkronisasi!"))
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
