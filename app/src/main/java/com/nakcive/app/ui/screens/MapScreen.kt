package com.nakcive.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.nakcive.app.R
import com.nakcive.app.data.entity.FishingRecord
import com.nakcive.app.ui.camera.getCurrentLocation

private const val DEFAULT_LAT = 36.5
private const val DEFAULT_LNG = 127.8
private const val LABEL_ID_PREFIX = "record_"
private const val CURRENT_LOCATION_LABEL_ID = "current_location"

@Composable
fun MapScreen(
    onBack: () -> Unit,
    onRecordClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = viewModel(),
) {
    val records by viewModel.records.collectAsState()
    var mapError by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        KakaoMapView(
            records = records,
            onRecordClick = onRecordClick,
            onMapError = { mapError = it },
            modifier = Modifier.fillMaxSize(),
        )

        when {
            mapError != null -> {
                Text(
                    text = "지도를 불러오지 못했습니다: $mapError",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                )
            }
            records.isEmpty() -> {
                Text(
                    text = "지도에 표시할 기록이 아직 없습니다",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                )
            }
        }

        Button(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        ) {
            Text("뒤로가기")
        }
    }
}

@Composable
private fun KakaoMapView(
    records: List<FishingRecord>,
    onRecordClick: (Long) -> Unit,
    onMapError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { mutableStateOf<MapView?>(null) }
    val kakaoMapState = remember { mutableStateOf<KakaoMap?>(null) }
    val hasCenteredCamera = remember { mutableStateOf(false) }
    val currentLocation = remember { mutableStateOf<LatLng?>(null) }
    // 벡터(XML) 드로어블은 카카오맵 라벨 렌더러가 못 읽는 경우가 있어서,
    // 미리 실제 비트맵 이미지로 직접 그려서 넘긴다.
    val markerBitmap = remember { createMarkerBitmap(context) }
    val currentLocationBitmap = remember { createCurrentLocationBitmap() }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            val location = getCurrentLocation(context)
            if (location != null) {
                currentLocation.value = LatLng.from(location.latitude, location.longitude)
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val view = MapView(context)
            mapView.value = view
            view.start(
                object : MapLifeCycleCallback() {
                    override fun onMapDestroy() {}
                    override fun onMapError(exception: Exception) {
                        onMapError(exception.message ?: exception.toString())
                    }
                },
                object : KakaoMapReadyCallback() {
                    override fun onMapReady(kakaoMap: KakaoMap) {
                        kakaoMap.setOnLabelClickListener { _, _, label ->
                            label.labelId
                                .removePrefix(LABEL_ID_PREFIX)
                                .toLongOrNull()
                                ?.let(onRecordClick)
                            true
                        }
                        // 지도가 준비된 시점엔 기록 목록이 아직 DB에서 다 안 불러와졌을 수 있어서,
                        // 실제로 마커를 찍고 카메라를 옮기는 건 아래 LaunchedEffect(records)에서 처리한다.
                        kakaoMapState.value = kakaoMap
                    }

                    override fun getPosition(): LatLng = LatLng.from(DEFAULT_LAT, DEFAULT_LNG)

                    override fun getZoomLevel(): Int = 7
                },
            )
            view
        },
    )

    LaunchedEffect(records, kakaoMapState.value, currentLocation.value) {
        val kakaoMap = kakaoMapState.value ?: return@LaunchedEffect
        try {
            val failureReason = drawRecordLabels(
                kakaoMap,
                records,
                markerBitmap,
                currentLocation.value,
                currentLocationBitmap,
            )
            if (failureReason != null) {
                onMapError("마커 표시 실패 ($failureReason)")
                return@LaunchedEffect
            }
            val target = records.firstOrNull()
            if (!hasCenteredCamera.value && target != null) {
                hasCenteredCamera.value = true
                kakaoMap.moveCamera(
                    CameraUpdateFactory.newCenterPosition(LatLng.from(target.latitude, target.longitude), 12),
                )
            }
        } catch (e: Exception) {
            onMapError("마커 표시 중 오류: ${e.message ?: e.toString()}")
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.value?.resume()
                Lifecycle.Event.ON_PAUSE -> mapView.value?.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

/** 성공하면 null, 실패하면 원인 문구를 반환한다 (화면에 그대로 보여주기 위함). */
private fun drawRecordLabels(
    kakaoMap: KakaoMap,
    records: List<FishingRecord>,
    markerBitmap: Bitmap,
    currentLocation: LatLng?,
    currentLocationBitmap: Bitmap,
): String? {
    val labelManager = kakaoMap.labelManager ?: return "labelManager가 null"
    val layer = labelManager.layer ?: return "labelManager.layer가 null"
    layer.removeAll()

    if (currentLocation != null) {
        val currentLocationStyles = labelManager.addLabelStyles(
            LabelStyles.from(LabelStyle.from(currentLocationBitmap)),
        ) ?: return "addLabelStyles(내 위치)가 null"
        layer.addLabel(
            LabelOptions.from(CURRENT_LOCATION_LABEL_ID, currentLocation).setStyles(currentLocationStyles),
        )
    }

    if (records.isEmpty()) return null
    val styles = labelManager.addLabelStyles(LabelStyles.from(LabelStyle.from(markerBitmap)))
        ?: return "addLabelStyles가 null"
    records.forEach { record ->
        val options = LabelOptions.from(
            "$LABEL_ID_PREFIX${record.id}",
            LatLng.from(record.latitude, record.longitude),
        ).setStyles(styles)
        layer.addLabel(options)
    }
    return null
}

private fun createMarkerBitmap(context: Context): Bitmap {
    val drawable = ContextCompat.getDrawable(context, R.drawable.ic_map_marker)!!
    val width = drawable.intrinsicWidth.coerceAtLeast(1)
    val height = drawable.intrinsicHeight.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

/** 파란 점 + 흰 테두리로 된 "내 위치" 마커를 코드로 직접 그린다 (리소스 파일 불필요). */
private fun createCurrentLocationBitmap(): Bitmap {
    val sizePx = 48
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = sizePx / 2f
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4285F4") }
    canvas.drawCircle(center, center, center, borderPaint)
    canvas.drawCircle(center, center, center - 5f, dotPaint)
    return bitmap
}
