---
name: compose-craft
description: High-craft Jetpack Compose Material 3 & Anti-AI Slop Guidelines inspired by Tivi (Chris Banes), Now In Android, and Android Architecture Samples.
---

# Jetpack Compose Craft & Anti-AI Slop Guidelines

Panduan ini mengatur standar kualitas antarmuka (UI) dan arsitektur komponen Jetpack Compose di `Personal_Wallet` agar terhindar dari tampilan generik (*AI slop*) serta memiliki *tactile feel* dan performa kelas dunia.

---

## 🎨 1. Anti-AI Slop & Visual Craft (Gaya Tivi - Chris Banes)

### A. Tactile Micro-Interactions (Efek Tekan Kenyal)
Dilarang menggunakan `Card` atau `Button` yang kaku tanpa animasi feedback. Semua elemen interaktif wajib menggunakan *spring physics press effect*:

```kotlin
fun Modifier.pressClickEffect() = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pressScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                }
            )
        }
}
```

### B. Presisi Tipografi Finansial (Tabular Numbers)
Nilai nominal uang (`Rp 1.500.000`) **wajib** menggunakan *tabular numbers* (`tnum`) agar angka sejajar secara vertikal dan tidak membuat layout melompat (*layout shift*) saat angka diperbarui:

```kotlin
val MonetaryTextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    fontFeatureSettings = "tnum" // Tabular numbers (angka dengan lebar konsisten)
)
```

### C. Seamless Edge-to-Edge & Custom Insets
Manfaatkan `WindowInsets` agar konten mengalir mulus di bawah status bar & navigation bar:

```kotlin
Scaffold(
    contentWindowInsets = WindowInsets.systemBars
) { innerPadding ->
    // Gunakan innerPadding secara presisi pada Modifier.padding(innerPadding)
}
```

---

## 🏛️ 2. Sealed UiState Pattern (Gaya Now in Android)

Setiap layar ViewModel wajib membungkus kondisi UI ke dalam `Sealed Interface` eksplisit. Dilarang menggunakan banyak `Boolean` terpisah (`isLoading`, `isError`, dll) di dalam ViewModel:

```kotlin
sealed interface TransactionUiState {
    data object Loading : TransactionUiState
    data object Empty : TransactionUiState
    data class Success(
        val transactions: List<Transaction>,
        val summary: PeriodSummary
    ) : TransactionUiState
    data class Error(val message: String) : TransactionUiState
}
```

---

## 🔄 3. Stateless vs Stateful Split (Gaya Architecture Samples)

Setiap berkas layar Composable **wajib dipisah menjadi 2 fungsi**:

1. **Stateful Wrapper (`TransactionScreen`):** Mengambil data dari ViewModel, mengumpulkan `StateFlow` via `collectAsStateWithLifecycle()`, dan menangani navigasi.
2. **Stateless Content (`TransactionContent`):** Hanya menerima data murni dan *lambda callback* event. Wajib memiliki `@Preview` instan!

```kotlin
// 1. Stateful Wrapper
@Composable
fun TransactionScreen(
    viewModel: TransactionViewModel = hiltViewModel(),
    onNavigateToDetail: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TransactionContent(
        uiState = uiState,
        onTransactionClick = onNavigateToDetail,
        onRefresh = viewModel::loadTransactions
    )
}

// 2. Stateless Content (Mendukung Compose @Preview tanpa ViewModel)
@Composable
fun TransactionContent(
    uiState: TransactionUiState,
    onTransactionClick: (Long) -> Unit,
    onRefresh: () -> Unit
) {
    when (uiState) {
        is TransactionUiState.Loading -> LoadingShimmer()
        is TransactionUiState.Empty -> EmptyStateView()
        is TransactionUiState.Success -> TransactionList(uiState.transactions, onTransactionClick)
        is TransactionUiState.Error -> ErrorView(uiState.message, onRetry = onRefresh)
    }
}
```

---

## 📊 4. Smooth Animations & Shimmer Skeleton

* **Layout Transition:** Gunakan `Modifier.animateContentSize()` pada elemen kartu atau daftar yang ukurannya bisa bertambah/berkurang.
* **Loading State:** Gunakan *Shimmer Animation Skeleton* daripada `CircularProgressIndicator` biasa di tengah layar.
