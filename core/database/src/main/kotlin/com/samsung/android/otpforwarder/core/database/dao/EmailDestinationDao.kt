package com.samsung.android.otpforwarder.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.samsung.android.otpforwarder.core.database.entity.EmailDestinationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailDestinationDao {

    @Query("SELECT * FROM email_destinations ORDER BY label COLLATE NOCASE ASC, emailAddress ASC")
    fun observeAll(): Flow<List<EmailDestinationEntity>>

    @Query("SELECT * FROM email_destinations WHERE isEnabled = 1 ORDER BY label COLLATE NOCASE ASC, emailAddress ASC")
    fun observeEnabled(): Flow<List<EmailDestinationEntity>>

    /** One-shot fetch for [ForwardingWorker] fan-out (parallel to SmsDestinationDao). */
    @Query("SELECT * FROM email_destinations WHERE isEnabled = 1 ORDER BY label COLLATE NOCASE ASC, emailAddress ASC")
    suspend fun getEnabledOnce(): List<EmailDestinationEntity>

    @Upsert
    suspend fun upsert(destination: EmailDestinationEntity)

    @Query("DELETE FROM email_destinations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE email_destinations SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)
}
