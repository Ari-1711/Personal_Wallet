package com.personalwallet.app.ui.screens.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personalwallet.app.core.model.AccountEntity
import com.personalwallet.app.core.model.WalletType
import com.personalwallet.app.ui.components.ObsidianCard
import com.personalwallet.app.ui.components.ObsidianChipStyle
import com.personalwallet.app.ui.components.ObsidianStatusChip
import java.text.NumberFormat
import java.util.Locale

@Composable
fun WalletScreen(
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    viewModel: WalletViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is WalletUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    WalletContent(
        uiState = uiState,
        onOpenSettlement = viewModel::openSettlementModal,
        onCloseSettlement = viewModel::closeSettlementModal,
        onOpenAddWallet = viewModel::openAddWalletModal,
        onCloseAddWallet = viewModel::closeAddWalletModal,
        onAddWallet = viewModel::addNewWallet,
        onProcessSettlement = viewModel::processPayLaterSettlement
    )
}

@Composable
private fun WalletContent(
    uiState: WalletUiState,
    onOpenSettlement: () -> Unit,
    onCloseSettlement: () -> Unit,
    onOpenAddWallet: () -> Unit,
    onCloseAddWallet: () -> Unit,
    onAddWallet: (String, WalletType, Long) -> Unit,
    onProcessSettlement: (Long, Long, Long) -> Unit
) {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    val cashWallets = uiState.accounts.filter { it.walletType == WalletType.CASH_OR_DEBIT }
    val payLaterWallets = uiState.accounts.filter { it.walletType == WalletType.CREDIT_OR_PAYLATER }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (uiState.error != null) {
            Text(
                text = uiState.error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center).padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Manajemen Dompet & Kekayaan",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Kelola Kas, Rekening Bank, dan Utang PayLater Anda",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Card Net Worth & Quick Action
                item {
                    ObsidianCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = "Kekayaan Bersih (Net Worth)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = format.format(uiState.netWorth),
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Total Kas Likuid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(format.format(uiState.totalCashBalance), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Total Utang PayLater", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(format.format(uiState.totalPayLaterDebt), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onOpenSettlement,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pelunasan PayLater")
                                }

                                OutlinedButton(
                                    onClick = onOpenAddWallet,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tambah Dompet")
                                }
                            }
                        }
                    }
                }

                // Section 1: Dompet Kas & Rekening
                item {
                    Text(
                        text = "Dompet Kas & Rekening Bank (${cashWallets.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (cashWallets.isEmpty()) {
                    item {
                        Text("Belum ada dompet kas. Ketik nama dompet di form transaksi atau tekan Tambah Dompet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(cashWallets, key = { it.id }) { wallet ->
                        WalletRowItem(account = wallet, icon = Icons.Default.AccountBalanceWallet)
                    }
                }

                // Section 2: Utang & PayLater
                item {
                    Text(
                        text = "Tagihan Kredit & PayLater (${payLaterWallets.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                if (payLaterWallets.isEmpty()) {
                    item {
                        Text("Tidak ada liabilitas/utang PayLater yang aktif.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(payLaterWallets, key = { it.id }) { wallet ->
                        WalletRowItem(account = wallet, icon = Icons.Default.CreditCard)
                    }
                }
            }
        }

        // Modal BottomSheet Pelunasan PayLater (Anti-Double Counting)
        if (uiState.isSettlementModalOpen) {
            PayLaterSettlementModal(
                cashWallets = cashWallets,
                payLaterWallets = payLaterWallets,
                onDismiss = onCloseSettlement,
                onConfirmSettlement = onProcessSettlement
            )
        }

        // Modal BottomSheet Tambah Dompet Baru
        if (uiState.isAddWalletModalOpen) {
            AddWalletModal(
                onDismiss = onCloseAddWallet,
                onAddWallet = onAddWallet
            )
        }
    }
}

