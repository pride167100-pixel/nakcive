package com.nakcive.app.data

import com.nakcive.app.data.entity.UserSpeciesRecord
import kotlinx.coroutines.flow.first

/**
 * 어종별 최고기록(UserSpeciesRecord)을 실제 남아있는 FishingRecord 기준으로
 * 다시 계산한다. 기록이 삭제되어 남은 게 없으면 도감 항목 자체를 지운다.
 */
object SpeciesRecordSync {
    suspend fun recalculate(database: NakciveDatabase, speciesId: Long) {
        val fishingRecordDao = database.fishingRecordDao()
        val userSpeciesRecordDao = database.userSpeciesRecordDao()

        val remaining = fishingRecordDao.getBySpecies(speciesId).first()
        if (remaining.isEmpty()) {
            userSpeciesRecordDao.getBySpeciesId(speciesId)?.let { userSpeciesRecordDao.delete(it) }
            return
        }

        val best = remaining.maxByOrNull { it.sizeCm ?: Double.NEGATIVE_INFINITY }
        userSpeciesRecordDao.upsert(
            UserSpeciesRecord(
                speciesId = speciesId,
                maxSizeCm = best?.sizeCm,
                maxWeightKg = best?.weightKg,
                maxRecordId = best?.id,
                firstCaughtAt = remaining.minOf { it.recordedAt },
                catchCount = remaining.size,
            )
        )
    }
}
