package com.samsung.android.otpforwarder.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.samsung.android.otpforwarder.core.database.dao.EmailDestinationDao
import com.samsung.android.otpforwarder.core.database.dao.ForwardingRecordDao
import com.samsung.android.otpforwarder.core.database.dao.SmsDestinationDao
import com.samsung.android.otpforwarder.core.database.entity.EmailDestinationEntity
import com.samsung.android.otpforwarder.core.database.entity.ForwardingRecordEntity
import com.samsung.android.otpforwarder.core.database.entity.SmsDestinationEntity

/**
 * Single Room database for the OTP Forwarder app.
 *
 * Version history:
 *   1 — initial schema (forwarding_records table).
 *   2 — added sms_destinations + email_destinations tables (2026-05-13, Rules → Destinations
 *       migration). No data migration: the v1 schema had no rules data persisted yet, so we
 *       rely on [Room.fallbackToDestructiveMigration] in [DatabaseModule] during pre-release.
 *       Replace with a real Migration once the app is shipped.
 *
 * [exportSchema] stays false until automated migration testing is wired up.
 */
@Database(
    entities = [
        ForwardingRecordEntity::class,
        SmsDestinationEntity::class,
        EmailDestinationEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class OtpDatabase : RoomDatabase() {
    abstract fun forwardingRecordDao(): ForwardingRecordDao
    abstract fun smsDestinationDao(): SmsDestinationDao
    abstract fun emailDestinationDao(): EmailDestinationDao
}
