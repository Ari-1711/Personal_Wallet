# Product Requirements Document (PRD): Personal_Wallet

## 1. Executive Summary & Vision

* **Nama Produk:** Personal_Wallet
* **Visi Produk:** Aplikasi pencatat keuangan personal berbasis *local-first* (100% *offline*) untuk Android yang membantu pengguna mengendalikan keuangan, mendeteksi kebocoran dana (*spending leaks*), serta mengelola dompet kas vs utang PayLater secara otomatis tanpa mengorbankan privasi data.
* **Platform Utama:** Android Native (Minimum SDK 26 / Android 8.0 Oreo, Target SDK 34+).
* **Filosofi Utama:** **100% Privacy & Zero Cloud Leak**. Seluruh data keuangan, log transaksi, dan teks notifikasi tersimpan dan diproses secara eksklusif di penyimpanan lokal perangkat (*Local SQLite/Room Database*).

---

## 2. Problem Statement & User Personas

### A. Masalah Yang Diselesaikan (Problem Statement)
1. **Friksi Pencatatan Manual:** Pengguna malas mencatat transaksi harian kecil (kopi, parkir, jajan) sehingga anggaran bocor tanpa disadari (*Latte Factor*).
2. **Bahaya Pencatatan Ganda PayLater/Kartu Kredit:** Kebanyakan aplikasi finansial salah mencatat pembayaran tagihan PayLater sebagai pengeluaran baru (*double expense*), padahal pengeluaran riil terjadi saat barang dibeli.
3. **Kekhawatiran Privasi Data:** Pengguna enggan menghubungkan akun bank ke cloud backend pihak ketiga karena risiko kebocoran data pribadi dan finansial.
4. **Bahaya Jebakan Utang (PayLater Spiral):** Pengguna tidak sadar bahwa porsi belanja gaya hidup yang dibiayai utang PayLater sudah melebihi batas aman kemampuan finansial mereka.

### B. User Persona
* **Target Pengguna:** Mahasiswa, *freelancer*, pekerja muda, dan profesional di Indonesia yang aktif menggunakan *e-wallet* (GoPay, DANA, ShopeePay/SPayLater) dan *m-banking* (BCA) untuk transaksi sehari-hari, serta menginginkan transparansi penuh atas keuangan mereka tanpa ketergantungan internet.

---

## 3. Product Scope & Functional Requirements

### F1. Manajemen Multi-Dompet (Cash vs Credit Accounts)
* **Kategori Dompet:**
  1. `CASH_OR_DEBIT`: Bank BCA, Dompet Tunai, Saldo GoPay, Saldo DANA, ShopeePay Biasa.
  2. `CREDIT_OR_PAYLATER`: SPayLater, GoPay Pinjam, Kartu Kredit, Kredivo.
* **Kalkulasi Saldo Konsisten:** Saldo dihitung dinamis dari `initialBalance` + agregasi transaksi `CONFIRMED` untuk mencegah desinkronisasi saat ada transaksi lampau yang diedit atau dihapus.

### F2. Ingestion & Ekstraksi Notifikasi Otomatis (*Draft-First*)
* **Mesin Parser Deterministik:** Mengekstrak teks notifikasi dari aplikasi GoPay, DANA, ShopeePay/SPayLater, dan BCA secara lokal di background (`NotificationListenerService`).
* **Status *Draft-First* (`PENDING`):** Transaksi hasil ekstraksi tidak langsung memengaruhi saldo, melainkan berstatus `PENDING` agar pengguna dapat memverifikasi atau menyesuaikan kategori cukup dengan 1 ketukan.
* **Penanganan Notifikasi Tak Terekstraksi (*Unparsed Fallback*):** Teks notifikasi yang tidak cocok dengan regex tidak dibuang, melainkan disimpan ke tabel `unparsed_notifications` untuk ditinjau ulang.
* **Anti-Duplikasi (Idempotency):** Notifikasi di-hash menggunakan SHA-256 (`packageName + rawText + timestampMinutes`) untuk mencegah duplikasi saat service restart.

### F3. Pencegahan Pencatatan Ganda Utang (*Double-Counting Prevention*)
* **Saat Belanja Pakai PayLater:** Dicatat sebagai `EXPENSE` pada dompet kredit di hari transaksi terjadi.
* **Saat Pelunasan Tagihan PayLater:** Dicatat sebagai `TRANSFER` (mutasi antar-dompet: Kas $\rightarrow$ PayLater), **bukan** `EXPENSE` baru.

### F4. Klasifikasi Pengeluaran 3-Tingkat (*Expense Hierarchy*)
1. **Fixed Expenses (`FIXED`):** Sewa kos, tagihan listrik/internet, cicilan utang pokok.
2. **Variable Essentials (`VARIABLE_ESSENTIAL`):** Bahan makanan (*groceries*), BBM/transportasi rutin, pulsa/data.
3. **Discretionary Spending (`DISCRETIONARY`):** Kopi kafe, *food delivery*, *top-up game*, *streaming*, belanja hobi.

