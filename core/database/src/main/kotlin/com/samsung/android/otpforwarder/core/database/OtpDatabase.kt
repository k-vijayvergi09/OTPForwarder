package com.samsung.android.otpforwarder.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.samsung.android.otpforwarder.core.database.dao.ForwardingRecordDao
import com.samsung.android.otpforwarder.core.database.entity.ForwardingRecordEntity

/**
 * Single Room database for the OTP Forwarder app.
 *
 * Version history:
 *   1 — initial schema (forwarding_records table).
 *
 * [exportSchema] is false for now; enable and add a schemas/ directory when
 * automated migration testing is needed.
 */
@Database(
    entities = [ForwardingRecordEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class OtpDatabase : RoomDatabase() {
    abstract fun forwardingRecordDao(): ForwardingRecordDao
}
