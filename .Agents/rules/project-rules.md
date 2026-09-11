---
trigger: always_on
---
# Project Rules: Personal_Wallet

## 1. Project Goal & High-Level Summary

* **Deskripsi singkat:** Aplikasi pencatat keuangan personal berbasis *local-first* (100% *offline*) untuk Android. Mendukung pencatatan transaksi multi-dompet (Kas/Debit vs Kredit/PayLater) melalui ekstraksi notifikasi otomatis (GoPay, DANA, ShopeePay/SPayLater, BCA) dan input manual untuk pengeluaran (*expense*), pemasukan (*income*), dan transfer/pelunasan tagihan (*transfer*). Dilengkapi mesin cerdas berbasis aturan (*rule-based engine*) untuk klasifikasi 3 tingkat pengeluaran, pencegahan pencatatan ganda utang, deteksi kebocoran dana (*spending leak detection*), serta analitik berkala (harian, bulanan, tahunan).
* **Target pengguna / Output utama:** Android Native App (APK/AAB), minimum SDK 26 (Android 8.0) dan target SDK 34+.
* **Penyimpanan:** 100% *Local Device Storage* (tanpa ketergantungan *cloud backend*).

---

## 2. Core Philosophy & Design Rules

* **Local-First & Zero Leak:** Data finansial, log transaksi, dan teks notifikasi dilarang keras meninggalkan perangkat. Privasi pengguna bersifat mutlak.
* **Double-Counting Prevention (Utang & PayLater):**
  * Pembelian memakai PayLater/Kartu Kredit dicatat sebagai `EXPENSE` pada dompet kredit di hari transaksi.
  * Pembayaran/pelunasan tagihan PayLater dari rekening kas dicatat sebagai `TRANSFER` (mutasi antar-dompet: Kas $\rightarrow$ PayLater), **bukan** `EXPENSE` baru, agar tidak terjadi penghitungan ganda pada laporan analitik.
* **Single Source of Truth (Single Ledger Balance Consistency):**
  * Saldo dompet (`currentBalance`) wajib tersinkronisasi secara konsisten berdasarkan kalkulasi saldo awal + agregasi transaksi berstatus `CONFIRMED`.
  * Pengubahan, penghapusan, atau penyisipan transaksi tanggal lampau tidak boleh menyebabkan desinkronisasi saldo dompet.
* **Deterministic Rule-Based Detection:** Mesin deteksi kebocoran (*leak detection*) dan klasifikasi wajib dijalankan secara deterministik via algoritma lokal/heuristik SQLite/Kotlin, bukan memanggil API LLM eksternal, agar tetap berfungsi seutuhnya saat perangkat *offline*.
* **Draft-First Ingestion:** Transaksi hasil ekstraksi notifikasi wajib berstatus `PENDING`. Pengguna memverifikasi atau menyesuaikan kategori dengan 1 ketukan sebelum memengaruhi saldo buku.
* **Graceful Ingestion & Unparsed Fallback:** Notifikasi yang tidak cocok dengan pola *regex* mana pun dilarang dibuang. Teks mentah disimpan ke tabel log *unparsed notification* agar dapat ditinjau ulang atau dijadikan pembuat aturan baru oleh pengguna.
* **Separation of Concerns:**
  * Parsing teks notifikasi terisolasi di `core/parser/`.
  * Deteksi anomali kebocoran dana terisolasi di `core/domain/analytics/`.
  * Agregasi laporan dan kueri saldo ditangani langsung oleh Room Database/SQLite via query terindeks.

---

## 3. Boundary & Guardrails (Strict Rules)

* **Zona Terlarang (READ-ONLY bagi AI Agent):**
  * `core/database/migrations/**` (skema migrasi Room yang sudah terbit dilarang diubah atau dihapus).
  * `core/model/TransactionEntity.kt` & `core/model/AccountEntity.kt` (skema tabel Room inti; perubahan hanya boleh via berkas migrasi baru).
  * `app/src/main/AndroidManifest.xml` (izin sensitif sistem dilarang diubah tanpa konfirmasi eksplisit).
