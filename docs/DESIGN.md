# Design System & UI/UX Standards: Personal_Wallet

## 1. UI/UX Philosophy & Anti-AI Slop Standards

Aplikasi `Personal_Wallet` menerapkan standar **High-Craft UI** (mengadaptasi prinsip Tivi oleh Chris Banes, Now in Android, dan Android Architecture Samples).

* **Tactile & Responsive:** Antarmuka harus memberikan umpan balik fisik yang memuaskan (*spring physics press effects*) pada setiap sentuhan.
* **Presisi Moneter:** Nilai nominal uang harus sejajar secara sempurna tanpa melompat saat nilainya diperbarui.
* **Modern Dark/Light Palette:** Penggunaan warna bertema finansial modern (*Emerald Green*, *Deep Slate*, *Muted Graphite*) sebagai pengganti warna ungu bawaan M3.

---

## 2. Color System (Material 3 Financial Tokens)

```kotlin
object WalletColors {
    // Primary Accents
    val EmeraldGreen = Color(0xFF00D09C)     // Income / Positive Balance / Primary Accent
    val CoralRed = Color(0xFFFF4D4D)         // Expense / Spending Leak Risk
    val SlateBlue = Color(0xFF4A80F0)        // Transfer / PayLater Movement

    // Dark Mode Palette
    val DarkBackground = Color(0xFF121212)   // Main Screen Background
    val DarkSurface = Color(0xFF1E1E1E)      // Cards & Sheets
    val DarkSurfaceVariant = Color(0xFF2C2C2C)// Elevating Cards

    // Text Colors
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFA0A0A0)
    val TextMuted = Color(0xFF6E6E6E)
}
```

---

## 3. Typography & Monetary Precision Standards

 Seluruh tampilan nominal mata uang (`Rp 1.500.000`) **wajib menggunakan Tabular Numbers (`tnum`)** agar lebar setiap digit konsisten:

```kotlin
val MonetaryTextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    fontFeatureSettings = "tnum" // Tabular numbers (lebar angka seragam)
)
```

---

## 4. Micro-Interactions & Animation Rules

### A. Spring Press Click Effect
Elemen tombol atau kartu daftar transaksi wajib menggunakan animasi efek tekan kenyal:

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

### B. Shimmer Skeleton Loading
Dilarang menggunakan `CircularProgressIndicator` melayang di tengah layar. Gunakan animasi *shimmer skeleton* pada placeholder kartu saat data sedang dimuat.

---

## 5. Screen Architecture Rules (Stateless vs Stateful)

Setiap file Composable layar wajib dipisah secara tegas:

1. **Stateful Wrapper (`TransactionScreen`):**
   * Mengambil ViewModel via Hilt (`hiltViewModel()`).
   * Mengumpulkan `StateFlow` via `collectAsStateWithLifecycle()`.
   * Menangani navigasi antar-layar.
2. **Stateless Content (`TransactionContent`):**
   * Menerima *Sealed UiState* murni dan *event callbacks*.
   * Memiliki `@Preview` instan di Android Studio tanpa perlu ViewModel mock.
