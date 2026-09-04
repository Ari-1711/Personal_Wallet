package com.personalwallet.app.ui.screens.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personalwallet.app.core.model.AccountEntity
import com.personalwallet.app.core.model.ExpenseCategoryType
import com.personalwallet.app.core.model.TransactionType

@Composable
fun AddTransactionScreen(
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is AddTransactionEvent.Success -> {
                    snackbarHostState.showSnackbar("Transaksi berhasil disimpan!")
                    onNavigateBack()
                }
                is AddTransactionEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    AddTransactionContent(
        uiState = uiState,
        onAmountChange = viewModel::onAmountChange,
        onNoteChange = viewModel::onNoteChange,
        onAccountNameChange = viewModel::onAccountNameChange,
        onSelectAccount = viewModel::selectAccount,
        onTransactionTypeChange = viewModel::onTransactionTypeChange,
        onCategoryChange = viewModel::onCategoryChange,
        onShortcutClick = viewModel::addAmountShortcut,
        onSubmitClick = viewModel::submitTransaction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionContent(
    uiState: AddTransactionUiState,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onAccountNameChange: (String) -> Unit,
    onSelectAccount: (AccountEntity) -> Unit,
    onTransactionTypeChange: (TransactionType) -> Unit,
    onCategoryChange: (ExpenseCategoryType) -> Unit,
    onShortcutClick: (Long) -> Unit,
    onSubmitClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Catat Transaksi Manual", 
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Tipe Transaksi (Pengeluaran vs Pemasukan)
        val transactionOptions = listOf("Pengeluaran", "Pemasukan")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            transactionOptions.forEachIndexed { index, label ->
                val type = if (index == 0) TransactionType.EXPENSE else TransactionType.INCOME
                SegmentedButton(
                    selected = uiState.transactionType == type,
                    onClick = { onTransactionTypeChange(type) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = transactionOptions.size)
                ) {
                    Text(label)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input Nominal
        OutlinedTextField(
            value = uiState.amount,
            onValueChange = onAmountChange,
            label = { Text("Nominal (Rp)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
            isError = uiState.error != null,
            supportingText = if (uiState.error != null) { { Text(uiState.error) } } else null
        )
        
        // Shortcut Nominal (+10rb, +50rb, +100rb)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = false, onClick = { onShortcutClick(10000L) }, label = { Text("+10rb") })
            FilterChip(selected = false, onClick = { onShortcutClick(50000L) }, label = { Text("+50rb") })
            FilterChip(selected = false, onClick = { onShortcutClick(100000L) }, label = { Text("+100rb") })
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input Sumber Dana / Dompet (Bisa diketik manual ATAU dipilih dari akun yang ada)
        var accountExpanded by remember { mutableStateOf(false) }
        val filteredAccounts = uiState.accounts.filter { 
            it.name.contains(uiState.accountNameInput, ignoreCase = true) 
        }

        ExposedDropdownMenuBox(
            expanded = accountExpanded && (filteredAccounts.isNotEmpty() || uiState.accountNameInput.isNotBlank()),
            onExpandedChange = { accountExpanded = !accountExpanded }
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                readOnly = false, // PENTING: Bisa diketik manual!
                value = uiState.accountNameInput,
                onValueChange = { 
                    onAccountNameChange(it)
                    accountExpanded = true
                },
                label = { Text("Sumber Dana / Dompet (Ketik atau Pilih)") },
                placeholder = { Text("Contoh: BCA, GoPay, Tunai, SPayLater") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            )
            
            ExposedDropdownMenu(
                expanded = accountExpanded && (filteredAccounts.isNotEmpty() || uiState.accountNameInput.isNotBlank()),
                onDismissRequest = { accountExpanded = false },
            ) {
                // Tampilkan opsi yang cocok dari database
                filteredAccounts.forEach { account ->
                    DropdownMenuItem(
                        text = { Text("${account.name} (Ada di Database)") },
                        onClick = {
                            onSelectAccount(account)
                            accountExpanded = false
                        }
                    )
                }
                
                // Jika mengetik nama baru yang belum ada di database, beri indikator pembuatan dompet baru
                if (uiState.accountNameInput.isNotBlank() && filteredAccounts.none { it.name.equals(uiState.accountNameInput.trim(), ignoreCase = true) }) {
                    DropdownMenuItem(
                        text = { Text("+ Tambah dompet baru: \"${uiState.accountNameInput.trim()}\"") },
                        onClick = {
                            accountExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input Catatan / Merchant
        OutlinedTextField(
            value = uiState.note,
            onValueChange = onNoteChange,
            label = { Text("Catatan / Nama Merchant") },
            placeholder = { Text("Contoh: Kopi Kenangan, Indomaret") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Hanya tampilkan kategori jika itu PENGELUARAN
        if (uiState.transactionType == TransactionType.EXPENSE) {
            Text(
                text = "Kategori Pengeluaran",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.category == ExpenseCategoryType.DISCRETIONARY,
                    onClick = { onCategoryChange(ExpenseCategoryType.DISCRETIONARY) },
                    label = { Text("Gaya Hidup") }
                )
                FilterChip(
                    selected = uiState.category == ExpenseCategoryType.VARIABLE_ESSENTIAL,
                    onClick = { onCategoryChange(ExpenseCategoryType.VARIABLE_ESSENTIAL) },
                    label = { Text("Kebutuhan") }
                )
                FilterChip(
                    selected = uiState.category == ExpenseCategoryType.FIXED,
                    onClick = { onCategoryChange(ExpenseCategoryType.FIXED) },
                    label = { Text("Tetap") }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onSubmitClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Simpan Transaksi")
            }
        }
    }
}
