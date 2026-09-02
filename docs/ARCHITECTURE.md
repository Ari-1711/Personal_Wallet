# Architecture & Engineering Standards: Personal_Wallet

## 1. Architectural Pattern: Clean Architecture + MVVM

Aplikasi `Personal_Wallet` menerapkan **Clean Architecture** dengan pola **MVVM (Model-View-ViewModel)** dan **Unidirectional Data Flow (UDF)**.

```
┌─────────────────────────────────────────────────────────┐
│                    UI Layer (Compose)                   │
│   Stateless Content  ◄───  Stateful Screen Wrapper      │
└───────────────────────────┬─────────────────────────────┘
                            │ Collect StateFlow / Events
                            ▼
┌─────────────────────────────────────────────────────────┐
│                 ViewModel / StateHolder                 │
│              Exposes Sealed UiState Flow                │
└───────────────────────────┬─────────────────────────────┘
                            │ Executes UseCases
                            ▼
┌─────────────────────────────────────────────────────────┐
│                    Domain Layer                         │
│     UseCases, Leak Detection Engine, Pure Models        │
└───────────────────────────┬─────────────────────────────┘
                            │ Calls Repository Contracts
                            ▼
┌─────────────────────────────────────────────────────────┐
│                    Data Layer                           │
│   OfflineFirst Repository, Room DAO, Local SQLite DB    │
└─────────────────────────────────────────────────────────┘
```

---

## 2. Directory & Package Structure

```
com.personalwallet.app/
├── core/
│   ├── data/                 # Repository implementation & Room Data Sources
│   │   ├── dao/              # AccountDao, TransactionDao, UnparsedNotificationDao
│   │   ├── model/            # Room Entities (AccountEntity, TransactionEntity, etc.)
│   │   └── repository/       # OfflineFirstWalletRepositoryImpl
│   ├── database/             # RoomDatabase Configuration & Migrations
│   ├── domain/               # Core Business Logic & Models
│   │   ├── analytics/        # Spending Leak Detection Heuristics (5 Rules)
│   │   ├── model/            # Pure Domain Models (Account, Transaction, PeriodSummary)
│   │   └── usecase/          # GetTransactionsUseCase, ProcessNotificationUseCase, etc.
│   ├── parser/               # Notification Regex Parsing Engine
│   │   └── rules/            # GopayParserRule, DanaParserRule, ShopeePayParserRule, BcaParserRule
│   └── service/              # Background NotificationListenerService
├── feature/                  # UI Feature Screens (Stateful Wrapper + Stateless Content)
│   ├── dashboard/            # Home / Net Balance / Leak Alert Banner
│   ├── drafts/               # Pending Notification Draft Verification
│   ├── transactions/         # Transaction List & Filters
│   └── analytics/            # Spending Leak & Category Analytics
└── ui/                       # Design System, Theme, Components, Navigation
    ├── components/           # PressEffects, MonetaryText, ShimmerSkeleton
    ├── navigation/           # Type-safe Jetpack Compose Navigation Graphs
    └── theme/                # Color, Type, Shape, Theme Material 3
```

---

## 3. Data Flow Between Services & Background Execution

### A. Notification Ingestion Flow
1. **System Trigger:** OS memancarkan notifikasi dari GoPay/DANA/ShopeePay/BCA.
2. **Service Interception:** `NotificationListenerService` menangkap teks notifikasi di thread latar belakang (`Dispatchers.Default`).
3. **Regex Extraction:** `ParserEngine` mencocokkan teks dengan `ParserRule`.
   * **Jika Match:** Hitung `deduplication_hash` (SHA-256) $\rightarrow$ Simpan ke `transactions` dengan status `PENDING`.
   * **Jika Unmatched:** Simpan ke `unparsed_notifications` sebagai log mentah.
4. **UI Notification:** Tampilan *Draft Verification Screen* otomatis memperbarui daftar *pending drafts* secara reaktif via `Flow`.

### B. Single Source of Truth Balance Calculation Flow
* Saldo dompet **tidak dihitung dari saldo statis yang mudah korup**, melainkan dikalkulasi dinamis via SQL Aggregation:
  $$\text{Current Balance} = \text{Initial Balance} + \sum(\text{INCOME}) - \sum(\text{EXPENSE}) \pm \sum(\text{TRANSFER})$$
* Berjalan otomatis secara reaktif saat ada transaksi berstatus `CONFIRMED`.

---

## 4. Technical Choices & Rationales (Decision Matrix)

| Keputusan Teknis | Pilihan Digunakan | Alasan & Rasionalisasi |
| :--- | :--- | :--- |
| **Penyimpanan** | Room (SQLite) | 100% Offline, tanpa ketergantungan cloud server, performa kueri lokal terbukti cepat (< 2ms). |
| **Moneter Data** | `Long` (Rupiah Bulat) | Menghindari *floating-point arithmetic precision error* (`Double`/`Float`). |
| **Background Thread** | `Dispatchers.Default` | Offload pemrosesan *regex* dan 5 aturan *leak detection* agar UI tidak pernah mengalami *lag*. |
| **DI Framework** | Hilt | Standar resmi Android, pengujian unit test terisolasi via Fake Repository. |
| **State Management**| `StateFlow` + `collectAsStateWithLifecycle()` | Siklus hidup aman (*lifecycle-aware*), mencegah memory leak saat UI di-background. |
