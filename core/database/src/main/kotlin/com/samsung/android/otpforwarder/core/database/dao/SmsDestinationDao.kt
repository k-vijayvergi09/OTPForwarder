package com.samsung.android.otpforwarder.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.samsung.android.otpforwarder.core.database.entity.SmsDestinationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsDestinationDao {

    /** Observe every SMS destination, alphabetical by label so the list is stable. */
    @Query("SELECT * FROM sms_destinations ORDER BY label COLLATE NOCASE ASC, phoneNumber ASC")
    fun observeAll(): Flow<List<SmsDestinationEntity>>

    /** Observe only destinations the user has currently enabled. */
    @Query("SELECT * FROM sms_destinations WHERE isEnabled = 1 ORDER BY label COLLATE NOCASE ASC, phoneNumber ASC")
    fun observeEnabled(): Flow<List<SmsDestinationEntity>>

    /**
     * One-shot fetch of every enabled destination, used by [ForwardingWorker] to
     * compute the fan-out list without subscribing to the Flow.
     */
    @Query("SELECT * FROM sms_destinations WHERE isEnabled = 1 ORDER BY label COLLATE NOCASE ASC, phoneNumber ASC")
    suspend fun getEnabledOnce(): List<SmsDestinationEntity>

    @Upsert
    suspend fun upsert(destination: SmsDestinationEntity)

    @Query("DELETE FROM sms_destinations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE sms_destinations SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)
}
