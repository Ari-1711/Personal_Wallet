# Postman Test Data & Mock Collections - Personal_Wallet

Folder ini berisi berkas pengujian JSON, CSV, dan Koleksi Postman untuk aplikasi **Personal_Wallet**.

## Berkas yang Tersedia:

1. **`Postman_Collection_Personal_Wallet.json`**:
   - **Koleksi Postman v2.1.0** lengkap dengan struktur folder request: *Notifications (Ingestion Feed)* dan *Transactions (Buku Kas)*.
   - Cara pakai: Buka **Postman** -> Klik **Import** -> Pilih file `Postman_Collection_Personal_Wallet.json`.

2. **`sample_transactions.json`**:
   - Array JSON memuat 5 skenario contoh transaksi (Pengeluaran Kopi, Belanja Indomaret, Belanja PayLater, Pelunasan Tagihan PayLater, dan Gaji Bulanan) lengkap dengan flag *Leak Trigger* dan *CategoryType*.
   - Cocok digunakan untuk pengujian Body Request atau Data Runner di Postman.

3. **`sample_transactions.csv`**:
   - Format CSV dari contoh transaksi di atas.
   - Cocok digunakan untuk **Postman Collection Runner** (fitur *Run Collection with CSV data*).

4. **`sample_notifications.json`**:
   - Payload JSON berisi contoh notifikasi mentah dari **GoPay, m-BCA, ShopeePay/SPayLater, dan DANA** untuk menguji aturan ekstrak regex di parser.
