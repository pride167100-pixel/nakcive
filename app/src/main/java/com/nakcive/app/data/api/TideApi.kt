package com.nakcive.app.data.api

import java.io.StringReader
import java.net.URLEncoder
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class TideInfo(
    val phase: String?,
    val levelCm: Double?,
    val stationName: String?,
)

private data class TidePoint(val minuteOfDay: Int, val heightCm: Double)

/**
 * 조위관측소(DT_)의 1분 단위 조위 예측 시계열(tideFcstTime)로 현재 물때 단계
 * (들물/중들물/만조/날물/중날물/간조)를 계산한다.
 *
 * 계산 방식: 하루치 조위 곡선에서 극댓값(만조)·극솟값(간조)을 찾고, 그 사이 구간을
 * 3등분해서 단계를 나눔 — 공식 기준이 아닌 단순화된 근사치.
 */
object TideApi {
    // 공공데이터포털 tideFcstTime API는 한 번에 최대 200개 행까지만 허용한다
    // (numOfRows=500/999/1440은 INVALID_REQUEST_PARAMETER_ERROR로 거부됨, 실측 확인).
    // 하루치(1440분) 전체를 얻으려면 200개씩 여러 페이지로 나눠 요청해서 이어붙여야 한다.
    private const val PAGE_SIZE = 200
    private const val MAX_PAGES = 8 // 200 * 8 = 1600 ≥ 1440분(하루)

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun fetchTideInfo(obsCode: String): TideInfo? = withContext(Dispatchers.IO) {
        try {
            val today = run {
                val now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
                val year = now.get(Calendar.YEAR)
                val month = now.get(Calendar.MONTH) + 1
                val day = now.get(Calendar.DAY_OF_MONTH)
                "%04d%02d%02d".format(year, month, day)
            }

            val allPoints = mutableListOf<TidePoint>()
            var stationName: String? = null
            var page = 1
            while (page <= MAX_PAGES) {
                val url = "https://apis.data.go.kr/1192136/tideFcstTime/GetTideFcstTimeApiService" +
                    "?serviceKey=${URLEncoder.encode(SERVICE_KEY, "UTF-8")}" +
                    "&obsCode=$obsCode&Date=$today&numOfRows=$PAGE_SIZE&pageNo=$page"
                val request = Request.Builder().url(url).build()
                val body = client.newCall(request).execute().use { it.body?.string() } ?: break
                val (points, name) = parseTidePoints(body)
                if (points.isEmpty()) break
                allPoints.addAll(points)
                if (stationName == null) stationName = name
                if (points.size < PAGE_SIZE) break // 마지막 페이지
                page++
            }
            if (allPoints.isEmpty()) return@withContext null

            val nowMinute = run {
                val now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
                now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            }
            val nowIndex = allPoints.indexOfFirst { it.minuteOfDay >= nowMinute }
                .let { if (it == -1) allPoints.lastIndex else it }

            TideInfo(
                phase = computeTidePhase(allPoints, nowIndex),
                levelCm = allPoints[nowIndex].heightCm,
                stationName = stationName,
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun computeTidePhase(points: List<TidePoint>, nowIndex: Int): String? {
        if (points.size < 3) return null

        val extrema = mutableListOf<Pair<Int, Boolean>>() // index to isHigh
        for (i in 1 until points.size - 1) {
            val prev = points[i - 1].heightCm
            val curr = points[i].heightCm
            val next = points[i + 1].heightCm
            if (curr > prev && curr > next) extrema.add(i to true)
            if (curr < prev && curr < next) extrema.add(i to false)
        }
        if (extrema.isEmpty()) return null

        val prevExtremum = extrema.lastOrNull { it.first <= nowIndex } ?: return null
        val nextExtremum = extrema.firstOrNull { it.first > nowIndex } ?: return null
        val (prevIdx, prevIsHigh) = prevExtremum
        val (nextIdx, nextIsHigh) = nextExtremum
        if (prevIsHigh == nextIsHigh || nextIdx <= prevIdx) return null

        val fraction = (nowIndex - prevIdx).toDouble() / (nextIdx - prevIdx).toDouble()

        return if (!prevIsHigh && nextIsHigh) {
            when {
                fraction < 1.0 / 3 -> "들물"
                fraction < 2.0 / 3 -> "중들물"
                else -> "만조"
            }
        } else {
            when {
                fraction < 1.0 / 3 -> "날물"
                fraction < 2.0 / 3 -> "중날물"
                else -> "간조"
            }
        }
    }

    private fun parseTidePoints(xml: String): Pair<List<TidePoint>, String?> {
        val points = mutableListOf<TidePoint>()
        var stationName: String? = null

        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(xml))

        var currentTag = ""
        var inItem = false
        var predcDt = ""
        var tdlvHgt = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag == "item") {
                        inItem = true
                        predcDt = ""
                        tdlvHgt = ""
                    }
                }

                XmlPullParser.TEXT -> {
                    val text = parser.text
                    if (inItem && text.isNotBlank()) {
                        when (currentTag) {
                            "obsvtrNm" -> if (stationName == null) stationName = text
                            "predcDt" -> predcDt = text
                            "tdlvHgt" -> tdlvHgt = text
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "item" && inItem) {
                        inItem = false
                        val height = tdlvHgt.toDoubleOrNull()
                        val minuteOfDay = minuteOfDayFrom(predcDt)
                        if (height != null && minuteOfDay != null) {
                            points.add(TidePoint(minuteOfDay, height))
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return points to stationName
    }

    private fun minuteOfDayFrom(predcDt: String): Int? {
        val timePart = predcDt.substringAfter(" ", "")
        val parts = timePart.split(":")
        if (parts.size < 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return hour * 60 + minute
    }
}
