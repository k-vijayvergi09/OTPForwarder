package com.samsung.android.otpforwarder.core.database

import com.samsung.android.otpforwarder.core.database.dao.SmsDestinationDao
import com.samsung.android.otpforwarder.core.database.entity.SmsDestinationEntity
import com.samsung.android.otpforwarder.core.database.entity.toDomain
import com.samsung.android.otpforwarder.core.database.entity.toEntity
import com.samsung.android.otpforwarder.core.domain.SmsDestinationRepository
import com.samsung.android.otpforwarder.core.model.SmsDestination
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SmsDestinationRepository] backed by Room (parallel to [RoomForwardingRepository]).
 */
@Singleton
class RoomSmsDestinationRepository @Inject constructor(
    private val dao: SmsDestinationDao,
) : SmsDestinationRepository {

    override fun observeAll(): Flow<List<SmsDestination>> =
        dao.observeAll().map { it.map(SmsDestinationEntity::toDomain) }

    override fun observeEnabled(): Flow<List<SmsDestination>> =
        dao.observeEnabled().map { it.map(SmsDestinationEntity::toDomain) }

    override suspend fun enabledOnce(): List<SmsDestination> =
        dao.getEnabledOnce().map(SmsDestinationEntity::toDomain)

    override suspend fun upsert(destination: SmsDestination) {
        dao.upsert(destination.toEntity())
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    override suspend fun setEnabled(id: String, enabled: Boolean) {
        dao.setEnabled(id, enabled)
    }
}
