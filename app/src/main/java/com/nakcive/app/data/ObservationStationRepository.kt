package com.nakcive.app.data

import android.content.Context
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 조위관측소(DT_)/해양관측부이(TW_)/종합해양과학기지(IE_) 등의 코드·좌표 목록.
 * app/src/main/assets/observation_stations.csv 에서 불러온다.
 */
object ObservationStationRepository {
    @Volatile
    private var cached: List<ObservationStation>? = null

    fun loadAll(context: Context): List<ObservationStation> {
        cached?.let { return it }
        val stations = context.assets.open("observation_stations.csv")
            .bufferedReader(Charsets.UTF_8)
            .useLines { lines ->
                lines.drop(1).mapNotNull { line ->
                    val parts = line.split(",")
                    if (parts.size < 6) return@mapNotNull null
                    ObservationStation(
                        code = parts[0].trim(),
                        type = parts[1].trim(),
                        name = parts[2].trim(),
                        latitude = parts[3].trim().toDoubleOrNull() ?: return@mapNotNull null,
                        longitude = parts[4].trim().toDoubleOrNull() ?: return@mapNotNull null,
                        englishName = parts[5].trim(),
                    )
                }.toList()
            }
        cached = stations
        return stations
    }

    /** codePrefix 예: "DT_"(조위관측소), "TW_"(해양관측부이) */
    fun findNearest(
        context: Context,
        latitude: Double,
        longitude: Double,
        codePrefix: String,
    ): ObservationStation? {
        return loadAll(context)
            .filter { it.code.startsWith(codePrefix) }
            .minByOrNull { distanceKm(latitude, longitude, it.latitude, it.longitude) }
    }

    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
}
