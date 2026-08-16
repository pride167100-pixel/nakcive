package com.nakcive.app.data

import com.nakcive.app.data.dao.SpeciesDao
import com.nakcive.app.data.entity.Species

/**
 * SPECIES_SEED_DATA를 species 테이블에 반영한다. 이미 있는 어종(이름 기준)은
 * 특징 정보를 최신 내용으로 갱신하고, 없는 어종은 새로 추가한다.
 */
object SpeciesSeeder {
    suspend fun ensureSeeded(speciesDao: SpeciesDao) {
        SPECIES_SEED_DATA.forEach { entry ->
            val existing = speciesDao.getByCommonName(entry.commonName)
            if (existing == null) {
                speciesDao.insert(
                    Species(
                        commonName = entry.commonName,
                        scientificName = entry.scientificName,
                        family = null,
                        order = null,
                        description = entry.description,
                        ecology = entry.ecology,
                        habitat = entry.habitat,
                        regionDistribution = null,
                        minLegalSize = entry.minLegalSize,
                        closedSeasonStart = entry.closedSeasonStart,
                        closedSeasonEnd = entry.closedSeasonEnd,
                        imagePath = null,
                    ),
                )
            } else {
                speciesDao.update(
                    existing.copy(
                        scientificName = entry.scientificName,
                        description = entry.description,
                        ecology = entry.ecology,
                        habitat = entry.habitat,
                        minLegalSize = entry.minLegalSize,
                        closedSeasonStart = entry.closedSeasonStart,
                        closedSeasonEnd = entry.closedSeasonEnd,
                    ),
                )
            }
        }
    }
}
