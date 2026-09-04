package com.personalwallet.app.ui.screens.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalwallet.app.core.domain.repository.AccountRepository
import com.personalwallet.app.core.domain.repository.TransactionRepository
import com.personalwallet.app.core.model.AccountEntity
import com.personalwallet.app.core.model.ExpenseCategoryType
import com.personalwallet.app.core.model.TransactionEntity
import com.personalwallet.app.core.model.TransactionSource
import com.personalwallet.app.core.model.TransactionStatus
import com.personalwallet.app.core.model.TransactionType
import com.personalwallet.app.core.model.WalletType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class WalletUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val totalCashBalance: Long = 0L,
    val totalPayLaterDebt: Long = 0L,
    val netWorth: Long = 0L,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isSettlementModalOpen: Boolean = false,
    val isAddWalletModalOpen: Boolean = false
)

sealed interface WalletUiEvent {
    data class ShowSnackbar(val message: String) : WalletUiEvent
}

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<WalletUiEvent>()
    val uiEvent: SharedFlow<WalletUiEvent> = _uiEvent

    init {
        loadWallets()
    }

    private fun loadWallets() {
        viewModelScope.launch {
            accountRepository.getAllAccounts()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = "Gagal memuat dompet: ${e.message}") }
                }
                .collect { accounts ->
                    val cashAccounts = accounts.filter { it.walletType == WalletType.CASH_OR_DEBIT }
                    val payLaterAccounts = accounts.filter { it.walletType == WalletType.CREDIT_OR_PAYLATER }
                    
                    val totalCash = cashAccounts.sumOf { it.currentBalance }
                    val totalDebt = payLaterAccounts.sumOf { it.currentBalance }
                    
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            accounts = accounts,
                            totalCashBalance = totalCash,
                            totalPayLaterDebt = totalDebt,
                            netWorth = totalCash - totalDebt,
                            error = null
                        ) 
                    }
                }
        }
    }

    fun openSettlementModal() = _uiState.update { it.copy(isSettlementModalOpen = true) }
    fun closeSettlementModal() = _uiState.update { it.copy(isSettlementModalOpen = false) }

    fun openAddWalletModal() = _uiState.update { it.copy(isAddWalletModalOpen = true) }
    fun closeAddWalletModal() = _uiState.update { it.copy(isAddWalletModalOpen = false) }

    fun addNewWallet(name: String, walletType: WalletType, initialBalance: Long) {
        if (name.isBlank()) {
            viewModelScope.launch { _uiEvent.emit(WalletUiEvent.ShowSnackbar("Nama dompet tidak boleh kosong.")) }
            return
        }

        viewModelScope.launch {
            try {
                val newAccount = AccountEntity(
                    name = name.trim(),
                    walletType = walletType,
                    initialBalance = initialBalance,
                    currentBalance = initialBalance
                )
                accountRepository.addAccount(newAccount)
                _uiState.update { it.copy(isAddWalletModalOpen = false) }
                _uiEvent.emit(WalletUiEvent.ShowSnackbar("Dompet '${name.trim()}' berhasil ditambahkan!"))
            } catch (e: Exception) {
                _uiEvent.emit(WalletUiEvent.ShowSnackbar("Gagal menambah dompet: ${e.message}"))
            }
        }
    }

    /**
     * MEMASUKKAN PELUNASAN PAYLATER SECARA NETRAL (ANTI-DOUBLE COUNTING)
     * Dicatat sebagai TransactionType.TRANSFER dari Kas -> PayLater
     */
    fun processPayLaterSettlement(
        cashAccountId: Long,
        payLaterAccountId: Long,
        amount: Long
    ) {
        if (amount <= 0) {
            viewModelScope.launch { _uiEvent.emit(WalletUiEvent.ShowSnackbar("Nominal pelunasan tidak valid.")) }
            return
        }

        viewModelScope.launch {
            try {
                val payLaterAccount = accountRepository.getAccountById(payLaterAccountId)
                val cashAccount = accountRepository.getAccountById(cashAccountId)
                
                val title = "Pelunasan ${payLaterAccount?.name ?: "PayLater"} via ${cashAccount?.name ?: "Kas"}"

                val settlementTx = TransactionEntity(
                    amount = amount,
                    type = TransactionType.TRANSFER, // Sesuai aturan: Transfer Netral
                    source = TransactionSource.MANUAL,
                    status = TransactionStatus.CONFIRMED,
                    sourceAccountId = cashAccountId,
                    destinationAccountId = payLaterAccountId,
                    merchantOrTitle = title,
                    timestamp = Instant.now().toEpochMilli(),
                    categoryType = ExpenseCategoryType.TRANSFER_CATEGORY,
                    subCategory = "Pelunasan Tagihan",
                    isLeakRisk = false
                )

                val txId = transactionRepository.insertTransaction(settlementTx)
                
                // Eksekusi atomik transfer di Room DB (Kas berkurang, PayLater terpulihkan)
                transactionRepository.confirmTransaction(
                    transactionId = txId,
                    amount = amount,
                    type = TransactionType.TRANSFER,
                    accountId = cashAccountId,
                    destinationAccountId = payLaterAccountId
                )

                _uiState.update { it.copy(isSettlementModalOpen = false) }
                _uiEvent.emit(WalletUiEvent.ShowSnackbar("Pelunasan berhasil dicatat sebagai Transfer Netral!"))
            } catch (e: Exception) {
                _uiEvent.emit(WalletUiEvent.ShowSnackbar("Gagal memproses pelunasan: ${e.message}"))
            }
        }
    }
}
