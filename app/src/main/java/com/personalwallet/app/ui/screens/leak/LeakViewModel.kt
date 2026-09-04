package com.personalwallet.app.ui.screens.leak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalwallet.app.core.domain.repository.TransactionRepository
import com.personalwallet.app.core.domain.usecase.LeakAnalyticsReport
import com.personalwallet.app.core.domain.usecase.LeakDetectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class LeakUiState(
    val isLoading: Boolean = true,
    val report: LeakAnalyticsReport = LeakAnalyticsReport(),
    val error: String? = null
)

@HiltViewModel
class LeakViewModel @Inject constructor(
    private val leakDetectionUseCase: LeakDetectionUseCase,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeakUiState())
    val uiState: StateFlow<LeakUiState> = _uiState

    init {
        observeTransactionsAndEvaluate()
    }

    private fun observeTransactionsAndEvaluate() {
        val today = LocalDate.now()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            transactionRepository.getAllTransactions()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = "Gagal memuat analitik: ${e.message}") }
                }
                .collect {
                    // Setiap ada perubahan transaksi di DB, hitung ulang 5 Aturan Bocor Halus di Dispatchers.Default
                    val report = leakDetectionUseCase.evaluateSpendingLeaks(today.year, today.monthValue)
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            report = report,
                            error = null
                        ) 
                    }
                }
        }
    }
}
