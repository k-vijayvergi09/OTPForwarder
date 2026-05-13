package com.samsung.android.otpforwarder.core.database

import com.samsung.android.otpforwarder.core.database.dao.EmailDestinationDao
import com.samsung.android.otpforwarder.core.database.entity.EmailDestinationEntity
import com.samsung.android.otpforwarder.core.database.entity.toDomain
import com.samsung.android.otpforwarder.core.database.entity.toEntity
import com.samsung.android.otpforwarder.core.domain.EmailDestinationRepository
import com.samsung.android.otpforwarder.core.model.EmailDestination
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [EmailDestinationRepository] backed by Room.
 */
@Singleton
class RoomEmailDestinationRepository @Inject constructor(
    private val dao: EmailDestinationDao,
) : EmailDestinationRepository {

    override fun observeAll(): Flow<List<EmailDestination>> =
        dao.observeAll().map { it.map(EmailDestinationEntity::toDomain) }

    override fun observeEnabled(): Flow<List<EmailDestination>> =
        dao.observeEnabled().map { it.map(EmailDestinationEntity::toDomain) }

    override suspend fun enabledOnce(): List<EmailDestination> =
        dao.getEnabledOnce().map(EmailDestinationEntity::toDomain)

    override suspend fun upsert(destination: EmailDestination) {
        dao.upsert(destination.toEntity())
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    override suspend fun setEnabled(id: String, enabled: Boolean) {
        dao.setEnabled(id, enabled)
    }
}
