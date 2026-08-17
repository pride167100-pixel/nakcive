package com.nakcive.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.nakcive.app.data.api.NearbyRestroom
import com.nakcive.app.data.entity.FishingRecord
import com.nakcive.app.ui.camera.getCurrentLocation

private const val DEFAULT_LAT = 36.5
private const val DEFAULT_LNG = 127.8
private const val LABEL_ID_PREFIX = "record_"
private const val RESTROOM_LABEL_ID_PREFIX = "restroom_"
private const val CURRENT_LOCATION_LABEL_ID = "current_location"
private const val EXPLORE_POINT_LABEL_ID = "explore_point"

@Composable
fun MapScreen(
    onBack: () -> Unit,
    onRecordClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = viewModel(),
) {
    val records by viewModel.records.collectAsState()
    val restroomState by viewModel.restroomState.collectAsState()
    var mapError by remember { mutableStateOf<String?>(null) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var explorePoint by remember { mutableStateOf<LatLng?>(null) }

    LaunchedEffect(restroomState.visible) {
        if (!restroomState.visible) explorePoint = null
    }

    Box(modifier = modifier.fillMaxSize()) {
        KakaoMapView(
            records = records,
            restroomState = restroomState,
            explorePoint = explorePoint,
            onRecordClick = onRecordClick,
            onMapError = { mapError = it },
            onLocationFound = { currentLocation = it },
            onLongPress = { latLng ->
                explorePoint = latLng
                viewModel.showRestroomsNear(latLng.latitude, latLng.longitude)
            },
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

        Surface(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(44.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Surface(
            onClick = {
                viewModel.toggleRestroomsAtCurrentLocation(currentLocation?.latitude, currentLocation?.longitude)
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(44.dp),
            shape = CircleShape,
            color = if (restroomState.visible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Wc,
                    contentDescription = "화장실 보기",
                    tint = if (restroomState.visible) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (restroomState.visible) {
            RestroomList(
                state = restroomState,
                onSelect = viewModel::selectRestroom,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
            )
        }
    }
}

@Composable
private fun RestroomList(
    state: RestroomUiState,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "주변 화장실",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "다른 위치가 궁금하면 지도를 길게 눌러보세요",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            when {
                state.isLoading -> Text("찾는 중...", fontSize = 13.sp)
                state.restrooms.isEmpty() -> Text("주변에서 화장실을 찾지 못했습니다", fontSize = 13.sp)
                else -> Column {
                    state.restrooms.forEach { restroom ->
                        val selected = restroom.id == state.selectedId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(restroom.id) }
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(10.dp),
                                )
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(text = restroom.name, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text(
                                text = restroom.distanceM?.let { "${it}m" } ?: "-",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KakaoMapView(
    records: List<FishingRecord>,
    restroomState: RestroomUiState,
    explorePoint: LatLng?,
    onRecordClick: (Long) -> Unit,
    onMapError: (String) -> Unit,
    onLocationFound: (LatLng) -> Unit,
    onLongPress: (LatLng) -> Unit,
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
    val restroomBitmap = remember { createRestroomBitmap(selected = false) }
    val restroomSelectedBitmap = remember { createRestroomBitmap(selected = true) }
    val explorePointBitmap = remember { createExplorePointBitmap() }
    // 카카오맵 SDK에는 지도 길게 누르기 이벤트가 따로 없어서, 터치를 직접 감지해 처리한다.
    // GestureDetector는 항상 이벤트를 그대로 흘려보내(false 반환) 지도 자체의 확대/이동 제스처는 그대로 동작한다.
    val gestureDetector = remember {
        GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = true

                override fun onLongPress(e: MotionEvent) {
                    val kakaoMap = kakaoMapState.value ?: return
                    val position = kakaoMap.fromScreenPoint(e.x.toInt(), e.y.toInt()) ?: return
                    onLongPress(position)
                }
            },
        )
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            val location = getCurrentLocation(context)
            if (location != null) {
                val latLng = LatLng.from(location.latitude, location.longitude)
                currentLocation.value = latLng
                onLocationFound(latLng)
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val view = MapView(context)
            mapView.value = view
            view.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                false
            }
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

    LaunchedEffect(
        records,
        kakaoMapState.value,
        currentLocation.value,
        restroomState.restrooms,
        restroomState.selectedId,
        explorePoint,
    ) {
        val kakaoMap = kakaoMapState.value ?: return@LaunchedEffect
        try {
            val failureReason = drawLabels(
                kakaoMap = kakaoMap,
                records = records,
                markerBitmap = markerBitmap,
                currentLocation = currentLocation.value,
                currentLocationBitmap = currentLocationBitmap,
                restrooms = restroomState.restrooms,
                restroomBitmap = restroomBitmap,
                restroomSelectedBitmap = restroomSelectedBitmap,
                selectedRestroomId = restroomState.selectedId,
                explorePoint = explorePoint,
                explorePointBitmap = explorePointBitmap,
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

    LaunchedEffect(restroomState.selectedId) {
        val kakaoMap = kakaoMapState.value ?: return@LaunchedEffect
        val selected = restroomState.restrooms.firstOrNull { it.id == restroomState.selectedId } ?: return@LaunchedEffect
        kakaoMap.moveCamera(
            CameraUpdateFactory.newCenterPosition(LatLng.from(selected.latitude, selected.longitude), 15),
        )
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
private fun drawLabels(
    kakaoMap: KakaoMap,
    records: List<FishingRecord>,
    markerBitmap: Bitmap,
    currentLocation: LatLng?,
    currentLocationBitmap: Bitmap,
    restrooms: List<NearbyRestroom>,
    restroomBitmap: Bitmap,
    restroomSelectedBitmap: Bitmap,
    selectedRestroomId: String?,
    explorePoint: LatLng?,
    explorePointBitmap: Bitmap,
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

    if (explorePoint != null) {
        val explorePointStyles = labelManager.addLabelStyles(
            LabelStyles.from(LabelStyle.from(explorePointBitmap)),
        ) ?: return "addLabelStyles(탐색 포인트)가 null"
        layer.addLabel(
            LabelOptions.from(EXPLORE_POINT_LABEL_ID, explorePoint).setStyles(explorePointStyles),
        )
    }

    if (records.isNotEmpty()) {
        val styles = labelManager.addLabelStyles(LabelStyles.from(LabelStyle.from(markerBitmap)))
            ?: return "addLabelStyles가 null"
        records.forEach { record ->
            val options = LabelOptions.from(
                "$LABEL_ID_PREFIX${record.id}",
                LatLng.from(record.latitude, record.longitude),
            ).setStyles(styles)
            layer.addLabel(options)
        }
    }

    if (restrooms.isNotEmpty()) {
        val normalStyles = labelManager.addLabelStyles(LabelStyles.from(LabelStyle.from(restroomBitmap)))
            ?: return "addLabelStyles(화장실)가 null"
        val selectedStyles = labelManager.addLabelStyles(LabelStyles.from(LabelStyle.from(restroomSelectedBitmap)))
            ?: return "addLabelStyles(화장실 선택)가 null"
        restrooms.forEach { restroom ->
            val isSelected = restroom.id == selectedRestroomId
            val options = LabelOptions.from(
                "$RESTROOM_LABEL_ID_PREFIX${restroom.id}",
                LatLng.from(restroom.latitude, restroom.longitude),
            ).setStyles(if (isSelected) selectedStyles else normalStyles)
            layer.addLabel(options)
        }
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

/** 지도를 길게 눌러 지정한 "탐색 포인트"를 주황색 점으로 표시한다. */
private fun createExplorePointBitmap(): Bitmap {
    val sizePx = 44
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = sizePx / 2f
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF7A59") }
    canvas.drawCircle(center, center, center, borderPaint)
    canvas.drawCircle(center, center, center - 4f, dotPaint)
    return bitmap
}

/** 보라색 "W" 점으로 된 화장실 마커. 선택되면 조금 더 크게 그려서 강조한다. */
private fun createRestroomBitmap(selected: Boolean): Bitmap {
    val sizePx = if (selected) 56 else 36
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = sizePx / 2f
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor(if (selected) "#7C4DFF" else "#9575CD")
    }
    canvas.drawCircle(center, center, center, borderPaint)
    canvas.drawCircle(center, center, center - 4f, dotPaint)
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = sizePx * 0.45f
        isFakeBoldText = true
    }
    val textY = center - (textPaint.descent() + textPaint.ascent()) / 2
    canvas.drawText("W", center, textY, textPaint)
    return bitmap
}
