package com.nakcive.app.data.api

import java.io.StringReader
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class KmaForecast(
    val windDirectionDeg: Double?,
    val windSpeedMs: Double?,
    val waveHeightM: Double?,
    val airTempC: Double?,
)

/** 기상청 단기예보(getVilageFcst) — 격자 기반이라 대한민국 어디든 값이 존재함. */
object KmaForecastApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val forecastCategories = setOf("WSD", "VEC", "WAV", "TMP")

    suspend fun fetchNearestForecast(nx: Int, ny: Int): KmaForecast? = withContext(Dispatchers.IO) {
        try {
            val (baseDate, baseTime) = latestBaseDateTime()
            val url = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst" +
                "?serviceKey=${URLEncoder.encode(SERVICE_KEY, "UTF-8")}" +
                "&pageNo=1&numOfRows=1000&dataType=XML" +
                "&base_date=$baseDate&base_time=$baseTime" +
                "&nx=$nx&ny=$ny"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@withContext null
                parseForecast(body)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseForecast(xml: String): KmaForecast? {
        data class Entry(val fcstDateTime: String, val value: String)

        val byCategory = mutableMapOf<String, Entry>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(xml))

        var currentTag = ""
        var inItem = false
        var category = ""
        var fcstDate = ""
        var fcstTime = ""
        var fcstValue = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag == "item") {
                        inItem = true
                        category = ""
                        fcstDate = ""
                        fcstTime = ""
                        fcstValue = ""
                    }
                }

                XmlPullParser.TEXT -> {
                    val text = parser.text
                    if (inItem && text.isNotBlank()) {
                        when (currentTag) {
                            "category" -> category = text
                            "fcstDate" -> fcstDate = text
                            "fcstTime" -> fcstTime = text
                            "fcstValue" -> fcstValue = text
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "item" && inItem) {
                        inItem = false
                        if (category in forecastCategories) {
                            val dateTime = fcstDate + fcstTime
                            val existing = byCategory[category]
                            if (existing == null || dateTime < existing.fcstDateTime) {
                                byCategory[category] = Entry(dateTime, fcstValue)
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        if (byCategory.isEmpty()) return null

        return KmaForecast(
            windDirectionDeg = byCategory["VEC"]?.value?.toDoubleOrNull(),
            windSpeedMs = byCategory["WSD"]?.value?.toDoubleOrNull(),
            waveHeightM = byCategory["WAV"]?.value?.toDoubleOrNull(),
            airTempC = byCategory["TMP"]?.value?.toDoubleOrNull(),
        )
    }

    /** 기상청 단기예보는 02,05,08,11,14,17,20,23시에 발표되고, 발표 후 약 10분 뒤부터 조회 가능. */
    private fun latestBaseDateTime(): Pair<String, String> {
        val slots = listOf(2, 5, 8, 11, 14, 17, 20, 23)
        val publishBufferMinutes = 10
        val seoulTimeZone = TimeZone.getTimeZone("Asia/Seoul")
        val calendar = Calendar.getInstance(seoulTimeZone)

        val currentTotalMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val availableSlot = slots.lastOrNull { slot -> currentTotalMinutes >= slot * 60 + publishBufferMinutes }

        val chosenSlot: Int
        if (availableSlot != null) {
            chosenSlot = availableSlot
        } else {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            chosenSlot = slots.last()
        }

        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.KOREA).apply { timeZone = seoulTimeZone }
        val baseDate = dateFormat.format(calendar.time)
        val baseTime = "%02d00".format(chosenSlot)
        return baseDate to baseTime
    }
}
