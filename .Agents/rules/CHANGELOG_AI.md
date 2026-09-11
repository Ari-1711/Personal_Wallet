---
trigger: always_on
---
# AI Dev Log

## Sesi Pembuatan Berkas Data Uji Postman (JSON & CSV)
- **Commit**: `0aef0e7`
- **Model yang digunakan**: AI Assistant (Gemini / Claude via Android Studio)
- **Problem**: Pengembang memerlukan berkas sampel data JSON/CSV dan koleksi Postman untuk menguji skenario transaksi, notifikasi e-wallet, dan skema data.
- **Changes Made**:
  - `test_data/Postman_Collection_Personal_Wallet.json`: Koleksi Postman v2.1.0 dengan request simulasi notifikasi (GoPay, BCA, DANA, SPayLater, Unparsed Fallback) dan transaksi.
  - `test_data/sample_transactions.json`: Array sampel 5 skenario transaksi (Pengeluaran, Pemasukan, Pelunasan PayLater).
  - `test_data/sample_transactions.csv`: Format CSV untuk pengujian *Postman Collection Runner*.
  - `test_data/sample_notifications.json`: Sampel notifikasi e-wallet mentah untuk pengujian regex parser.
  - `test_data/README.md`: Panduan petunjuk impor dan penggunaan di Postman.
- **Decisions & Rationale**:
  - Menyediakan format JSON dan CSV agar fleksibel diimpor ke Postman maupun alat testing REST/Mock lokal lainnya.

---

## Sesi Pembersihan Git Tracking & Hygiene Repositori
- **Commit**: `808a4ae`
- **Model yang digunakan**: AI Assistant (Gemini / Claude via Android Studio)
- **Problem**: Folder referensi desain `stitch_offline_smart_wallet_tracker` sempat ter-push ke repositori publik GitHub.
- **Changes Made**:
  - `.gitignore`: Menambahkan baris `stitch_offline_smart_wallet_tracker/`.
  - Git Index: Menghapus tracking folder dari indeks git (`git rm -r --cached`) tanpa menghapus file asli di penyimpanan lokal.
- **Decisions & Rationale**:
  - Mencegah file gambar/mockup berukuran besar mengotori repositori utama, sambil tetap mempertahankan file referensi tersebut di sistem lokal untuk pengembangan UI.

---

## Sesi Implementasi Obsidian Sage UI, Draft Ingestion, Leak Analytics & PayLater
- **Commit**: `5ed70bc`
- **Model yang digunakan**: AI Assistant (Gemini / Claude via Android Studio)
- **Problem**: 
  - Mengatasi galat *Incompatible Gradle JVM version* saat sinkronisasi akibat penggunaan Java 25.
  - Membangun antarmuka *Obsidian Sage*, sistem ekstraksi notifikasi otomatis, mesin analitik kebocoran dana, dan form pelunasan PayLater.
- **Changes Made**:
  - `gradle.properties` & `.idea/gradle.xml`: Menyesuaikan versi JDK ke Java 17/21 agar Gradle sync dan build berjalan sukses.
  - `core/service/WalletNotificationService.kt`: Layanan latar belakang pembaca notifikasi otomatis (*Draft-First*) dengan hashing SHA-256 anti-duplikasi & fallback `unparsed_notifications`.
  - `core/parser/rules/GoPayParserRule.kt`: Aturan regex ekstraksi transaksi GoPay.
  - `core/domain/usecase/LeakDetectionUseCase.kt`: Implementasi 5 aturan deteksi bocor halus (*Latte Factor*, *Zombie Subscriptions*, *Impulse Spikes*, *Lifestyle Creep*, *PayLater Overuse*) di `Dispatchers.Default`.
  - `ui/theme/*` & `ui/components/*`: Menerapkan palet warna *Obsidian Sage* (Tema 100% Gelap) dan komponen UI independen (`ObsidianCard`, `ObsidianStatusChip`).
  - `ui/screens/transaction/*`: Form input manual transaksi dengan pilihan dompet fleksibel (otomatis membuat dompet baru dari nama yang diketik).
  - `ui/screens/draft/*`: Layar Inbox Draft (Tab *Draft Transaksi* & *Notifikasi Mentah*).
  - `ui/screens/leak/*`: Layar analitik risiko kebocoran keuangan dengan indikator rasio PayLater.
  - `ui/screens/wallet/*`: Layar manajemen Net Worth dan Modal Pelunasan PayLater netral (*Anti-Double Counting*).