* **Zona Kerja yang Diizinkan (WRITE SCOPE bagi AI Agent):**
  * `feature/**` (tampilan antarmuka Jetpack Compose, ViewModel, dan UI state).
  * `core/parser/rules/**` (daftar pola *regex* untuk e-wallet, paylater, dan m-banking).
  * `core/domain/**` (UseCases, *leak detection heuristics*, dan kalkulator analitik).
  * `core/data/**` (implementasi Room DAO dan Repository).

---

## 4. Stack & Architecture Standards

* **Core Engine & Data Layer:**
  * Bahasa: Kotlin (100%).
  * Database: Room Database (SQLite) + Kotlin Coroutines & Flow.
  * Dependency Injection: Hilt.
  * Background Execution: `NotificationListenerService` + `Dispatchers.Default` untuk parsing teks notifikasi.
* **UI & Presentation Layer:**
  * Framework: Jetpack Compose (Material 3).
  * State Management: ViewModel + `StateFlow` + `collectAsStateWithLifecycle()`.
  * Date/Time: `java.time` (Instant, LocalDate, ZoneId) berbasis *epoch millisecond* (`Long`).
* **Standar Kontrak Moneter:**
  * Nilai uang wajib bertipe `Long` (Rupiah bulat, misal: `50000L`). **Dilarang memakai `Float` atau `Double`.**

---

## 5. Expense Classification & Spending Leak Detection Engine

### A. Expense Classification Hierarchy (Tingkat Pengeluaran)

1. **Fixed Expenses (`FIXED`):**
   * *Definisi:* Beban wajib berkala dengan jadwal dan nominal pasti. Sulit dipangkas jangka pendek.
   * *Subkategori:*
     * `RENT_HOUSING`: Sewa kos, kontrakan, IPL, cicilan rumah.
     * `UTILITIES`: Listrik/air pascabayar, internet rumah.
     * `FINANCIAL_OBLIGATIONS`: Cicilan utang pokok/bunga pinjaman, premi asuransi, iuran wajib.

2. **Variable Essentials (`VARIABLE_ESSENTIAL`):**
   * *Definisi:* Kebutuhan pokok mendasar yang nominalnya fluktuatif sesuai pemakaian.
   * *Subkategori:*
     * `GROCERIES`: Bahan mentah, air galon, perlengkapan mandi/kebersihan.
     * `COMMUTE`: BBM, tiket transportasi umum, biaya parkir/tol rutin.
     * `COMMUNICATION`: Pulsa telepon dan kuota data esensial.

3. **Discretionary Spending (`DISCRETIONARY`):**
   * *Definisi:* Pos non-esensial penunjang gaya hidup, hiburan, atau kenyamanan. Pos pertama yang dapat dipangkas instan.
   * *Subkategori:*
     * `FNB_LIFESTYLE`: Kopi kafe, camilan, nongkrong, *food delivery*.
     * `ENTERTAINMENT_SUBSCRIPTIONS`: Bioskop, konser, *top-up* gim, *streaming* digital, SaaS personal.
     * `PERSONAL_SHOPPING`: Pakaian, aksesori, barang hobi, gawai non-kerja.

### B. Spending Leak Detection Framework (5 Aturan Deteksi Anomali)

Seluruh parameter batas (*thresholds*) dinilai secara dinamis melalui objek konfiguraasi `LeakDetectionConfig` agar fleksibel dan dapat dikustomisasi pengguna. Evaluasi dijalankan secara asinkron (`Dispatchers.Default`) via `AnalyticsUseCase`.

1. **Rule 1: The Micro-Expense Leak (Latte Factor):**
   * Transaksi pos diskresioner berulang bernominal kecil ($< \text{thresholdMicroExpenseAmount}$, default: Rp25.000) dengan frekuensi tinggi ($\ge \text{thresholdMicroExpenseWeeklyCount}$, default: 3–4 kali/minggu). Flag: `MICRO_EXPENSE`.

