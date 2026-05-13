package com.samsung.android.otpforwarder.core.database.di

import android.content.Context
import androidx.room.Room
import com.samsung.android.otpforwarder.core.database.OtpDatabase
import com.samsung.android.otpforwarder.core.database.dao.EmailDestinationDao
import com.samsung.android.otpforwarder.core.database.dao.ForwardingRecordDao
import com.samsung.android.otpforwarder.core.database.dao.SmsDestinationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideOtpDatabase(
        @ApplicationContext context: Context,
    ): OtpDatabase = Room.databaseBuilder(
        context,
        OtpDatabase::class.java,
        "otp_forwarder.db",
    )
        // Acceptable while we are pre-release. Replace with explicit Migration objects
        // before the first Play Store / direct-APK build.
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    fun provideForwardingRecordDao(db: OtpDatabase): ForwardingRecordDao =
        db.forwardingRecordDao()

    @Provides
    fun provideSmsDestinationDao(db: OtpDatabase): SmsDestinationDao =
        db.smsDestinationDao()

    @Provides
    fun provideEmailDestinationDao(db: OtpDatabase): EmailDestinationDao =
        db.emailDestinationDao()
}
