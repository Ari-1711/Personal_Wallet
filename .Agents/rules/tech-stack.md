
---
trigger: always_on
---

# Tech Stack & Engineering Standards: Personal_Wallet

## 1. Project Context & Primary Stack
- **Repository / Project Name:** [Personal_Wallet](https://github.com/Ari-1711/Personal_Wallet)
- **Application Type:** Android Native Application (Local-First / 100% Offline)
- **Primary Language:** Kotlin (100%)
- **UI Framework:** Jetpack Compose (Material 3)
- **Core Engine & Background:** Android `NotificationListenerService` + Kotlin Coroutines & Flow
- **Local Database & Storage:** Room Database (SQLite Abstraction)
- **Dependency Injection:** Hilt
- **Build System & Tooling:** Gradle (Kotlin DSL - `.gradle.kts`)
- **Target Platform:** Android SDK 26 (Android 8.0 Oreo) hingga Android SDK 34+
- **Version Control & Distribution:** Git & GitHub (Distribusi instalasi publik via GitHub Releases)

---

## 2. Engineering & Architecture Standards
- **Arsitektur Aplikasi:**
  * Pola arsitektur: Clean Architecture + MVVM (Model-View-ViewModel).
  * Struktur modul: 
    - `ui/`: Komponen murni Composable, navigasi, dan tema.
    - `feature/`: ViewModel dan *screen-level state holders*.
    - `core/domain/`: UseCase independen, algoritma heuristik *leak detection*, dan model murni bisnis.
    - `core/parser/`: Mesin ekstraksi *regex* notifikasi (terisolasi total dari UI dan DAO).
    - `core/data/`: Implementasi Repository, Room Entity, dan DAO.
- **Komponen & Modularitas UI (Jetpack Compose):**
  * Pisahkan Composable menjadi *Stateful* (mengambil data dari ViewModel) dan *Stateless* (hanya menerima data via parameter dan memancarkan *event* / *lambda callback*).
  * Patuhi prinsip *Single Responsibility*: Hindari menulis Composable raksasa melebihi 150 baris dalam satu fungsi.
- **State Management & Reaktivitas:**
  * Wajib gunakan `StateFlow` di dalam ViewModel dan konsumsi di Compose menggunakan `collectAsStateWithLifecycle()`.
  * Hindari mutasi *state* langsung di UI; seluruh mutasi wajib melalui pemanggilan fungsi eksplisit di ViewModel.
- **Penanganan Status Data & Asinkron:**
  * Gunakan wrapper hasil standar `Resource<T>` (`Success`, `Error`, `Loading`) untuk setiap operasi database dan ekstraksi data.
  * Tampilkan umpan balik UI yang jelas untuk setiap status: *shimmer/loading indicator*, *empty state*, dan pesan galat deskriptif.
- **Presisi Moneter & Waktu:**
  * Nilai transaksi wajib bertipe `Long` (Rupiah bulat, misal `50000L`). **Dilarang memakai `Float` atau `Double`.**
  * Perhitungan tanggal dan analitik berkala wajib menggunakan API `java.time` (`LocalDate`, `Instant`, `ZoneId.systemDefault()`).

---

## 3. Integration, Git & Quality Standards
- **Commit Convention:**
  * Gunakan format *Conventional Commits*:
    - `feat:` (fitur baru, misal: *parser* DANA baru, filter analitik).
    - `fix:` (perbaikan *bug*, misal: *regex mismatch*, duplikasi data).
    - `refactor:` (perubahan struktur kode tanpa mengubah fungsionalitas).
    - `docs:` (pembaruan dokumentasi, `README.md`, atau berkas aturan).
    - `chore:` (konfigurasi Gradle, penambahan dependensi, atau *cleanup*).
- **Background Execution & Thread Safety:**
  * Operasi berat (evaluasi *regex*, pembacaan teks notifikasi, dan kalkulasi analitik) dilarang berjalan di *Main Thread* (`Dispatchers.Main`); wajib dialihkan ke *background thread* (`Dispatchers.Default` atau `Dispatchers.IO`).
- **Data Integrity & Schema Migration:**
  * Perubahan skema tabel Room wajib disertai berkas migrasi eksplisit (`Migration(x, y)`), hindari penggunaan `fallbackToDestructiveMigration()` di fase produksi.
- **Agent Incremental Edits:**
  * Pisahkan aturan *regex* per aplikasi e-wallet ke dalam berkas konfigurasi independen (misal: `GopayParserRule.kt`, `DanaParserRule.kt`) agar AI agent dapat memperbaiki pola ekstraksi tanpa menyentuh modul inti lainnya.

