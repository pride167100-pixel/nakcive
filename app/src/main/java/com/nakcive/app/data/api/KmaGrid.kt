package com.nakcive.app.data.api

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

/** 기상청 단기예보 API가 쓰는 격자(Lambert Conformal Conic) 좌표 변환. */
object KmaGrid {
    private const val RE = 6371.00877
    private const val GRID = 5.0
    private const val SLAT1 = 30.0
    private const val SLAT2 = 60.0
    private const val OLON = 126.0
    private const val OLAT = 38.0
    private const val XO = 43.0
    private const val YO = 136.0
    private const val DEGRAD = PI / 180.0

    fun latLonToGrid(latitude: Double, longitude: Double): Pair<Int, Int> {
        val re = RE / GRID
        val slat1 = SLAT1 * DEGRAD
        val slat2 = SLAT2 * DEGRAD
        val olon = OLON * DEGRAD
        val olat = OLAT * DEGRAD

        var sn = tan(PI * 0.25 + slat2 * 0.5) / tan(PI * 0.25 + slat1 * 0.5)
        sn = ln(cos(slat1) / cos(slat2)) / ln(sn)
        var sf = tan(PI * 0.25 + slat1 * 0.5)
        sf = sf.pow(sn) * cos(slat1) / sn
        var ro = tan(PI * 0.25 + olat * 0.5)
        ro = re * sf / ro.pow(sn)

        var ra = tan(PI * 0.25 + latitude * DEGRAD * 0.5)
        ra = re * sf / ra.pow(sn)
        var theta = longitude * DEGRAD - olon
        if (theta > PI) theta -= 2.0 * PI
        if (theta < -PI) theta += 2.0 * PI
        theta *= sn

        val x = floor(ra * sin(theta) + XO + 0.5)
        val y = floor(ro - ra * cos(theta) + YO + 0.5)
        return x.toInt() to y.toInt()
    }
}