- **Decisions & Rationale**:
  - Menerapkan arsitektur MVVM murni dengan `StateFlow` dan `SharedFlow` (`uiEvent`) di Compose sesuai dengan panduan *skill* `android-viewmodel`.
  - UI dimodelkan langsung dari file referensi HTML *Obsidian Sage* agar presisi dan tidak terlihat generik ("AI slop").
  - Menetapkan pelunasan PayLater sebagai `TransactionType.TRANSFER` dari Kas $\rightarrow$ PayLater sehingga hanya terjadi perpindahan kas, tidak tercatat sebagai pengeluaran (*Expense*) dua kali.
- **Pending / Next Steps**:
  - Menambah Parser Rules untuk notifikasi Bank BCA, DANA, dan SPayLater.
  - Mengimplementasikan tampilan layar Buku Kas (Riwayat Transaksi Lengkap) beserta filternya.
  - Menajamkan logika deteksi *Zombie Subscription* dan *Lifestyle Creep* (aturan 2 dan 4) pada *Leak Engine*.

---

## Sesi Implementasi Core Wallet & Database Room
- **Commit**: `e2035a1`
- **Model yang digunakan**: AI Assistant
- **Problem**: Membangun skema penyimpanan data lokal yang terenkripsi, terstruktur, dan aman tanpa ketergantungan cloud (*Local-First*).
- **Changes Made**:
  - `core/model/*`: Dibuat `AccountEntity`, `TransactionEntity`, `UnparsedNotificationEntity`, dan enumerasi domain (`TransactionType`, `WalletType`, `ExpenseCategoryType`, `LeakTriggerType`).
  - `core/database/*`: Dibuat `WalletDatabase`, `AccountDao`, `TransactionDao`, dan `UnparsedNotificationDao`.
  - `core/data/repository/*`: Implementasi `AccountRepositoryImpl` dan `TransactionRepositoryImpl` dengan transaksi atomik `db.withTransaction`.
  - `core/di/*`: Dibuat `DatabaseModule` dan `RepositoryModule` untuk injeksi dependensi Hilt.
  - `ui/navigation/*`: Dibuat `MainNavGraph` dan `Screen` sealed class untuk navigasi 4 tab utama.
- **Decisions & Rationale**:
  - Menggunakan tipe data `Long` untuk seluruh nilai moneter (Rupiah bulat) untuk menghindari kelemahan presisi `Float`/`Double`.
  - Menggunakan Room Database + Kotlin Coroutines Flow untuk pembaruan data UI yang reaktif.

---

## Sesi Inisialisasi Pondasi Proyek Android
- **Commit**: `1572195` & `cb9f4a0`
- **Model yang digunakan**: AI Assistant
- **Problem**: Menyiapkan struktur dasar proyek Android Native sebelum implementasi fitur.
- **Changes Made**:
  - `gradle/libs.versions.toml`: Setup Version Catalog (AGP, Kotlin 2.0.20, Room 2.6.1, Hilt 2.51.1, Compose BOM).
  - `app/build.gradle.kts`: Konfigurasi plugin Android Application, KSP, Compose Compiler, dan Hilt.
  - `MainActivity.kt` & `WalletApplication.kt`: Penyiapan *entry point* aplikasi dengan anotasi `@HiltAndroidApp` dan `@AndroidEntryPoint`.
  - `docs/PRD.md`: Memindahkan dokumen Product Requirement Document ke folder `docs/`.
- **Decisions & Rationale**:
  - Menggunakan Kotlin Gradle DSL (`.gradle.kts`) dan KSP untuk *code generation* yang lebih cepat dibandingkan kapt.

---

## Sesi Setup Rules & CI Workflow
- **Commit**: `d7767e6`
- **Model yang digunakan**: AI Assistant
- **Problem**: Penyiapan aturan pengkodean, *skills*, dan integrasi berkelanjutan.
- **Changes Made**:
  - `.Agents/rules/`: Dibuat aturan proyek (`project-rules.md`, `tech-stack.md`, `ERROR_HANDLING_RULES.md`).
  - `.Agents/skills/`: Ditambahkan kumpulan *skills* pengkodean Android, Coroutines, Jetpack Compose, dan styling.
  - `.github/workflows/`: Dibuat workflow CI GitHub Actions untuk validasi build.
  - `.gitignore`: Setup aturan pengabaian file build, SDK local properties, dan cache.
- **Decisions & Rationale**:
  - Menetapkan aturan ketat *Local-First* & *Zero Telemetry* dari awal pembuatan proyek.
