package com.personalwallet.app.ui.screens.transaction

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

data class AddTransactionUiState(
    val amount: String = "",
    val note: String = "",
    val accountNameInput: String = "", // Teks nama dompet/sumber dana (Bisa diketik manual)
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val category: ExpenseCategoryType = ExpenseCategoryType.DISCRETIONARY,
    val selectedAccountId: Long? = null,
    val accounts: List<AccountEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface AddTransactionEvent {
    data object Success : AddTransactionEvent
    data class ShowError(val message: String) : AddTransactionEvent
}

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<AddTransactionEvent>()
    val uiEvent: SharedFlow<AddTransactionEvent> = _uiEvent

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            accountRepository.getAllAccounts()
                .catch { e ->
                    _uiState.update { it.copy(error = "Gagal memuat akun dompet: ${e.message}") }
                }
                .collect { accounts ->
                    _uiState.update { state ->
                        val defaultAccount = accounts.firstOrNull()
                        val newInput = if (state.accountNameInput.isBlank() && defaultAccount != null) defaultAccount.name else state.accountNameInput
                        val newId = if (state.selectedAccountId == null && defaultAccount != null) defaultAccount.id else state.selectedAccountId
                        state.copy(
                            accounts = accounts,
                            accountNameInput = newInput,
                            selectedAccountId = newId
                        )
                    }
                }
        }
    }

    fun onAmountChange(newAmount: String) {
        if (newAmount.all { it.isDigit() }) {
            _uiState.update { it.copy(amount = newAmount, error = null) }
        }
    }

    fun onNoteChange(newNote: String) {
        _uiState.update { it.copy(note = newNote, error = null) }
    }

    fun onAccountNameChange(name: String) {
        val matchedAccount = _uiState.value.accounts.find { it.name.equals(name.trim(), ignoreCase = true) }
        _uiState.update {
            it.copy(
                accountNameInput = name,
                selectedAccountId = matchedAccount?.id,
                error = null
            )
        }
    }

    fun selectAccount(account: AccountEntity) {
        _uiState.update {
            it.copy(
                accountNameInput = account.name,
                selectedAccountId = account.id,
                error = null
            )
        }
    }

    fun onTransactionTypeChange(type: TransactionType) {
        val newCategory = when (type) {
            TransactionType.EXPENSE -> ExpenseCategoryType.DISCRETIONARY
            TransactionType.INCOME -> ExpenseCategoryType.INCOME_CATEGORY
            TransactionType.TRANSFER -> ExpenseCategoryType.TRANSFER_CATEGORY
        }
        _uiState.update { it.copy(transactionType = type, category = newCategory, error = null) }
    }

    fun onCategoryChange(category: ExpenseCategoryType) {
        _uiState.update { it.copy(category = category, error = null) }
    }

    fun addAmountShortcut(addValue: Long) {
        val current = _uiState.value.amount.toLongOrNull() ?: 0L
        _uiState.update { it.copy(amount = (current + addValue).toString()) }
    }

    fun submitTransaction() {
        val state = uiState.value
        val amount = state.amount.toLongOrNull()
        
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(error = "Nominal tidak boleh kosong atau nol.") }
            return
        }
        
        val accountName = state.accountNameInput.trim()
        if (accountName.isBlank()) {
            _uiState.update { it.copy(error = "Ketik atau pilih sumber dana (contoh: BCA, GoPay, Tunai).") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Cari apakah dompet dengan nama ini sudah ada
                var accountId = state.selectedAccountId
                val existingAccount = state.accounts.find { it.name.equals(accountName, ignoreCase = true) }

                if (existingAccount != null) {
                    accountId = existingAccount.id
                } else {
                    // Jika belum ada, otomatis buat dompet baru di database!
                    val isPayLater = accountName.contains("paylater", ignoreCase = true) || 
                                     accountName.contains("kredit", ignoreCase = true) ||
                                     accountName.contains("spaylater", ignoreCase = true)
                    
                    val walletType = if (isPayLater) WalletType.CREDIT_OR_PAYLATER else WalletType.CASH_OR_DEBIT
                    
                    val newAccount = AccountEntity(
                        name = accountName,
                        walletType = walletType,
                        initialBalance = 0L,
                        currentBalance = 0L
                    )
                    accountId = accountRepository.addAccount(newAccount)
                }

                val newTransaction = TransactionEntity(
                    amount = amount,
                    type = state.transactionType,
                    source = TransactionSource.MANUAL,
                    status = TransactionStatus.CONFIRMED,
                    sourceAccountId = accountId,
                    destinationAccountId = null,
                    merchantOrTitle = state.note.ifBlank { "Transaksi Manual ($accountName)" },
                    timestamp = Instant.now().toEpochMilli(),
                    categoryType = state.category,
                    subCategory = "Manual Input",
                    isLeakRisk = false
                )
                
                val insertedId = transactionRepository.insertTransaction(newTransaction)
                
                transactionRepository.confirmTransaction(
                    transactionId = insertedId,
                    amount = amount,
                    type = state.transactionType,
                    accountId = accountId,
                    destinationAccountId = null
                )

                _uiEvent.emit(AddTransactionEvent.Success)
            } catch (e: Exception) {
                _uiEvent.emit(AddTransactionEvent.ShowError(e.message ?: "Terjadi kesalahan sistem"))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
