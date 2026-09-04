package com.personalwallet.app.core.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unparsed_notifications")
data class UnparsedNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "raw_text") val rawText: String,
    @ColumnInfo(name = "received_timestamp") val receivedTimestamp: Long,
    @ColumnInfo(name = "is_resolved") val isResolved: Boolean = false
)
