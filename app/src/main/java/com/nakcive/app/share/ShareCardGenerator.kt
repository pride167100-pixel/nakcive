package com.nakcive.app.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import com.nakcive.app.data.entity.FishingRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val CARD_WIDTH = 1080
private const val CARD_HEIGHT = 1350

/** 기록 사진 위에 어종·크기·위치·날짜 정보를 얹은 공유용 카드 이미지를 만든다. */
object ShareCardGenerator {

    suspend fun generate(context: Context, record: FishingRecord, speciesLabel: String): Uri? =
        withContext(Dispatchers.IO) {
            try {
                val photoUri = Uri.parse(record.photoPath)
                val decodedBitmap = context.contentResolver.openInputStream(photoUri)
                    ?.use { BitmapFactory.decodeStream(it) }
                    ?: return@withContext null
                val sourceBitmap = applyExifRotation(context, photoUri, decodedBitmap)

                val card = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(card)
                drawPhoto(canvas, sourceBitmap)
                drawGradientOverlay(canvas)
                drawInfoText(canvas, record, speciesLabel)

                val file = saveToCache(context, card, record.id)
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (_: Exception) {
                null
            }
        }

    /** CameraX가 저장한 사진은 EXIF 방향 태그로 회전 정보를 담고 있어, 픽셀을 직접
     * 다루는 BitmapFactory는 이를 무시한다. 태그를 읽어 실제 픽셀을 돌려준다. */
    private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val degrees = context.contentResolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun drawPhoto(canvas: Canvas, source: Bitmap) {
        val scale = maxOf(
            CARD_WIDTH.toFloat() / source.width,
            CARD_HEIGHT.toFloat() / source.height,
        )
        val scaledWidth = source.width * scale
        val scaledHeight = source.height * scale
        val left = (CARD_WIDTH - scaledWidth) / 2f
        val top = (CARD_HEIGHT - scaledHeight) / 2f
        val destRect = RectF(left, top, left + scaledWidth, top + scaledHeight)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, null, destRect, paint)
    }

    private fun drawGradientOverlay(canvas: Canvas) {
        val gradientTop = CARD_HEIGHT * 0.58f
        val gradientPaint = Paint().apply {
            shader = LinearGradient(
                0f, gradientTop, 0f, CARD_HEIGHT.toFloat(),
                Color.TRANSPARENT, Color.argb(210, 0, 0, 0),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, gradientTop, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), gradientPaint)
    }

    private fun drawInfoText(canvas: Canvas, record: FishingRecord, speciesLabel: String) {
        val margin = 56f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 68f
            isFakeBoldText = true
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 38f
        }
        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(200, 255, 255, 255)
            textSize = 32f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }

        var textY = CARD_HEIGHT - 220f
        canvas.drawText(speciesLabel, margin, textY, titlePaint)

        val sizeWeightLine = buildString {
            record.sizeCm?.let { append("${it}cm") }
            record.weightKg?.let {
                if (isNotEmpty()) append("  ")
                append("${it}kg")
            }
        }
        if (sizeWeightLine.isNotBlank()) {
            textY += 60f
            canvas.drawText(sizeWeightLine, margin, textY, bodyPaint)
        }

        val maxTextWidth = CARD_WIDTH - margin * 2

        record.address?.let {
            textY += 54f
            canvas.drawText(fitText(it, bodyPaint, maxTextWidth), margin, textY, bodyPaint)
        }

        textY += 54f
        val dateText = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date(record.recordedAt))
        val tideText = record.tidePhase?.let { " · 물때 $it" } ?: ""
        canvas.drawText(fitText(dateText + tideText, bodyPaint, maxTextWidth), margin, textY, bodyPaint)

        canvas.drawText("낚카이브", CARD_WIDTH - margin, CARD_HEIGHT - 50f, watermarkPaint)
    }

    /** 주어진 폭을 넘으면 뒷부분을 "..."으로 잘라 카드 밖으로 넘치지 않게 한다. */
    private fun fitText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "..."
        val ellipsisWidth = paint.measureText(ellipsis)
        val fitChars = paint.breakText(text, true, maxWidth - ellipsisWidth, null)
        return text.substring(0, fitChars) + ellipsis
    }

    private fun saveToCache(context: Context, bitmap: Bitmap, recordId: Long): File {
        val dir = File(context.cacheDir, "share_cards").apply { mkdirs() }
        val file = File(dir, "share_${recordId}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
        }
        return file
    }
}
