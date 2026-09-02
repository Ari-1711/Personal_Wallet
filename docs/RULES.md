# Guardrails & Developer Rules: Personal_Wallet

## 1. Boundary & Guardrails AI Agent Scope

Untuk mencegah kesalahan pengubahan kuis atau skema database yang sudah berjalan, ditetapkan zona batasan berikut:

### 🔴 Zona Terlarang (READ-ONLY bagi AI Agent & Kontributor)
* `core/database/migrations/**` (Skema migrasi Room yang sudah rilis DILARANG diubah/dihapus).
* `core/model/TransactionEntity.kt` & `core/model/AccountEntity.kt` (Entitas inti database; perubahan wajib melalui file migrasi baru).
* `app/src/main/AndroidManifest.xml` (Izin sensitif sistem dilarang diubah tanpa konfirmasi pengguna).

### 🟢 Zona Kerja Yang Diizinkan (WRITE SCOPE)
* `feature/**` (Komposable UI, ViewModel, dan UI state).
* `core/parser/rules/**` (Pola regex ekstraksi e-wallet).
* `core/domain/**` (UseCases, *leak detection heuristics*, dan kalkulator analitik).
* `core/data/**` (Implementasi Room DAO dan Repository).

---

## 2. Coding Conventions & Quality Standards

* **100% Kotlin Idiomatic:** Utamakan immutability (`val`, `data class`, `copy()`).
* **Format Moneter Wajib `Long`:** Nilai mata uang wajib Rupiah bulat (`50000L`). Dilarang memakai `Float` atau `Double`.
* **Clean Code & Function Size:** Fungsi Composable atau UseCase tidak boleh melebihi 150 baris kode dalam satu fungsi.
* **Commit Conventions:** Mengikuti *Conventional Commits* (`feat:`, `fix:`, `refactor:`, `docs:`, `chore:`).

---

## 3. Error Handling Rules

* **Zero Uncaught Crashes:** Pemrosesan notifikasi di latar belakang (`NotificationListenerService`) wajib dibungkus `try-catch` agar service tidak dimatikan oleh OS Android.
* **Logging Tagging:** Seluruh log wajib diberi tag terstruktur `[Layer/Feature]` (misal: `[Core/Parser]`, `[Core/Database]`).
* **Pesan UI Ramah Pengguna:** UI mengonsumsi status `Resource.Error` dengan pesan terjemahan yang jelas, bukan exception mentah SQLite.
