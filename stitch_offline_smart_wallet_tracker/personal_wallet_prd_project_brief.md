# Product Requirement Document (PRD) & Project Brief: Personal_Wallet
**Versi Dokumen:** 1.0 (Final)  
**Status Proyek:** Siap Implementasi / Production-Ready Blueprint  
**Arsitektur & Platform:** Android Native (Jetpack Compose, Material 3, Room SQLite, 100% Local-First Offline)

---

## 1. Executive Summary & Visi Produk

### 1.1 Latar Belakang
Aplikasi keuangan modern di Indonesia kerap menuntut sinkronisasi akun perbankan melalui API pihak ketiga, menghubungkan cloud pihak ketiga, atau menyajikan iklan yang mengganggu privasi. Di sisi lain, adopsi PayLater (SPayLater, GoPayLater, Kredivo) yang masif sering menimbulkan masalah **"double counting"** pencatatan: pengguna mencatat transaksi saat membeli barang dengan PayLater, lalu mencatatnya kembali sebagai pengeluaran saat membayar tagihan bulanan. Selain itu, pengeluaran impulsif mikro (*bocor halus*) dan langganan pasif (*zombie subscriptions*) sulit terlacak jika pencatatan harus dilakukan manual satu per satu.

### 1.2 Visi & Nilai Unik
**Personal_Wallet** adalah aplikasi pencatat keuangan personal berbasis **100% Local-First & Zero Telemetry** untuk Android. Seluruh kalkulasi, penyimpanan riwayat transaksi, dan deteksi kebocoran dana dieksekusi secara lokal di perangkat pengguna menggunakan database SQLite/Room terenkripsi, tanpa cloud backend atau analitik pihak ketiga.

#### 4 Pilar Utama Personal_Wallet:
1. **100% Local-First & Zero Leak**: Data finansial dan notifikasi tidak pernah meninggalkan perangkat pengguna (Bebas cloud breach & zero telemetry).
2. **Double-Counting Prevention (Utang vs Kas)**: Pemisahan tegas antara dompet likuid (*Cash/Debit*) dan dompet kredit (*PayLater/Credit Card*). Belanja PayLater adalah beban kredit, sedangkan pembayaran tagihan adalah mutasi internal/transfer netral.
3. **Draft-First Notification Ingestion**: Membaca notifikasi transaksi perbankan/e-wallet lokal (BCA, GoPay, Shopee/SPayLater, DANA) dan menyajikannya sebagai *Draft* untuk verifikasi 1 ketukan tanpa auto-insert liar.
4. **Deterministic Rule-Based Leak Engine**: Algoritma offline berbasis aturan cerdas untuk mendeteksi *Bocor Halus*, *Zombie Subscriptions*, dan dependensi PayLater tanpa model machine learning yang berat.

---

## 2. Sasaran Target & Persona Pengguna

- **Persona Utama: "Pekerja Muda Urban (22–35 tahun)"**
  - Menggunakan kombinasi rekening kas (BCA, Mandiri), e-wallet harian (GoPay, DANA), dan PayLater e-commerce (SPayLater).
  - Mengutamakan privasi finansial, tidak ingin data perbankan terunggah ke server cloud.
  - Sering mengalami kejutan tagihan di akhir bulan akibat pengeluaran kecil berulang (*kopi kekinian, biaya admin, ojek online, langganan streaming*).
- **Kebutuhan Pengguna**:
  - Tampilan visual yang tenang (*calm UI*), minim beban kognitif, tidak membuat pusing dengan teks berlebihan.
  - Alur input transaksi cepat (< 5 detik) dan pencatatan draft semi-otomatis dari notifikasi SMS/Push Notification perbankan.

---

## 3. Scope & Inventaris Layar Desain (Design Map)

Personal_Wallet dirancang menggunakan sistem desain **Obsidian Sage** (Dark Mode, warna aksen Emerald/Sage lembut `#2ea043`, tipografi Manrope/Inter ergonomis).

### A. Layar Navigasi Utama (4 Tab Utama)
1. **Tab 1 — Dashboard & Ringkasan Saldo (`SCREEN_9`)**:
   - Total Net Worth (Kas Bersih = Saldo Kas - Total Tagihan PayLater).
   - Kartu multi-dompet ringkas (BCA, GoPay, Tunai, SPayLater).
   - Ringkasan Arus Kas Bulan Berjalan (Pemasukan vs Pengeluaran).
   - Radar Bocor Halus & Widget Akses Cepat Riwayat Transaksi.
2. **Tab 2 — Draft Ingestion Notifikasi (`SCREEN_12` & `SCREEN_18`)**:
   - Feed transaksi pending hasil ekstraksi push notification/SMS bank secara lokal.
   - Aksi cepat 1 ketukan: *Setujui (Konfirmasi)*, *Edit Cepat*, atau *Abaikan (Tolak)*.
   - Penanganan fallback untuk notifikasi gagal parse (*Unparsed Ingestion*).