2. **Rule 2: Zombie Subscriptions (Langganan Pasif):**
   * Pemotongan berulang bulanan/tahunan dari *merchant* digital tanpa penambahan aktivitas pencatatan manual terkait dalam rentang waktu yang dapat dikonfigurasi. Flag: `ZOMBIE_SUB`.

3. **Rule 3: Impulse & Emotional Spikes:**
   * Lonjakan belanja non-esensial secara tiba-tiba di periode rawan (tanggal kembar e-commerce, akhir pekan, atau 1–3 hari pasca-gajian) dengan deviation signifikan ($\ge \text{thresholdImpulseDevMultiplier}$, default: 2.0x) di atas rata-rata harian. Flag: `IMPULSE_BUY`.

4. **Rule 4: Lifestyle Creep (Inflasi Gaya Hidup):**
   * Kenaikan persentase pos *Discretionary* dari bulan ke bulan yang menekan rasio tabungan (*savings rate*), meski pemasukan meningkat. Flag: `LIFESTYLE_CREEP`.

5. **Rule 5: PayLater / Debt Spiral Dependency:**
   * Proporsi transaksi gaya hidup (`DISCRETIONARY`) yang didanai melalui akun bertipe `CREDIT_OR_PAYLATER` melebihi ambang batas aman ($> \text{thresholdPayLaterRatio}$, default: 20% dari total pengeluaran bulanan). Flag: `PAYLATER_OVERUSE`.

---

## 6. Data Contracts & Schema Standards

### A. Enumerasi Domain & Konfigurasi

```kotlin
enum class TransactionType {
  EXPENSE,  // Mengurangi saldo kas atau menambah utang paylater
  INCOME,   // Menambah saldo kas
  TRANSFER  // Pemindahan dana (misal: Pelunasan tagihan PayLater dari Kas)
}

enum class WalletType {
  CASH_OR_DEBIT,       // Rekening Bank, Tunai, Saldo GoPay/ShopeePay biasa
  CREDIT_OR_PAYLATER   // SPayLater, GoPay Pinjam, Kartu Kredit, Kredivo
}

enum class TransactionSource { NOTIFICATION, MANUAL }
enum class TransactionStatus { PENDING, CONFIRMED, REJECTED }

enum class ExpenseCategoryType {
  FIXED,
  VARIABLE_ESSENTIAL,
  DISCRETIONARY,
  INCOME_CATEGORY,
  TRANSFER_CATEGORY // Khusus pelunasan utang / mutasi antar-dompet
}

enum class LeakTriggerType {
  NONE,
  MICRO_EXPENSE,
  ZOMBIE_SUB,
  IMPULSE_BUY,
  LIFESTYLE_CREEP,
  PAYLATER_OVERUSE
}

data class LeakDetectionConfig(
  val thresholdMicroExpenseAmount: Long = 25000L,
  val thresholdMicroExpenseWeeklyCount: Int = 3,
  val thresholdPayLaterRatio: Float = 0.20f,
  val thresholdImpulseDevMultiplier: Float = 2.0f
)
```

### B. Entity Data Contract (Room SQLite Tables)

