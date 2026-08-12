package com.nakcive.app.ui.camera

import android.content.Context
import android.location.Geocoder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION")
suspend fun reverseGeocode(context: Context, latitude: Double, longitude: Double): String? {
    return withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.KOREA)
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            addresses?.firstOrNull()?.let { address ->
                listOfNotNull(
                    address.adminArea,
                    address.subAdminArea,
                    address.locality,
                    address.subLocality,
                    address.thoroughfare,
                ).distinct().joinToString(" ").ifBlank { null }
            }
        } catch (_: Exception) {
            // 인터넷이 안 되는 곳(먼바다 등)에서는 주소 변환이 실패할 수 있음.
            // 이 경우 위도/경도는 이미 저장되어 있으니 위치 자체를 잃지는 않음.
            null
        }
    }
}