3. **Tab 3 — Deteksi Kebocoran Dana / Spending Leak Deep Dive (`SCREEN_11` & `SCREEN_14`)**:
   - Visualisasi 5 Aturan Deteksi Kebocoran Keuangan.
   - Peringatan *Zombie Subscriptions*, rasio *Latte Factor*, dan metrik ketergantungan PayLater.
4. **Tab 4 — Dompet & Pelunasan Tenang (`SCREEN_10` & `SCREEN_16`)**:
   - Manajemen pembagian dompet: *Kas Likuid* vs *Utang/Kredit*.
   - Rincian jatuh tempo tagihan PayLater dan riwayat pelunasan bersih.

### B. Layar Alur Aksi (Action Flows & Form Modals)
5. **Modal Catat Pengeluaran Manual (`SCREEN_7`)**:
   - Input nominal besar cepat dengan shortcut (`+10rb`, `+50rb`, `+100rb`).
   - Selector 3 tingkatan kategori alokasi anggaran (Gaya Hidup, Esensial, Tetap).
   - Deteksi instan *Bocor Halus Radar* saat nominal < Rp 25.000 pada kategori gaya hidup.
6. **Modal Catat Pemasukan Manual (`SCREEN_6`)**:
   - Input pemasukan dengan pilihan dompet likuid penerima (BCA, GoPay, Tunai, Mandiri).
   - Kategori pemasukan terstruktur: Pendapatan Aktif (Gaji, Bonus), Sampingan (Freelance, Online Shop), Pasif (Investasi, Bunga).
7. **Modal Pelunasan PayLater & Transfer Antar Dompet (`SCREEN_5`)**:
   - Mekanisme anti-double counting: pemilihan dompet sumber kas (BCA) dan dompet liabilitas target (SPayLater).
   - Pilihan instan pelunasan penuh (*Lunas Semua*) dan pengelolaan biaya admin transfer.
8. **Halaman Riwayat Transaksi Lengkap / Buku Kas (`SCREEN_3`)**:
   - Timeline transaksi lengkap dikelompokkan per tanggal.
   - Filter cepat (Bocor Halus, Pengeluaran, Pemasukan, Mutasi Transfer).
   - Fitur ekspor data mandiri: **Unduh CSV** dan **Cadangkan JSON**.

---

## 4. Spesifikasi Logika Bisnis & Mesin Keuangan

### 4.1 Hierarki Pengeluaran (Expense Hierarchy)
Semua pengeluaran diklasifikasikan ke dalam 3 hierarki ketat:
1. **Fixed Expenses (`FIXED`)**: Biaya wajib berulang tidak terhindarkan (*Sewa Kos, Listrik & Air, Asuransi*).
2. **Variable Essentials (`VARIABLE_ESSENTIAL`)**: Kebutuhan pokok dengan nilai fluktuatif (*Bahan Pokok/Supermarket, Bensin/Transportasi, Paket Data*).
3. **Discretionary Spending (`DISCRETIONARY`)**: Keinginan dan gaya hidup yang dapat ditekan (*Kopi/Jajan, Makan di Luar, Hiburan, Belanja Impulsif*).

### 4.2 Mesin Deteksi 5 Kebocoran Keuangan (Offline Rule Engine)
Mesin deteksi berjalan secara deterministik di lokal perangkat tanpa AI eksternal:
1. **The Micro-Expense Leak (`MICRO_EXPENSE` / Latte Factor)**:
   - *Kriteria*: Pengeluaran kategori `DISCRETIONARY` dengan nominal < Rp 25.000 dengan frekuensi $\ge$ 3 kali dalam 7 hari terakhir.
2. **Zombie Subscriptions (`ZOMBIE_SUB`)**:
   - *Kriteria*: Pemotongan transaksi otomatis berulang bulanan (misal: Netflix, Spotify, Gym) yang tidak mengalami interaksi pembacaan/peninjauan aktif selama > 45 hari.
3. **Impulse & Emotional Spikes (`IMPULSE_BUY`)**:
   - *Kriteria*: Lonjakan total belanja harian $\ge 2.0\times$ rata-rata harian yang terkonsentrasi di tanggal rawan (Promo Tanggal Kembar 10.10/11.11, Akhir Pekan, atau H+1 sd H+3 Gajian).
4. **Lifestyle Creep (`LIFESTYLE_CREEP`)**:
   - *Kriteria*: Terjadi kenaikan rasio `DISCRETIONARY` terhadap total pengeluaran bulanan yang menyebabkan persentase tabungan (*savings rate*) turun selama 2 bulan berturut-turut.
