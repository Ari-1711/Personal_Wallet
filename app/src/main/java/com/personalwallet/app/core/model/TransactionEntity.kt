package com.personalwallet.app.core.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["status"]),
        Index(value = ["source_account_id"]),
        Index(value = ["is_leak_risk"]),
        Index(value = ["deduplication_hash"], unique = true) // Mencegah duplikasi notifikasi saat service restart
    ],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_account_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["destination_account_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "amount") val amount: Long, // Rupiah bulat
    @ColumnInfo(name = "type") val type: TransactionType,
    @ColumnInfo(name = "source") val source: TransactionSource,
    @ColumnInfo(name = "status") val status: TransactionStatus,
    @ColumnInfo(name = "source_account_id") val sourceAccountId: Long,
    @ColumnInfo(name = "destination_account_id") val destinationAccountId: Long? = null, // Diisi jika tipe = TRANSFER
    @ColumnInfo(name = "merchant_or_title") val merchantOrTitle: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long, // Epoch millis (Local)
    @ColumnInfo(name = "category_type") val categoryType: ExpenseCategoryType,
    @ColumnInfo(name = "sub_category") val subCategory: String,
    @ColumnInfo(name = "is_leak_risk") val isLeakRisk: Boolean = false,
    @ColumnInfo(name = "leak_trigger_type") val leakTriggerType: LeakTriggerType = LeakTriggerType.NONE,
    @ColumnInfo(name = "action_suggestion") val actionSuggestion: String? = null,
    @ColumnInfo(name = "raw_notification_text") val rawNotificationText: String? = null,
    @ColumnInfo(name = "deduplication_hash") val deduplicationHash: String? = null
)