@Composable
private fun WalletRowItem(account: AccountEntity, icon: ImageVector) {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(account.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    ObsidianStatusChip(
                        text = if (account.walletType == WalletType.CASH_OR_DEBIT) "Kas / Rekening" else "PayLater / Kredit",
                        style = if (account.walletType == WalletType.CASH_OR_DEBIT) ObsidianChipStyle.POSITIVE else ObsidianChipStyle.PENDING
                    )
                }
            }

            Text(
                text = format.format(account.currentBalance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (account.walletType == WalletType.CASH_OR_DEBIT) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayLaterSettlementModal(
    cashWallets: List<AccountEntity>,
    payLaterWallets: List<AccountEntity>,
    onDismiss: () -> Unit,
    onConfirmSettlement: (Long, Long, Long) -> Unit
) {
    var selectedCashId by remember { mutableStateOf(cashWallets.firstOrNull()?.id ?: 0L) }
    var selectedPayLaterId by remember { mutableStateOf(payLaterWallets.firstOrNull()?.id ?: 0L) }
    var amountInput by remember { mutableStateOf("") }
    
    var cashExpanded by remember { mutableStateOf(false) }
    var payLaterExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Pelunasan Tagihan PayLater",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Dicatat sebagai TRANSFER netral antar-dompet (Anti Double-Counting)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Selector Dompet Sumber Kas
            ExposedDropdownMenuBox(
                expanded = cashExpanded,
                onExpandedChange = { cashExpanded = !cashExpanded }
            ) {
                val selectedName = cashWallets.find { it.id == selectedCashId }?.name ?: "Pilih Kas"
                OutlinedTextField(
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    readOnly = true,
                    value = selectedName,
                    onValueChange = {},
                    label = { Text("Sumber Kas Pelunas") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cashExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = cashExpanded,
                    onDismissRequest = { cashExpanded = false }
                ) {
                    cashWallets.forEach { w ->
                        DropdownMenuItem(
                            text = { Text(w.name) },
                            onClick = { selectedCashId = w.id; cashExpanded = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Selector Tagihan PayLater Target
            ExposedDropdownMenuBox(
                expanded = payLaterExpanded,
                onExpandedChange = { payLaterExpanded = !payLaterExpanded }
            ) {
                val selectedName = payLaterWallets.find { it.id == selectedPayLaterId }?.name ?: "Pilih Tagihan"
                OutlinedTextField(
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    readOnly = true,
                    value = selectedName,
                    onValueChange = {},
                    label = { Text("Target Tagihan PayLater") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = payLaterExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = payLaterExpanded,
                    onDismissRequest = { payLaterExpanded = false }
                ) {
                    payLaterWallets.forEach { w ->
                        DropdownMenuItem(
                            text = { Text(w.name) },
                            onClick = { selectedPayLaterId = w.id; payLaterExpanded = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Nominal
            OutlinedTextField(
                value = amountInput,
                onValueChange = { if (it.all { char -> char.isDigit() }) amountInput = it },
                label = { Text("Nominal Pelunasan (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val amount = amountInput.toLongOrNull() ?: 0L
                    if (selectedCashId != 0L && selectedPayLaterId != 0L && amount > 0) {
                        onConfirmSettlement(selectedCashId, selectedPayLaterId, amount)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Proses Pelunasan Netral")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWalletModal(
    onDismiss: () -> Unit,
    onAddWallet: (String, WalletType, Long) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(WalletType.CASH_OR_DEBIT) }
    var initialBalanceInput by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Tambah Dompet Baru",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Nama Dompet / Rekening") },
                placeholder = { Text("Contoh: BCA, GoPay, SPayLater") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            val types = listOf("Kas / Debit", "PayLater / Kredit")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                types.forEachIndexed { index, label ->
                    val type = if (index == 0) WalletType.CASH_OR_DEBIT else WalletType.CREDIT_OR_PAYLATER
                    SegmentedButton(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size)
                    ) {
                        Text(label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = initialBalanceInput,
                onValueChange = { if (it.all { char -> char.isDigit() }) initialBalanceInput = it },
                label = { Text("Saldo Awal (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val balance = initialBalanceInput.toLongOrNull() ?: 0L
                    if (nameInput.isNotBlank()) {
                        onAddWallet(nameInput, selectedType, balance)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan Dompet")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
