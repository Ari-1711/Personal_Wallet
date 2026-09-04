package com.personalwallet.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    data object Drafts : Screen("drafts", "Drafts", Icons.AutoMirrored.Filled.ListAlt)
    data object LeakDetection : Screen("leak_detection", "Analitik", Icons.Default.Analytics)
    data object Wallets : Screen("wallets", "Dompet", Icons.Default.AccountBalanceWallet)
    data object AddTransaction : Screen("add_transaction", "Tambah", Icons.Default.Add)
}

val BottomNavScreens = listOf(
    Screen.Dashboard,
    Screen.Drafts,
    Screen.LeakDetection,
    Screen.Wallets
)
