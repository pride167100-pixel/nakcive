package com.nakcive.app.ui.camera

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CameraCaptureScreen(
    onPhotoCaptured: (Uri) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    var isProcessing by remember { mutableStateOf(false) }
    var frozenPhotoUri by remember { mutableStateOf<Uri?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture,
                        )
                    } catch (_: Exception) {
                        Toast.makeText(ctx, "카메라를 열 수 없습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize(),
        )

        // 촬영 직후에는 실시간 화면 대신 방금 찍은 사진을 고정해서 보여줌 (셔터가 눌린 느낌)
        frozenPhotoUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = "방금 촬영한 사진",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isProcessing) {
                Text("위치·날씨 확인 중... 잠시만 기다려주세요", color = Color.White)
            }
            Button(
                enabled = !isProcessing,
                onClick = {
                    isProcessing = true
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(
                        context.contentResolver,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        createImageContentValues(),
                    ).build()
                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                val savedUri = output.savedUri
                                if (savedUri != null) {
                                    frozenPhotoUri = savedUri
                                    onPhotoCaptured(savedUri)
                                } else {
                                    isProcessing = false
                                    Toast.makeText(
                                        context,
                                        "사진 저장에 실패했습니다. 다시 시도해주세요.",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                isProcessing = false
                                Toast.makeText(
                                    context,
                                    "사진 저장에 실패했습니다. 다시 시도해주세요.",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                    )
                },
            ) {
                Text("촬영")
            }
            TextButton(onClick = onCancel, enabled = !isProcessing) {
                Text("취소")
            }
        }
    }
}

private fun createImageContentValues(): ContentValues {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    return ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "낚카이브_$timestamp")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/낚카이브")
        }
    }
}
