package com.personalwallet.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personalwallet.app.core.model.UnparsedNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UnparsedNotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnparsedNotification(notification: UnparsedNotificationEntity)

    @Query("SELECT * FROM unparsed_notifications WHERE is_resolved = 0 ORDER BY received_timestamp DESC")
    fun getUnresolvedNotifications(): Flow<List<UnparsedNotificationEntity>>
    
    @Query("UPDATE unparsed_notifications SET is_resolved = 1 WHERE id = :id")
    suspend fun markAsResolved(id: Long)
}
