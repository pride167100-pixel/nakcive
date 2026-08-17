package com.nakcive.app.data.api

import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

// 카카오 개발자 콘솔의 REST API 키. 네이티브 앱 키(지도용)와는 다른 값이다.
// 개인용 프로젝트라 코드에 직접 둠 — 저장소를 공개할 계획이 생기면 새로 발급받아 교체할 것.
internal const val KAKAO_REST_API_KEY = "d2770b4ec79dc2bf294374b545de63bf"

data class NearbyRestroom(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val distanceM: Int?,
)

/** 카카오 로컬 키워드 검색으로 주변 공중화장실을 찾는다. */
object KakaoLocalApi {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun fetchNearbyRestrooms(
        latitude: Double,
        longitude: Double,
        limit: Int = 5,
        radiusM: Int = 5000,
    ): List<NearbyRestroom> = withContext(Dispatchers.IO) {
        try {
            val url = "https://dapi.kakao.com/v2/local/search/keyword.json" +
                "?query=${URLEncoder.encode("공중화장실", "UTF-8")}" +
                "&x=$longitude&y=$latitude&radius=$radiusM&sort=distance&size=$limit"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "KakaoAK $KAKAO_REST_API_KEY")
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@withContext emptyList()
                if (!response.isSuccessful) return@withContext emptyList()
                parseResults(body)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseResults(json: String): List<NearbyRestroom> {
        val documents = JSONObject(json).optJSONArray("documents") ?: return emptyList()
        return (0 until documents.length()).mapNotNull { i ->
            val doc = documents.optJSONObject(i) ?: return@mapNotNull null
            val lat = doc.optString("y").toDoubleOrNull() ?: return@mapNotNull null
            val lng = doc.optString("x").toDoubleOrNull() ?: return@mapNotNull null
            val id = doc.optString("id")
            if (id.isBlank()) return@mapNotNull null
            NearbyRestroom(
                id = id,
                name = doc.optString("place_name"),
                latitude = lat,
                longitude = lng,
                distanceM = doc.optString("distance").toIntOrNull(),
            )
        }
    }
}
