# Database Schema & Data Contracts: Personal_Wallet

## 1. Database Overview

* **Engine:** Room Database (SQLite Abstraction).
* **Database Name:** `personal_wallet_db`.
* **Prinsip Utama:** Skema tabel di-desain dengan indeks terindeks ketat untuk menjamin kueri agregasi laporan berjalan **< 2ms** secara lokal.

---

## 2. Table Definitions & Constraints

### A. Table: `accounts` (Dompet & Akun Finansial)

Mengakomodasi rekening kas/debit maupun akun utang PayLater/Kartu Kredit.

```sql
CREATE TABLE IF NOT EXISTS `accounts` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `name` TEXT NOT NULL,                  -- Contoh: "BCA Utama", "SPayLater", "Dompet Tunai"
    `wallet_type` TEXT NOT NULL,           -- Enum: 'CASH_OR_DEBIT', 'CREDIT_OR_PAYLATER'
    `initial_balance` INTEGER NOT NULL,    -- Saldo awal/modal (Rupiah Long)
    `current_balance` INTEGER NOT NULL     -- Saldo akumulasi aktif
);
```

### B. Table: `transactions` (Buku Besar Transaksi)

Menyimpan seluruh transaksi manual maupun hasil ekstraksi notifikasi.

```sql
CREATE TABLE IF NOT EXISTS `transactions` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `amount` INTEGER NOT NULL,                       -- Rupiah bulat (Long)
    `type` TEXT NOT NULL,                            -- Enum: 'EXPENSE', 'INCOME', 'TRANSFER'
    `source` TEXT NOT NULL,                          -- Enum: 'NOTIFICATION', 'MANUAL'
    `status` TEXT NOT NULL,                          -- Enum: 'PENDING', 'CONFIRMED', 'REJECTED'
    `source_account_id` INTEGER NOT NULL,            -- Akun asal
    `destination_account_id` INTEGER,                -- Diisi jika type = 'TRANSFER'
    `merchant_or_title` TEXT NOT NULL,               -- Nama toko / judul transaksi
    `timestamp` INTEGER NOT NULL,                    -- Epoch Millisecond (Local)
    `category_type` TEXT NOT NULL,                   -- Enum: 'FIXED', 'VARIABLE_ESSENTIAL', 'DISCRETIONARY', dll
    `sub_category` TEXT NOT NULL,                    -- Subkategori (misal: 'FNB_LIFESTYLE')
    `is_leak_risk` INTEGER NOT NULL DEFAULT 0,       -- Boolean: 1 jika terdeteksi bocor
    `leak_trigger_type` TEXT NOT NULL,               -- Enum: 'NONE', 'MICRO_EXPENSE', 'IMPULSE_BUY', dll
    `action_suggestion` TEXT,                        -- Saran tindakan hemat
    `raw_notification_text` TEXT,                    -- Teks notifikasi mentah
    `deduplication_hash` TEXT,                       -- SHA-256 hash unik untuk idempotency
    FOREIGN KEY(`source_account_id`) REFERENCES `accounts`(`id`) ON DELETE RESTRICT,
    FOREIGN KEY(`destination_account_id`) REFERENCES `accounts`(`id`) ON DELETE SET NULL
);

-- Indeks Kinerja Kueri
CREATE INDEX IF NOT EXISTS `index_transactions_timestamp` ON `transactions` (`timestamp`);
CREATE INDEX IF NOT EXISTS `index_transactions_status` ON `transactions` (`status`);
CREATE INDEX IF NOT EXISTS `index_transactions_source_account_id` ON `transactions` (`source_account_id`);
CREATE INDEX IF NOT EXISTS `index_transactions_is_leak_risk` ON `transactions` (`is_leak_risk`);
CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_deduplication_hash` ON `transactions` (`deduplication_hash`);
```

### C. Table: `unparsed_notifications` (Log Fallback Notifikasi Mentah)

Menampung teks notifikasi yang gagal dicocokkan pola *regex*.

```sql
CREATE TABLE IF NOT EXISTS `unparsed_notifications` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `package_name` TEXT NOT NULL,          -- Contoh: "com.gojek.app", "com.bca"
    `raw_text` TEXT NOT NULL,              -- Teks notifikasi mentah
    `received_timestamp` INTEGER NOT NULL, -- Epoch Millisecond
    `is_resolved` INTEGER NOT NULL DEFAULT 0
);
```

---

## 3. Akuntansi & Regresi PayLater (Rules)

1. **Belanja Pakai PayLater:**
   * `type`: `EXPENSE`
   * `source_account_id`: ID Akun PayLater (tipe `CREDIT_OR_PAYLATER`)
   * `category_type`: `DISCRETIONARY` / `FIXED`
2. **Pelunasan Tagihan PayLater:**
   * `type`: `TRANSFER`
   * `source_account_id`: ID Akun Kas/BCA (`CASH_OR_DEBIT`)
   * `destination_account_id`: ID Akun PayLater (`CREDIT_OR_PAYLATER`)
   * **Penting:** Tidak dicatat sebagai `EXPENSE` baru untuk mencegah *double counting*.

---

## 4. Migration Strategy Guidelines

* Seluruh perubahan skema wajib menggunakan berkas migrasi eksplisit (`Migration(oldVersion, newVersion)`).
* **Dilarang keras** menggunakan `fallbackToDestructiveMigration()` pada rilis produksi agar data finansial pengguna tidak pernah terhapus.
