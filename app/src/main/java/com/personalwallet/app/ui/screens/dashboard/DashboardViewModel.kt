package com.personalwallet.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personalwallet.app.core.domain.usecase.AnalyticsUseCase
import com.personalwallet.app.core.model.PeriodSummary
import com.personalwallet.app.core.domain.repository.TransactionRepository
import com.personalwallet.app.core.model.TransactionEntity
import com.personalwallet.app.core.model.TransactionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val summary: PeriodSummary? = null,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val analyticsUseCase: AnalyticsUseCase,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        loadCurrentMonthSummary()
        loadRecentTransactions()
    }

    private fun loadRecentTransactions() {
        viewModelScope.launch {
            transactionRepository.getAllTransactions()
                .catch { exception ->
                    _uiState.update { it.copy(error = "Gagal memuat transaksi: ${exception.localizedMessage}") }
                }
                .collect { transactions ->
                    // Ambil maksimal 4 transaksi terbaru yang sudah di-confirm
                    val recent = transactions
                        .filter { it.status == TransactionStatus.CONFIRMED }
                        .take(4)
                    _uiState.update { it.copy(recentTransactions = recent) }
                }
        }
    }

    private fun loadCurrentMonthSummary() {
        val today = LocalDate.now()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            analyticsUseCase.getMonthlySummary(today.year, today.monthValue)
                .catch { exception ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "Gagal memuat ringkasan: ${exception.localizedMessage}"
                        ) 
                    }
                }
                .collect { summary ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            summary = summary,
                            error = null
                        ) 
                    }
                }
        }
    }
}