5. **PayLater Dependency Spiral (`PAYLATER_OVERUSE`)**:
   - *Kriteria*: Porsi belanja gaya hidup (`DISCRETIONARY`) yang dibayar menggunakan saldo kredit/PayLater melebihi **20%** dari total belanja bulanan.

### 4.3 Logika Single Source of Truth & Pencegahan Dobel Beban
$$\text{Net Worth} = \sum (\text{Saldo Dompet Kas}) - \sum (\text{Tagihan Dompet Kredit/PayLater})$$
- Transaksi belanja dengan PayLater:
  - `Wallet.creditBalance` bertambah (utang naik).
  - Tercatat sebagai `EXPENSE` kategori bersangkutan.
- Transaksi pelunasan tagihan PayLater via Kas:
  - Dicatat sebagai tipe transaksi `TRANSFER` (Internal Movement).
  - Saldo Kas berkurang, utang PayLater berkurang secara seimbang.
  - **Dilarang keras** dicatat sebagai `EXPENSE` baru agar neraca pengeluaran tidak terhitung 2 kali.

---

## 5. Arsitektur Teknis & Database (Android Native)

### 5.1 Spesifikasi Platform
- **Target SDK**: Android 14+ (API 34) | **Min SDK**: API 26 (Android 8.0 Oreo).
- **Bahasa & Framework**: Kotlin 1.9+, Jetpack Compose (Material 3), Kotlin Coroutines & Flow.
- **Dependency Injection**: Hilt / Koin.
- **Local Database**: Room Persistence Library (SQLite lokal terenkripsi dengan SQLCipher).

### 5.2 Skema Entitas Database Utama (Data Model Room)
```kotlin
// 1. Entitas Dompet
@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey val id: String,
    val name: String,             // e.g. "BCA", "GoPay", "SPayLater"
    val type: WalletType,         // CASH, BANK, EWALLET, CREDIT_PAYLATER
    val initialBalance: Long,     // Saldo awal dalam Rupiah
    val currentBalance: Long,     // Saldo berjalan tersinkronisasi
    val creditLimit: Long? = null // Limit maksimal jika bertipe CREDIT_PAYLATER
)

// 2. Entitas Transaksi
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val amount: Long,
    val type: TransactionType,    // EXPENSE, INCOME, TRANSFER
    val sourceWalletId: String,
    val targetWalletId: String?,  // Terisi jika transfer/pelunasan
    val category: ExpenseCategory?,
    val note: String,
    val isLeakDetected: Boolean = false,
    val leakRuleType: LeakType? = null,
    val status: TransactionStatus // PENDING_DRAFT, CONFIRMED, REJECTED
)

// 3. Entitas Cadangan Log Notifikasi
@Entity(tableName = "unparsed_notifications")
data class UnparsedNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id Long = 0,
    val rawSender: String,
    val rawBody: String,
    val receivedAt: Long
)
```

---

## 6. Roadmap Pengembangan & Milestone

| Fase | Durasi | Milestone & Deliverable Utama |
| :--- | :--- | :--- |
| **Fase 1: Core Engine & Room DB** | Minggu 1–2 | Setup arsitektur MVI/MVVM, skema Room DB SQLite, kalkulator single balance ledger, dan unit test anti double-counting. |
| **Fase 2: UI Compose (Design System)** | Minggu 3–4 | Implementasi tema *Obsidian Sage*, 4 layar utama (Dashboard, Draft, Deteksi, Dompet), dan 3 modal formulir manual. |
| **Fase 3: Notification Listener Service** | Minggu 5–6 | Integrasi `NotificationListenerService` Android, parser regex offline (BCA, GoPay, SPayLater), dan feed *Draft Ingestion*. |
| **Fase 4: Spending Leak Engine** | Minggu 7 | Implementasi 5 aturan deteksi bocor halus offline, filter buku kas, dan visualisasi analisis grafik bulanan. |
| **Fase 5: Ekspor Mandiri & Hardening** | Minggu 8 | Enkripsi database SQLCipher, fitur ekspor CSV/JSON offline, pengujian performa memori, dan rilis APK internal. |

---

## 7. Metrik Keberhasilan (Success Metrics)
1. **0 Network Permissions**: Aplikasi dapat berjalan sempurna tanpa izin `android.permission.INTERNET`.
2. **Kecepatan Pencatatan**: Waktu dari membuka aplikasi hingga menyimpan transaksi manual < 5 detik.
3. **Akurasi Parser Draft**: Ekstraksi nominal, merchant, dan dompet dari notifikasi bank mencapai akurasi $\ge 95\%$.
4. **Zero Double-Counting**: Mutasi pelunasan PayLater 100% konsisten antara pengurangan kas dan pemulihan limit kredit.