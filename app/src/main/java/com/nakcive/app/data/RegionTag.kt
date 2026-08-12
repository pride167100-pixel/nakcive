package com.nakcive.app.data

val REGION_TAGS = listOf("서해", "남해", "동해", "제주")
const val DEFAULT_REGION_TAG = "남해"

fun regionTagFromLocation(latitude: Double, longitude: Double): String {
    if (latitude == 0.0 && longitude == 0.0) return DEFAULT_REGION_TAG
    return when {
        latitude < 34.0 -> "제주"
        longitude >= 129.0 -> "동해"
        longitude <= 126.5 -> "서해"
        else -> "남해"
    }
}
