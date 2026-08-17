package com.nakcive.app.data

import android.content.Context

data class PublicRestroomEntry(val name: String, val address: String)

/**
 * 공공데이터포털 전국공중화장실표준데이터 중 남해안 인근 지역(부산·경남·전남 해안 시군)만 추린 목록.
 * app/src/main/assets/public_restrooms.csv 에서 불러온다. 좌표가 없어서 이름/주소만 담고 있고,
 * 실제 좌표는 RestroomGeocodeSync가 최초 1회 카카오 주소 검색으로 변환해 캐시해둔다.
 */
object PublicRestroomRepository {
    @Volatile
    private var cached: List<PublicRestroomEntry>? = null

    fun loadAll(context: Context): List<PublicRestroomEntry> {
        cached?.let { return it }
        val entries = context.assets.open("public_restrooms.csv")
            .bufferedReader(Charsets.UTF_8)
            .useLines { lines ->
                lines.drop(1).mapNotNull { line ->
                    val fields = parseCsvLine(line)
                    if (fields.size < 2) return@mapNotNull null
                    PublicRestroomEntry(name = fields[0], address = fields[1])
                }.toList()
            }
        cached = entries
        return entries
    }

    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes -> {
                    if (c == '"') {
                        if (i + 1 < line.length && line[i + 1] == '"') {
                            current.append('"')
                            i++
                        } else {
                            inQuotes = false
                        }
                    } else {
                        current.append(c)
                    }
                }
                c == '"' -> inQuotes = true
                c == ',' -> {
                    fields.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        fields.add(current.toString())
        return fields
    }
}
