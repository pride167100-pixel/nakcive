package com.nakcive.app.data

import android.content.Context
import com.nakcive.app.data.api.KakaoLocalApi
import com.nakcive.app.data.entity.GeocodedRestroom
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private const val CONCURRENCY = 20

/**
 * PublicRestroomRepository의 주소 목록(좌표 없음)을 카카오 주소 검색으로 한 번 좌표로 바꿔
 * geocoded_restrooms 테이블에 캐시해둔다. 이미 채워져 있으면 아무것도 하지 않는다(최초 1회성 작업).
 */
object RestroomGeocodeSync {
    suspend fun ensureReady(
        context: Context,
        database: NakciveDatabase,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ) {
        val dao = database.geocodedRestroomDao()
        if (dao.count() > 0) return

        val entries = PublicRestroomRepository.loadAll(context)
        val semaphore = Semaphore(CONCURRENCY)
        val completed = AtomicInteger(0)

        val geocoded = coroutineScope {
            entries.map { entry ->
                async {
                    val coords = semaphore.withPermit { KakaoLocalApi.geocodeAddress(entry.address) }
                    onProgress(completed.incrementAndGet(), entries.size)
                    coords?.let { (lat, lng) ->
                        GeocodedRestroom(name = entry.name, address = entry.address, latitude = lat, longitude = lng)
                    }
                }
            }.awaitAll().filterNotNull()
        }

        dao.insertAll(geocoded)
    }
}
