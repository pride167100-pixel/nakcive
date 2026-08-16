package com.nakcive.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
                        drawRecordLabels(kakaoMap, records)
                        kakaoMap.setOnLabelClickListener { _, _, label ->
                            label.labelId
                                .removePrefix(LABEL_ID_PREFIX)
                                .toLongOrNull()
                                ?.let(onRecordClick)
                            true
                        }
                    }

                    override fun getPosition(): LatLng {
                        val target = records.firstOrNull()
                        return if (target != null) {
                            LatLng.from(target.latitude, target.longitude)
                        } else {
                            LatLng.from(DEFAULT_LAT, DEFAULT_LNG)
                        }
                    }

                    override fun getZoomLevel(): Int = if (records.isEmpty()) 7 else 12
                },
            )
            view
        },
    )

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
    val styles = labelManager.addLabelStyles(LabelStyles.from(LabelStyle.from(R.drawable.ic_map_marker)))
    val layer = labelManager.layer ?: return
    records.forEach { record ->
        val options = LabelOptions.from(
            "$LABEL_ID_PREFIX${record.id}",
            LatLng.from(record.latitude, record.longitude),
        ).setStyles(styles)
        layer.addLabel(options)
    }
}
