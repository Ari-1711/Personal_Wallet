package com.personalwallet.app.ui.screens.dashboard

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.border
import com.personalwallet.app.ui.components.ObsidianCard
import com.personalwallet.app.ui.components.ObsidianChipStyle
import com.personalwallet.app.ui.components.ObsidianStatusChip
import com.personalwallet.app.core.model.TransactionEntity
import com.personalwallet.app.core.model.TransactionType
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DashboardContent(
        uiState = uiState
    )
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            uiState.error != null -> {
                Text(
                    text = uiState.error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            }
            uiState.summary != null -> {
                val format = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
                    maximumFractionDigits = 0
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 24.dp) // Mengikuti grid architecture Obsidian Sage
                ) {
                    // Header App (Bisa dipisah ke TopBar nanti, untuk sekarang kita render inline)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Personal Wallet",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ObsidianStatusChip(text = "Offline", style = ObsidianChipStyle.NEUTRAL)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Saldo & Utang (2 Kartu Ringkas)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ObsidianCard(modifier = Modifier.weight(1f)) {
                            Column {
                                Text("Saldo Kas", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(format.format(14850000L), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                                Text("Tersedia", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        
                        ObsidianCard(modifier = Modifier.weight(1f)) {
                            Column {
                                Text("Tagihan PayLater", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(format.format(3420000L), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                                Text("Jatuh tempo 5 hari", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Transaksi Terkini Dummy (Hardcoded untuk mengikuti UI reference yang diminta)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Transaksi Terkini", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Lihat Semua", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Column {
                            if (uiState.recentTransactions.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Belum ada transaksi terkini",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                uiState.recentTransactions.forEachIndexed { index, transaction ->
                                    val icon = when (transaction.type) {
                                        TransactionType.EXPENSE -> Icons.Default.ShoppingBag
                                        TransactionType.INCOME -> Icons.Default.AccountBalanceWallet
                                        TransactionType.TRANSFER -> Icons.Default.CreditCard
                                    }
                                    
                                    val amountPrefix = if (transaction.type == TransactionType.INCOME || transaction.type == TransactionType.TRANSFER) "+" else "-"
                                    val amountStr = "$amountPrefix${format.format(transaction.amount)}"
                                    
                                    val chip = if (transaction.isLeakRisk) {
                                        ObsidianChipStyle.PENDING to "Bocor"
                                    } else null

                                    DummyTransactionItem(
                                        icon = icon,
                                        title = transaction.merchantOrTitle,
                                        subtitle = transaction.categoryType.name.replace("_", " ").lowercase()
                                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                                        amountStr = amountStr,
                                        chip = chip
                                    )
                                    
                                    if (index < uiState.recentTransactions.size - 1) {
                                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DummyTransactionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    amountStr: String,
    chip: Pair<ObsidianChipStyle, String>?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                    if (chip != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        ObsidianStatusChip(text = chip.second, style = chip.first)
                    }
                }
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            text = amountStr,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (amountStr.startsWith("+")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
        )
    }
}


