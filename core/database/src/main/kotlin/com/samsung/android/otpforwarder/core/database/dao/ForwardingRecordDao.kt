package com.samsung.android.otpforwarder.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.samsung.android.otpforwarder.core.database.entity.ForwardingRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ForwardingRecordDao {

    /**
     * Observe all records ordered newest-first.
     * Returns a [Flow] that re-emits whenever the table changes.
     */
    @Query("SELECT * FROM forwarding_records ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<ForwardingRecordEntity>>

    /**
     * Insert or replace a record.
     * Using [Upsert] means [recordDetectedOtp] and [addRecord] are both safe to
     * call for the same ID without producing duplicates.
     */
    @Upsert
    suspend fun upsert(record: ForwardingRecordEntity)

    /**
     * Patch only the status, error, and updatedAt fields.
     * Avoids re-writing the full row and keeps the OTP body / sender stable.
     */
    @Query(
        """
        UPDATE forwarding_records
           SET status       = :status,
               errorMessage = :errorMessage,
               updatedAt    = :updatedAt
         WHERE id = :id
        """
    )
    suspend fun updateStatus(
        id: String,
        status: String,
        errorMessage: String?,
        updatedAt: Long,
    )

    /**
     * Patch the destinations field after a successful forwarding attempt.
     */
    @Query(
        """
        UPDATE forwarding_records
           SET destinations = :destinations,
               updatedAt    = :updatedAt
         WHERE id = :id
        """
    )
    suspend fun updateDestinations(
        id: String,
        destinations: String,
        updatedAt: Long,
    )
}
