package com.nakcive.app.data.api

import java.io.StringReader
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

// 공공데이터포털 발급키. 개인용 프로젝트라 코드에 직접 둠 —
// 나중에 저장소를 공개할 계획이 생기면 키를 새로 발급받아 교체할 것.
private const val SERVICE_KEY =
    "rymmiIi8a7nx2RPFaZFB6BAC3IwiuVklOpdAjFQxYhEVCbl2P8PH2XcXRUjOii0ciqNBW/iQmbZ5OLWmtuW96g=="

data class BuoyObservation(
    val stationName: String,
    val observedAt: String,
    val windDirectionDeg: Double?,
    val windSpeedMs: Double?,
    val waveHeightM: Double?,
    val waterTempC: Double?,
    val airTempC: Double?,
)

object MarineDataApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun fetchLatestBuoyObservation(obsCode: String): BuoyObservation? =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://apis.data.go.kr/1192136/twRecent/GetTWRecentApiService" +
                    "?serviceKey=${URLEncoder.encode(SERVICE_KEY, "UTF-8")}" +
                    "&obsCode=$obsCode"
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: return@withContext null
                    parseFirstItem(body)
                }
            } catch (_: Exception) {
                // 신호가 안 잡히거나 서버 문제 등: 위치/GPS 값은 이미 저장되었으니
                // 날씨 정보만 비워두고 진행
                null
            }
        }

    private fun parseFirstItem(xml: String): BuoyObservation? {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(xml))

        var stationName = ""
        var observedAt = ""
        var windDir: Double? = null
        var windSpeed: Double? = null
        var waveHeight: Double? = null
        var waterTemp: Double? = null
        var airTemp: Double? = null

        var currentTag = ""
        var inItem = false
        var itemClosed = false

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT && !itemClosed) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag == "item") inItem = true
                }

                XmlPullParser.TEXT -> {
                    if (inItem) {
                        val text = parser.text
                        when (currentTag) {
                            "obsvtrNm" -> stationName = text
                            "obsrvnDt" -> observedAt = text
                            "wndrct" -> windDir = text.toDoubleOrNull()
                            "wspd" -> windSpeed = text.toDoubleOrNull()
                            "wvhgt" -> waveHeight = text.toDoubleOrNull()
                            "wtem" -> waterTemp = text.toDoubleOrNull()
                            "artmp" -> airTemp = text.toDoubleOrNull()
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "item" && inItem) itemClosed = true
                }
            }
            if (!itemClosed) eventType = parser.next()
        }

        return if (stationName.isNotBlank()) {
            BuoyObservation(stationName, observedAt, windDir, windSpeed, waveHeight, waterTemp, airTemp)
        } else {
            null
        }
    }
}

private val COMPASS_16 = listOf(
    "북", "북북동", "북동", "동북동", "동", "동남동", "남동", "남남동",
    "남", "남남서", "남서", "서남서", "서", "서북서", "북서", "북북서",
)

fun degreesToCompass(degrees: Double): String {
    val index = (((degrees % 360) / 22.5) + 0.5).toInt() % 16
    return COMPASS_16[index]
}
