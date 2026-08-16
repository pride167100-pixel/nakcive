package com.nakcive.app.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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

private const val DEFAULT_LAT = 36.5
private const val DEFAULT_LNG = 127.8
private const val LABEL_ID_PREFIX = "record_"

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
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { mutableStateOf<MapView?>(null) }
    val kakaoMapState = remember { mutableStateOf<KakaoMap?>(null) }
    val hasCenteredCamera = remember { mutableStateOf(false) }

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

    LaunchedEffect(records, kakaoMapState.value) {
        val kakaoMap = kakaoMapState.value ?: return@LaunchedEffect
        drawRecordLabels(kakaoMap, records)
        val target = records.firstOrNull()
        if (!hasCenteredCamera.value && target != null) {
            hasCenteredCamera.value = true
            kakaoMap.moveCamera(
                CameraUpdateFactory.newCenterPosition(LatLng.from(target.latitude, target.longitude), 12),
            )
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

private fun drawRecordLabels(kakaoMap: KakaoMap, records: List<FishingRecord>) {
    val labelManager = kakaoMap.labelManager ?: return
    val layer = labelManager.layer ?: return
    layer.removeAll()
    if (records.isEmpty()) return
    val styles = labelManager.addLabelStyles(LabelStyles.from(LabelStyle.from(R.drawable.ic_map_marker)))
    records.forEach { record ->
        val options = LabelOptions.from(
            "$LABEL_ID_PREFIX${record.id}",
            LatLng.from(record.latitude, record.longitude),
        ).setStyles(styles)
        layer.addLabel(options)
    }
}
