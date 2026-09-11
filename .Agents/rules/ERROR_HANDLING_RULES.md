---
trigger: always_on
---

# Error Handling & Logging Rules: Personal_Wallet

## 1. Core Philosophy & Error Boundaries

* **Zero Uncaught Crashes:** Karena aplikasi ini berbasis *local-first* dan berjalan di latar belakang (`NotificationListenerService`), **terjadinya crash fatal di latar belakang adalah dosa besar**. Semua eksekusi tidak aman wajib dibungkus pemrosesan galat yang rapi.
* **Separation of Error Concerns:**
  * **System/Developer Logs:** Log detail internal (stacktrace, nama method, raw input, query error) untuk kebutuhan debugging developer.
  * **User UI Feedback:** Pesan terjemahan yang ramah pengguna (misal: *"Gagal menyimpan transaksi"*, *"Format nominal tidak valid"*), **bukan** pesan exception mentah seperti `SQLiteConstraintException`.
* **No Silent Drops:** Jika suatu proses gagal (misal: ekstraksi notifikasi gagal match regex), data **dilarang dibuang tanpa jejak**. Alihkan ke mekanisme *fallback* (`unparsed_notifications`).

---

## 2. Standardized Error Domain Mapping

Gunakan wrapper `Resource<T>` di seluruh lapisan *Domain* dan *Repository*, serta petakan Exception teknis ke kelas Domain Error yang terstruktur:

```kotlin
sealed class DomainError(
  override val message: String,
  override val cause: Throwable? = null
) : Exception(message, cause) {

  data class DatabaseError(override val message: String, override val cause: Throwable? = null) : DomainError(message, cause)
  data class ParsingError(val rawText: String, override val message: String, override val cause: Throwable? = null) : DomainError(message, cause)
  data class DuplicateTransactionError(val hash: String) : DomainError("Transaksi ganda terdeteksi dengan hash: $hash")
  data class ValidationError(val fieldName: String, override val message: String) : DomainError(message)
  data class UnknownError(override val message: String, override val cause: Throwable? = null) : DomainError(message, cause)
}
```

---

## 3. Standardized Logging Standards

### A. Tagging Strategy
Gunakan format tag terstruktur `[Layer/Feature]` agar mudah difilter di Logcat maupun file log lokal:

* `[Core/Parser]` -> Ekstraksi regex dan parsing notifikasi.
* `[Core/Database]` -> Operasi Room DAO, kueri, dan transaksi DB.
* `[Core/Domain]` -> Evaluasi *Spending Leak*, UseCases, dan aturan bisnis.
* `[Feature/Transaction]` -> ViewModel dan aksi UI terkait transaksi.

### B. Classification Log Level

| Log Level | Kapan Digunakan | Contoh Penggunaan |
| :--- | :--- | :--- |
| `Log.e()` (**ERROR**) | Kegagalan fatal atau korupsi data yang menggagalkan operasi utama. | Gagal membuka/menulis Room DB, kegagalan transaksi DB. |
| `Log.w()` (**WARN**) | Peringatan non-fatal. Sistem masih bisa berjalan via *fallback*. | Notifikasi tidak cocok dengan regex (disimpan ke `unparsed_notifications`), transaksi duplikat diabaikan. |
| `Log.i()` (**INFO**) | Kejadian penting yang menandai siklus hidup atau aksi sukses. | Transaksi baru dikonfirmasi, *NotificationListenerService* terhubung. |
| `Log.d()` (**DEBUG**) | Detail informasi untuk analisis pengembang saat *vibe coding*. | Hasil ekstraksi grup regex, parameter kueri analitik. |

---

## 4. Layer-Specific Error Handling Rules

### A. `NotificationListenerService` & Parser Layer
1. **Perisai Kebal (Immunity Shield):** Seluruh logika `onNotificationPosted()` wajib dibungkus blok `try-catch(t: Throwable)`.
2. **Fallback Flow:**
   ```kotlin
   try {
       val transaction = parserEngine.parse(packageName, text)
       repository.insertPendingTransaction(transaction)
       Log.i("[Core/Parser]", "Berhasil mengekstrak notifikasi dari $packageName")
   } catch (e: Exception) {
       Log.w("[Core/Parser]", "Gagal memproses notifikasi: ${e.localizedMessage}. Menyimpan ke log unparsed.")
       repository.saveUnparsedNotification(packageName, text)
   }
   ```

### B. Room Database & Repository Layer
1. **Handling Duplikasi (Unique Hash Collision):**
   * Gunakan `OnConflictStrategy.IGNORE` pada DAO atau tangkap `SQLiteConstraintException`.
   * Return `Resource.Error(DomainError.DuplicateTransactionError)` jika terjadi bentrokan *deduplication_hash*.
2. **Coroutine Flow Exception Handling:**
   * Tangkap galat pada `Flow` menggunakan operator `.catch {}`:
   ```kotlin
   fun getTransactions(): Flow<Resource<List<Transaction>>> = transactionDao
       .getTransactions()
       .map<List<TransactionEntity>, Resource<List<Transaction>>> { entities ->
           Resource.Success(entities.map { it.toDomain() })
       }
       .catch { throwable ->
           Log.e("[Core/Database]", "Gagal membaca transaksi", throwable)
           emit(Resource.Error(DomainError.DatabaseError("Gagal memuat data transaksi", throwable)))
       }
   ```

### C. ViewModel & Presentation Layer (Jetpack Compose)
1. **Coroutine Safety:** Setiap `viewModelScope.launch` yang melakukan operasi I/O atau UseCase wajib menggunakan `try-catch` atau `CoroutineExceptionHandler`:
   ```kotlin
   val handler = CoroutineExceptionHandler { _, exception ->
       Log.e("[Feature/Transaction]", "Uncaught ViewModel Exception", exception)
       _uiState.update { it.copy(errorMessage = "Terjadi kesalahan sistem yang tidak terduga") }
   }
   ```
2. **UI State Error Exposure:** Tampilkan pesan galat via `SnackbarHost`, `Toast`, atau komponen *Error State UI* yang jelas, dan sediakan tombol **"Coba Lagi" (Retry)** bila memungkinkan.

---

## 5. Local Audit Trail (Debugging di Perangkat)

Untuk membantu debugging tanpa perlu menyambungkan kabel USB ke Logcat Android Studio:
* Sediakan wrapper `AppLogger` yang selain menulis ke Android `Log`, juga menyimpan log berstatus `ERROR` & `WARN` (maksimal 100 baris terbaru) ke penyimpanan lokal/SharedPreferences/Room.
* Sediakan halaman tersembunyi/diagnostik di menu **Pengaturan $\rightarrow$ Log Sistem** agar pengguna dapat melihat atau menyalin log error saat melaporkan bug di GitHub Issues/Releases.
