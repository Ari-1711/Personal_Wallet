package com.personalwallet.app.ui.screens.draft

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personalwallet.app.core.model.AccountEntity
import com.personalwallet.app.core.model.TransactionEntity
import com.personalwallet.app.core.model.UnparsedNotificationEntity
import com.personalwallet.app.ui.components.ObsidianCard
import com.personalwallet.app.ui.components.ObsidianChipStyle
import com.personalwallet.app.ui.components.ObsidianStatusChip
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DraftScreen(
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    viewModel: DraftViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is DraftUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    DraftContent(
        uiState = uiState,
        onTabSelect = viewModel::selectTab,
        onConfirmDraft = viewModel::confirmDraft,
        onRejectDraft = viewModel::rejectDraft,
        onResolveUnparsed = viewModel::resolveUnparsed
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraftContent(
    uiState: DraftUiState,
    onTabSelect: (DraftTab) -> Unit,
    onConfirmDraft: (TransactionEntity, Long?) -> Unit,
    onRejectDraft: (Long) -> Unit,
    onResolveUnparsed: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Draft & Ingestion Log",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Selector (Draft Transaksi vs Notifikasi Mentah)
        val tabs = listOf(
            "Draft (${uiState.pendingDrafts.size})",
            "Mentah (${uiState.unparsedNotifications.size})"
        )
        
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, label ->
                val tab = if (index == 0) DraftTab.PENDING_DRAFTS else DraftTab.UNPARSED_NOTIFICATIONS
                SegmentedButton(
                    selected = uiState.selectedTab == tab,
                    onClick = { onTabSelect(tab) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size)
                ) {
                    Text(label)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.selectedTab == DraftTab.PENDING_DRAFTS) {
            if (uiState.pendingDrafts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Belum ada draft transaksi pending.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.pendingDrafts, key = { it.id }) { draft ->
                        PendingDraftCard(
                            draft = draft,
                            accounts = uiState.accounts,
                            onConfirm = { accountId -> onConfirmDraft(draft, accountId) },
                            onReject = { onRejectDraft(draft.id) }
                        )
                    }
                }
            }
        } else {
            if (uiState.unparsedNotifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Tidak ada log notifikasi mentah.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.unparsedNotifications, key = { it.id }) { unparsed ->
                        UnparsedNotificationCard(
                            unparsed = unparsed,
                            onResolve = { onResolveUnparsed(unparsed.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PendingDraftCard(
    draft: TransactionEntity,
    accounts: List<AccountEntity>,
    onConfirm: (Long?) -> Unit,
    onReject: () -> Unit
) {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }
    
    var selectedAccountId by remember { 
        mutableStateOf(accounts.find { it.id == draft.sourceAccountId }?.id ?: accounts.firstOrNull()?.id) 
    }
    var accountExpanded by remember { mutableStateOf(false) }

    ObsidianCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = draft.merchantOrTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Notifikasi Auto-Parsed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = "-${format.format(draft.amount)}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (!draft.rawNotificationText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"${draft.rawNotificationText}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pemilih Dompet Sumber Dana sebelum 1-click Confirm
            ExposedDropdownMenuBox(
                expanded = accountExpanded,
                onExpandedChange = { accountExpanded = !accountExpanded }
            ) {
                val selectedAccountName = accounts.find { it.id == selectedAccountId }?.name ?: "Pilih Dompet Target"
                
                OutlinedTextField(
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    readOnly = true,
                    value = selectedAccountName,
                    onValueChange = { },
                    label = { Text("Potong dari Dompet") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                )
                ExposedDropdownMenu(
                    expanded = accountExpanded,
                    onDismissRequest = { accountExpanded = false },
                ) {
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                selectedAccountId = account.id
                                accountExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: Confirm & Reject
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Abaikan")
                }
                
                Button(
                    onClick = { onConfirm(selectedAccountId) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Setujui (Konfirmasi)")
                }
            }
        }
    }
}

@Composable
private fun UnparsedNotificationCard(
    unparsed: UnparsedNotificationEntity,
    onResolve: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale("id", "ID"))
    val dateStr = Instant.ofEpochMilli(unparsed.receivedTimestamp)
        .atZone(ZoneId.systemDefault())
        .format(formatter)

    ObsidianCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = unparsed.packageName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                ObsidianStatusChip(text = "Fallback", style = ObsidianChipStyle.NEUTRAL)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = unparsed.rawText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onResolve,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Tandai Selesai")
            }
        }
    }
}
