# Project Rules: Personal_Wallet (Local-First Offline Android App)

## 1. Project Goal & High-Level Summary
- **Deskripsi singkat:** Aplikasi pencatat keuangan personal berbasis local-first (100% offline) untuk Android. Mendukung pencatatan transaksi multi-dompet (Kas/Debit vs Kredit/PayLater) melalui ekstraksi notifikasi otomatis (GoPay, DANA, ShopeePay/SPayLater, BCA) dan input manual untuk expense, income, dan transfer/pelunasan.
- **Target pengguna / Output utama:** Android Native App (Jetpack Compose Material 3), Min SDK 26, Target SDK 34+.
- **Penyimpanan:** 100% Local Device Storage (Room SQLite, tanpa ketergantungan cloud backend).

## 2. Core Philosophy & Design Rules
- **Local-First & Zero Leak:** Data finansial, log notifikasi tetap di perangkat.
- **Double-Counting Prevention (Utang & PayLater):**
  - Belanja via PayLater/Kartu Kredit = `EXPENSE` pada dompet kredit.
  - Pembayaran/pelunasan tagihan PayLater dari Kas = `TRANSFER` (Kas -> PayLater), bukan Expense baru.
- **Single Source of Truth (Single Ledger Balance Consistency):**
  - `currentBalance` tersinkronisasi konsisten (saldo awal + agregasi `CONFIRMED`).
- **Deterministic Rule-Based Detection:** 5 aturan deteksi kebocoran dana lokal offline.
- **Draft-First Ingestion:** Ekstraksi notifikasi masuk sebagai `PENDING` untuk verifikasi 1 ketukan.
- **Graceful Ingestion & Unparsed Fallback:** Regex mismatch disimpan ke `unparsed_notifications`.

## 3. Expense Classification & Spending Leak Detection Engine
### A. Expense Classification Hierarchy:
1. **Fixed Expenses (`FIXED`):** `RENT_HOUSING`, `UTILITIES`, `FINANCIAL_OBLIGATIONS`.
2. **Variable Essentials (`VARIABLE_ESSENTIAL`):** `GROCERIES`, `COMMUTE`, `COMMUNICATION`.
3. **Discretionary Spending (`DISCRETIONARY`):** `FNB_LIFESTYLE`, `ENTERTAINMENT_SUBSCRIPTIONS`, `PERSONAL_SHOPPING`.

### B. 5 Aturan Deteksi Kebocoran (Spending Leak Detection):
1. **The Micro-Expense Leak (`MICRO_EXPENSE` / Latte Factor):** Discretionary repetitif < Rp25.000 frekuensi >= 3-4x/minggu.
2. **Zombie Subscriptions (`ZOMBIE_SUB`):** Pemotongan langganan berkala pasif tanpa interaksi aktif.
3. **Impulse & Emotional Spikes (`IMPULSE_BUY`):** Lonjakan >= 2.0x di tanggal rawan (kembar / weekend / gajian).
4. **Lifestyle Creep (`LIFESTYLE_CREEP`):** Rasio discretionary meningkat menekan savings rate.
5. **PayLater / Debt Spiral Dependency (`PAYLATER_OVERUSE`):** Belanja discretionary via PayLater > 20% total pengeluaran.
