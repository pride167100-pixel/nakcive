package com.nakcive.app.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
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
                val sourceBitmap = context.contentResolver
                    .openInputStream(Uri.parse(record.photoPath))
                    ?.use { BitmapFactory.decodeStream(it) }
                    ?: return@withContext null

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

        record.address?.let {
            textY += 54f
            canvas.drawText(it, margin, textY, bodyPaint)
        }

        textY += 54f
        val dateText = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date(record.recordedAt))
        val tideText = record.tidePhase?.let { " · 물때 $it" } ?: ""
        canvas.drawText(dateText + tideText, margin, textY, bodyPaint)

        canvas.drawText("낚카이브", CARD_WIDTH - margin, CARD_HEIGHT - 50f, watermarkPaint)
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