### F5. Spending Leak Detection Engine (5 Aturan Anomali)
Menjalankan analisis heuristik lokal secara asinkron (`Dispatchers.Default`) untuk mendeteksi 5 pola kebocoran dana dengan ambang batas yang dapat dikustomisasi (`LeakDetectionConfig`):
1. **Rule 1: Micro-Expense Leak (`MICRO_EXPENSE`):** Transaksi diskresioner kecil (< Rp25.000) dengan frekuensi tinggi ($\ge$ 3x/minggu).
2. **Rule 2: Zombie Subscriptions (`ZOMBIE_SUB`):** Pemotongan berulang bulanan dari merchant digital tanpa aktivitas manual terkait.
3. **Rule 3: Impulse & Emotional Spikes (`IMPULSE_BUY`):** Lonjakan belanja non-esensial di tanggal kembar/akhir pekan/pasca-gajian ($\ge$ 2.0x rata-rata harian).
4. **Rule 4: Lifestyle Creep (`LIFESTYLE_CREEP`):** Kenaikan rasio pos *Discretionary* yang menekan rasio tabungan.
5. **Rule 5: PayLater Dependency (`PAYLATER_OVERUSE`):** Belanja gaya hidup via PayLater melebihi 20% total pengeluaran bulanan.

### F6. Analitik & Laporan Finansial
* Agregasi laporan berkala (Harian, Bulanan, Tahunan).
* Perhitungan Total Pengeluaran, Total Pemasukan, Akumulasi Utang PayLater, dan Saldo Bersih (*Net Balance*).

---

## 4. Non-Functional Requirements & Performance Standards

* **100% Offline & Zero Network Dependencies:** Tidak menggunakan Retrofit/OkHttp maupun SDK Cloud Analytics.
* **Immunity Shield Background Service:** `NotificationListenerService` dilindungi perisai `try-catch` agar tidak pernah memicu *crash* di latar belakang.
* **Thread Safety & Offloading:** Pemrosesan *regex* dan analisis *leak detection* wajib di-offload ke `Dispatchers.Default` / `Dispatchers.IO`.
* **Monetary Contract Precision:** Seluruh nilai mata uang wajib bertipe `Long` (Rupiah bulat, misal: `50000L`). **Dilarang keras memakai `Float` atau `Double`.**
* **Local Audit Log:** Menyimpan 100 baris log sistem terbaru di penyimpanan lokal untuk kemudahan debugging offline.

---

## 5. UI/UX & High-Craft Design Standards

Sesuai panduan `compose-craft/SKILL.md` (mengadopsi pola Tivi, Now in Android, & Architecture Samples):
* **Tactile Press Physics:** Efek tekan kenyal (*spring animation*) pada seluruh tombol dan item daftar (`pressClickEffect()`).
* **Presisi Tipografi Finansial:** Menggunakan *tabular numbers* (`fontFeatureSettings = "tnum"`) pada tampilan angka mata uang.
* **Seamless Edge-to-Edge:** Penggunaan `WindowInsets` yang presisi.
* **Sealed UiState Pattern:** Status layar dibungkus `sealed interface UiState` (`Loading`, `Empty`, `Success`, `Error`).
* **Stateless vs Stateful Composable Split:** Pemisahan wrapper ViewModel dan konten stateless murni untuk mendukung Compose `@Preview` instan.
* **Shimmer Skeleton Loading:** Menggunakan animasi shimmer sebagai pengganti *loading indicator* biasa.

---

## 6. Technical Stack & Architecture Architecture

* **Bahasa:** Kotlin 100%
* **Arsitektur:** Clean Architecture + MVVM (Model-View-ViewModel)
* **UI Framework:** Jetpack Compose (Material 3)
* **Database & Storage:** Room Database (SQLite) + Kotlin Coroutines & Flow
* **Dependency Injection:** Hilt
* **Build System:** Gradle Kotlin DSL (`.gradle.kts`) + Version Catalog (`libs.versions.toml`)
* **CI/CD:** GitHub Actions Workflow (`.github/workflows/android-ci.yml`)

---

## 7. Delivery Roadmap & Milestones

* **Phase 0: Project Foundation & Gradle Catalog** *(Dalam Proses)*
  * Setup Gradle Version Catalog (`libs.versions.toml`), `build.gradle.kts`, `AndroidManifest.xml`, `Application`, & `MainActivity`.
* **Phase 1: Core Domain & Regex Parser Engine**
  * Data class domain murni & rule parser e-wallet (GoPay, DANA, ShopeePay/SPayLater, BCA).
* **Phase 2: Local Storage (Room Database & Repositories)**
  * Room Entity, DAO, Database, & Repository implementation.
* **Phase 3: Background Notification Listener & Spending Leak Engine**
  * `NotificationListenerService` + `AnalyticsUseCase` leak rules.
* **Phase 4: High-Craft Compose UI Screens**
  * Dashboard, Draft Verification, Transaction List/Filter, Analytics Leak Report, & Notification Permission Onboarding Guide.
* **Phase 5: Automated Testing, Performance Audit, & GitHub Release**
  * Unit test Room/ViewModel, Compose performance audit, & rilis APK pertama.