```kotlin
@Entity(tableName = "accounts")
data class AccountEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  @ColumnInfo(name = "name") val name: String, // Contoh: "BCA", "SPayLater", "Dompet Tunai"
  @ColumnInfo(name = "wallet_type") val walletType: WalletType,
  @ColumnInfo(name = "initial_balance") val initialBalance: Long = 0L, // Saldo modal awal
  @ColumnInfo(name = "current_balance") val currentBalance: Long = 0L // Saldo akumulasi aktif
)

@Entity(
  tableName = "transactions",
  indices = [
    Index(value = ["timestamp"]),
    Index(value = ["status"]),
    Index(value = ["source_account_id"]),
    Index(value = ["is_leak_risk"]),
    Index(value = ["deduplication_hash"], unique = true) // Mencegah duplikasi notifikasi saat service restart
  ],
  foreignKeys = [
    ForeignKey(
      entity = AccountEntity::class,
      parentColumns = ["id"],
      childColumns = ["source_account_id"],
      onDelete = ForeignKey.RESTRICT
    ),
    ForeignKey(
      entity = AccountEntity::class,
      parentColumns = ["id"],
      childColumns = ["destination_account_id"],
      onDelete = ForeignKey.SET_NULL
    )
  ]
)
data class TransactionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  @ColumnInfo(name = "amount") val amount: Long, // Rupiah bulat
  @ColumnInfo(name = "type") val type: TransactionType,
  @ColumnInfo(name = "source") val source: TransactionSource,
  @ColumnInfo(name = "status") val status: TransactionStatus,
  @ColumnInfo(name = "source_account_id") val sourceAccountId: Long,
  @ColumnInfo(name = "destination_account_id") val destinationAccountId: Long? = null, // Diisi jika tipe = TRANSFER
  @ColumnInfo(name = "merchant_or_title") val merchantOrTitle: String,
  @ColumnInfo(name = "timestamp") val timestamp: Long, // Epoch millis (Local)
  @ColumnInfo(name = "category_type") val categoryType: ExpenseCategoryType,
  @ColumnInfo(name = "sub_category") val subCategory: String,
  @ColumnInfo(name = "is_leak_risk") val isLeakRisk: Boolean = false,
  @ColumnInfo(name = "leak_trigger_type") val leakTriggerType: LeakTriggerType = LeakTriggerType.NONE,
  @ColumnInfo(name = "action_suggestion") val actionSuggestion: String? = null,
  @ColumnInfo(name = "raw_notification_text") val rawNotificationText: String? = null,
  @ColumnInfo(name = "deduplication_hash") val deduplicationHash: String? = null
)

@Entity(tableName = "unparsed_notifications")
data class UnparsedNotificationEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  @ColumnInfo(name = "package_name") val packageName: String,
  @ColumnInfo(name = "raw_text") val rawText: String,
  @ColumnInfo(name = "received_timestamp") val receivedTimestamp: Long,
  @ColumnInfo(name = "is_resolved") val isResolved: Boolean = false
)
```

### C. Analitik & Format Kontrak Output

```kotlin
data class PeriodSummary(
  val totalExpense: Long,       // Hanya menjumlahkan TransactionType.EXPENSE
  val totalIncome: Long,        // Hanya menjumlahkan TransactionType.INCOME
  val totalDebtAccumulated: Long, // Akumulasi belanja via CREDIT_OR_PAYLATER yang belum lunas
  val netBalance: Long,
  val periodLabel: String
)

sealed interface Resource<out T> {
  data class Success<T>(val data: T) : Resource<T>
  data class Error(val throwable: Throwable, val message: String? = null) : Resource<Nothing>
  data object Loading : Resource<Nothing>
}
```

---

## 7. Performance & Resource Constraints

* **Thread Offloading:** Ekstraksi *regex* dan evaluasi 5 aturan *leak detection* wajib berjalan di thread latar belakang (`Dispatchers.Default`).
* **Database Optimization:** Kueri agregasi laporan (harian/bulanan/tahunan) hanya menghitung transaksi berstatus `CONFIRMED` dan bertipe `EXPENSE` atau `INCOME`. Transaksi `TRANSFER` diabaikan dari total pengeluaran/pemasukan buku kas.
* **Anti-Duplikasi (Idempotency Hashing):** Setiap notifikasi dihitung nilai hash SHA-256 unik berdasarkan formula: `SHA256(packageName + rawText + (timestamp / 60000))`. Transaksi dengan hash identik dalam jendela waktu 1 menit dipastikan aman dari pencatatan ganda saat service restart.
* **Graceful Degradation & Unparsed Logging:** Kegagalan ekstraksi teks notifikasi (*regex mismatch*) tidak boleh memicu *app crash*. Notifikasi otomatis disimpan ke `UnparsedNotificationEntity` sebagai data uji pengembangan pola *regex* baru.
