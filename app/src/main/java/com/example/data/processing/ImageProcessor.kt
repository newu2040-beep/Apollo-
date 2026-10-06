package com.example.data.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import com.example.data.model.ApolloImage
import com.example.data.model.CanvasFitMode
import com.example.data.model.CropAspectRatio
import com.example.data.model.CustomAspectRatio
import com.example.data.model.ExportFormat
import com.example.data.model.ExportOptions
import com.example.data.model.FilterConfig
import com.example.data.model.FilterType
import com.example.data.model.ImageAdjustment
import com.example.data.model.ResolutionPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ImageProcessor {

    suspend fun loadBitmap(context: Context, image: ApolloImage, maxDimension: Int? = null): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (image.drawableResId != null) {
                val opts = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                if (maxDimension != null) {
                    opts.inJustDecodeBounds = true
                    BitmapFactory.decodeResource(context.resources, image.drawableResId, opts)
                    val rawMax = maxOf(opts.outWidth, opts.outHeight)
                    if (rawMax > maxDimension) {
                        opts.inSampleSize = (rawMax / maxDimension).coerceAtLeast(1)
                    }
                    opts.inJustDecodeBounds = false
                }
                return@withContext BitmapFactory.decodeResource(context.resources, image.drawableResId, opts)
            }

            var inputStream: InputStream? = context.contentResolver.openInputStream(image.uri)
            val opts = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            if (maxDimension != null && inputStream != null) {
                opts.inJustDecodeBounds = true
                BitmapFactory.decodeStream(inputStream, null, opts)
                inputStream.close()
                val rawMax = maxOf(opts.outWidth, opts.outHeight)
                if (rawMax > maxDimension) {
                    opts.inSampleSize = (rawMax / maxDimension).coerceAtLeast(1)
                }
                opts.inJustDecodeBounds = false
                inputStream = context.contentResolver.openInputStream(image.uri)
            }

            val bmp = BitmapFactory.decodeStream(inputStream, null, opts)
            inputStream?.close()
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun applyAdjustmentsAndFilters(
        source: Bitmap,
        adjustment: ImageAdjustment,
        filter: FilterConfig,
        cropRatio: CropAspectRatio = CropAspectRatio.ORIGINAL,
        customRatio: CustomAspectRatio? = null
    ): Bitmap {
        var workingBitmap = source

        // 1. Transformations: Rotation and Flips
        if (adjustment.rotationDegrees != 0 || adjustment.flipHorizontal || adjustment.flipVertical) {
            val matrix = Matrix()
            if (adjustment.rotationDegrees != 0) {
                matrix.postRotate(adjustment.rotationDegrees.toFloat())
            }
            val scaleX = if (adjustment.flipHorizontal) -1f else 1f
            val scaleY = if (adjustment.flipVertical) -1f else 1f
            if (scaleX != 1f || scaleY != 1f) {
                matrix.postScale(scaleX, scaleY)
            }
            workingBitmap = Bitmap.createBitmap(
                workingBitmap,
                0,
                0,
                workingBitmap.width,
                workingBitmap.height,
                matrix,
                true
            )
        }

        // 2. Crop to aspect ratio if fixed
        val effectiveRatio: Pair<Float, Float>? = when {
            customRatio != null && customRatio.isFixed -> Pair(customRatio.ratioWidth, customRatio.ratioHeight)
            cropRatio.isFixed -> Pair(cropRatio.ratioWidth, cropRatio.ratioHeight)
            else -> null
        }

        if (effectiveRatio != null) {
            val (rw, rh) = effectiveRatio
            val targetRatio = rw / rh
            val currentRatio = workingBitmap.width.toFloat() / workingBitmap.height.toFloat()

            val cropW: Int
            val cropH: Int
            if (currentRatio > targetRatio) {
                cropH = workingBitmap.height
                cropW = (cropH * targetRatio).toInt().coerceAtMost(workingBitmap.width)
            } else {
                cropW = workingBitmap.width
                cropH = (cropW / targetRatio).toInt().coerceAtMost(workingBitmap.height)
            }

            val startX = ((workingBitmap.width - cropW) / 2).coerceAtLeast(0)
            val startY = ((workingBitmap.height - cropH) / 2).coerceAtLeast(0)

            if (cropW > 0 && cropH > 0 && (cropW != workingBitmap.width || cropH != workingBitmap.height)) {
                workingBitmap = Bitmap.createBitmap(workingBitmap, startX, startY, cropW, cropH)
            }
        }

        // 3. ColorMatrix (Brightness, Contrast, Saturation, Temperature, Filter)
        val combinedMatrix = ColorMatrix()

        // Saturation
        val satMatrix = ColorMatrix()
        val satFactor = (adjustment.saturation + 100f) / 100f // 0.0 to 2.0
        satMatrix.setSaturation(satFactor.coerceIn(0f, 2.5f))
        combinedMatrix.postConcat(satMatrix)

        // Contrast
        val contrastScale = ((adjustment.contrast + 100f) / 100f).coerceIn(0.1f, 3.0f)
        val contrastTranslate = (-0.5f * contrastScale + 0.5f) * 255f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrastScale, 0f, 0f, 0f, contrastTranslate,
                0f, contrastScale, 0f, 0f, contrastTranslate,
                0f, 0f, contrastScale, 0f, contrastTranslate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        combinedMatrix.postConcat(contrastMatrix)

        // Brightness
        val brightVal = adjustment.brightness * 1.2f
        val brightMatrix = ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, brightVal,
                0f, 1f, 0f, 0f, brightVal,
                0f, 0f, 1f, 0f, brightVal,
                0f, 0f, 0f, 1f, 0f
            )
        )
        combinedMatrix.postConcat(brightMatrix)

        // Temperature
        if (adjustment.temperature != 0f) {
            val tempVal = adjustment.temperature * 0.8f
            val tempMatrix = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, tempVal,
                    0f, 1f, 0f, 0f, tempVal * 0.2f,
                    0f, 0f, 1f, 0f, -tempVal,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            combinedMatrix.postConcat(tempMatrix)
        }

        // Filter Matrix
        if (filter.type != FilterType.ORIGINAL) {
            val filterMatrix = getFilterMatrix(filter.type, filter.intensity)
            combinedMatrix.postConcat(filterMatrix)
        }

        // Render adjusted bitmap
        val result = Bitmap.createBitmap(workingBitmap.width, workingBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(combinedMatrix)
        }
        canvas.drawBitmap(workingBitmap, 0f, 0f, paint)

        // Vignette effect
        if (adjustment.vignette > 0f) {
            val w = result.width.toFloat()
            val h = result.height.toFloat()
            val radius = maxOf(w, h) * 0.75f
            val vignetteAlpha = (adjustment.vignette / 100f * 180).toInt().coerceIn(0, 255)
            val gradient = RadialGradient(
                w / 2f, h / 2f, radius,
                intArrayOf(Color.TRANSPARENT, Color.argb(vignetteAlpha, 0, 0, 0)),
                floatArrayOf(0.5f, 1.0f),
                Shader.TileMode.CLAMP
            )
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = gradient
            }
            canvas.drawRect(0f, 0f, w, h, vignettePaint)
        }

        return result
    }

    private fun getFilterMatrix(type: FilterType, intensity: Float): ColorMatrix {
        val i = intensity.coerceIn(0f, 1f)
        val matrix = when (type) {
            FilterType.ORIGINAL -> ColorMatrix()
            FilterType.WARM -> ColorMatrix(
                floatArrayOf(
                    1f + 0.15f * i, 0f, 0f, 0f, 15f * i,
                    0f, 1f + 0.05f * i, 0f, 0f, 5f * i,
                    0f, 0f, 1f - 0.15f * i, 0f, -10f * i,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            FilterType.COOL -> ColorMatrix(
                floatArrayOf(
                    1f - 0.10f * i, 0f, 0f, 0f, -10f * i,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f + 0.20f * i, 0f, 20f * i,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            FilterType.VINTAGE -> ColorMatrix(
                floatArrayOf(
                    0.9f * i + (1 - i), 0.1f * i, 0.1f * i, 0f, 25f * i,
                    0.05f * i, 0.85f * i + (1 - i), 0.05f * i, 0f, 15f * i,
                    0.02f * i, 0.05f * i, 0.70f * i + (1 - i), 0f, 5f * i,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            FilterType.BW, FilterType.MONO -> {
                val bw = ColorMatrix()
                bw.setSaturation(1f - i)
                bw
            }
            FilterType.CINEMA -> ColorMatrix(
                floatArrayOf(
                    1.1f * i + (1 - i), 0f, 0f, 0f, 10f * i,
                    0f, 1.0f, 0f, 0f, 0f,
                    0f, 0f, 1.2f * i + (1 - i), 0f, -5f * i,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            FilterType.PASTEL -> ColorMatrix(
                floatArrayOf(
                    0.85f * i + (1 - i), 0.1f * i, 0.05f * i, 0f, 35f * i,
                    0.05f * i, 0.85f * i + (1 - i), 0.1f * i, 0f, 35f * i,
                    0.1f * i, 0.05f * i, 0.85f * i + (1 - i), 0f, 40f * i,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            FilterType.DRAMATIC -> ColorMatrix(
                floatArrayOf(
                    1.3f * i + (1 - i), 0f, 0f, 0f, -20f * i,
                    0f, 1.3f * i + (1 - i), 0f, 0f, -20f * i,
                    0f, 0f, 1.3f * i + (1 - i), 0f, -20f * i,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            FilterType.RETRO -> ColorMatrix(
                floatArrayOf(
                    1.0f, 0.1f * i, 0f, 0f, 20f * i,
                    0f, 0.95f, 0f, 0f, 10f * i,
                    0f, 0.1f * i, 0.8f * i + (1 - i), 0f, -15f * i,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            FilterType.NATURAL -> ColorMatrix(
                floatArrayOf(
                    1.05f * i + (1 - i), 0f, 0f, 0f, 5f * i,
                    0f, 1.05f * i + (1 - i), 0f, 0f, 5f * i,
                    0f, 0f, 1.02f * i + (1 - i), 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            FilterType.FADE -> ColorMatrix(
                floatArrayOf(
                    0.85f * i + (1 - i), 0f, 0f, 0f, 30f * i,
                    0f, 0.85f * i + (1 - i), 0f, 0f, 30f * i,
                    0f, 0f, 0.85f * i + (1 - i), 0f, 30f * i,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }
        return matrix
    }

    fun computeTargetDimensions(
        currentW: Int,
        currentH: Int,
        preset: ResolutionPreset,
        customW: Int? = null,
        customH: Int? = null,
        scaleMultiplier: Float = 1.0f,
        customRatio: CustomAspectRatio? = null
    ): Pair<Int, Int> {
        val aspect = currentW.toFloat() / currentH.toFloat()

        var (rawW, rawH) = when (preset) {
            ResolutionPreset.ORIGINAL -> Pair(currentW, currentH)
            ResolutionPreset.HD_720P -> {
                if (aspect >= 1f) Pair(1280, (1280 / aspect).toInt())
                else Pair((720 * aspect).toInt(), 720)
            }
            ResolutionPreset.FHD_1080P -> {
                if (aspect >= 1f) Pair(1920, (1920 / aspect).toInt())
                else Pair((1080 * aspect).toInt(), 1080)
            }
            ResolutionPreset.QHD_2K -> {
                if (aspect >= 1f) Pair(2560, (2560 / aspect).toInt())
                else Pair((1440 * aspect).toInt(), 1440)
            }
            ResolutionPreset.UHD_4K -> {
                if (aspect >= 1f) Pair(3840, (3840 / aspect).toInt())
                else Pair((2160 * aspect).toInt(), 2160)
            }
            ResolutionPreset.UHD_5K -> {
                if (aspect >= 1f) Pair(5120, (5120 / aspect).toInt())
                else Pair((2880 * aspect).toInt(), 2880)
            }
            ResolutionPreset.UHD_8K -> {
                if (aspect >= 1f) Pair(7680, (7680 / aspect).toInt())
                else Pair((4320 * aspect).toInt(), 4320)
            }
            ResolutionPreset.CUSTOM -> {
                val cw = customW ?: currentW
                val ch = customH ?: currentH
                Pair(cw.coerceAtLeast(16), ch.coerceAtLeast(16))
            }
        }

        // Apply custom aspect ratio override if active and preset is not custom pixels
        if (customRatio != null && customRatio.isFixed && preset != ResolutionPreset.CUSTOM) {
            val targetRatio = customRatio.ratioWidth / customRatio.ratioHeight
            rawH = (rawW / targetRatio).toInt().coerceAtLeast(16)
        }

        // Apply scale multiplier (e.g., 0.5x, 2.0x, 4.0x)
        val finalW = (rawW * scaleMultiplier).toInt().coerceIn(16, 16384)
        val finalH = (rawH * scaleMultiplier).toInt().coerceIn(16, 16384)

        return Pair(finalW, finalH)
    }

    fun renderCanvasWithFitMode(
        source: Bitmap,
        targetW: Int,
        targetH: Int,
        fitMode: CanvasFitMode,
        themeAccentHex: Long = 0xFF1868F8
    ): Bitmap {
        if (source.width == targetW && source.height == targetH && fitMode == CanvasFitMode.SMART_CROP) {
            return source
        }

        return when (fitMode) {
            CanvasFitMode.STRETCH -> {
                Bitmap.createScaledBitmap(source, targetW, targetH, true)
            }
            CanvasFitMode.SMART_CROP -> {
                val targetRatio = targetW.toFloat() / targetH.toFloat()
                val srcRatio = source.width.toFloat() / source.height.toFloat()

                val cropW: Int
                val cropH: Int
                if (srcRatio > targetRatio) {
                    cropH = source.height
                    cropW = (cropH * targetRatio).toInt().coerceAtMost(source.width)
                } else {
                    cropW = source.width
                    cropH = (cropW / targetRatio).toInt().coerceAtMost(source.height)
                }
                val startX = ((source.width - cropW) / 2).coerceAtLeast(0)
                val startY = ((source.height - cropH) / 2).coerceAtLeast(0)

                val cropped = Bitmap.createBitmap(source, startX, startY, cropW, cropH)
                if (cropped.width != targetW || cropped.height != targetH) {
                    Bitmap.createScaledBitmap(cropped, targetW, targetH, true)
                } else {
                    cropped
                }
            }
            CanvasFitMode.FIT_PAD_BLUR,
            CanvasFitMode.FIT_PAD_BLACK,
            CanvasFitMode.FIT_PAD_WHITE,
            CanvasFitMode.FIT_PAD_THEME,
            CanvasFitMode.FIT_PAD_TRANSPARENT -> {
                val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)

                // 1. Draw Background
                when (fitMode) {
                    CanvasFitMode.FIT_PAD_BLACK -> {
                        canvas.drawColor(Color.BLACK)
                    }
                    CanvasFitMode.FIT_PAD_WHITE -> {
                        canvas.drawColor(Color.WHITE)
                    }
                    CanvasFitMode.FIT_PAD_THEME -> {
                        canvas.drawColor(themeAccentHex.toInt())
                    }
                    CanvasFitMode.FIT_PAD_TRANSPARENT -> {
                        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                    }
                    CanvasFitMode.FIT_PAD_BLUR -> {
                        val blurSample = Bitmap.createScaledBitmap(source, 60, (60 * (source.height.toFloat() / source.width.toFloat())).toInt().coerceAtLeast(10), true)
                        val blurred = fastBlur(blurSample, 12)
                        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                            alpha = 230
                        }
                        canvas.drawBitmap(blurred, null, Rect(0, 0, targetW, targetH), bgPaint)

                        // Dark tint overlay on blur for contrast
                        val dimPaint = Paint().apply {
                            color = Color.argb(80, 0, 0, 0)
                        }
                        canvas.drawRect(0f, 0f, targetW.toFloat(), targetH.toFloat(), dimPaint)
                    }
                    else -> {}
                }

                // 2. Draw scaled image centered
                val srcAspect = source.width.toFloat() / source.height.toFloat()
                val targetAspect = targetW.toFloat() / targetH.toFloat()

                val drawW: Int
                val drawH: Int
                if (srcAspect > targetAspect) {
                    drawW = targetW
                    drawH = (targetW / srcAspect).toInt()
                } else {
                    drawH = targetH
                    drawW = (targetH * srcAspect).toInt()
                }

                val left = (targetW - drawW) / 2
                val top = (targetH - drawH) / 2
                val destRect = Rect(left, top, left + drawW, top + drawH)
                val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(source, null, destRect, imagePaint)

                output
            }
        }
    }

    private fun fastBlur(sentBitmap: Bitmap, radius: Int): Bitmap {
        val bitmap = sentBitmap.copy(sentBitmap.config ?: Bitmap.Config.ARGB_8888, true)
        if (radius < 1) return bitmap

        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(maxOf(w, h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (idx in 0 until 256 * divsum) {
            dv[idx] = idx / divsum
        }

        yw = 0
        yi = 0

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var rbs: Int
        val routsum: Int
        val goutsum: Int
        val boutsum: Int
        val rinsum: Int
        val ginsum: Int
        val binsum: Int

        for (curY in 0 until h) {
            rsum = 0
            gsum = 0
            bsum = 0
            for (idx in -radius..radius) {
                p = pix[yi + minOf(wm, maxOf(idx, 0))]
                rsum += (p shr 16) and 0xFF
                gsum += (p shr 8) and 0xFF
                bsum += p and 0xFF
            }
            for (curX in 0 until w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                if (curY == 0) {
                    vmin[curX] = minOf(curX + radius + 1, wm)
                }
                p = pix[yw + vmin[curX]]
                val p2 = pix[yw + maxOf(curX - radius, 0)]

                rsum += ((p shr 16) and 0xFF) - ((p2 shr 16) and 0xFF)
                gsum += ((p shr 8) and 0xFF) - ((p2 shr 8) and 0xFF)
                bsum += (p and 0xFF) - (p2 and 0xFF)

                yi++
            }
            yw += w
        }

        for (curX in 0 until w) {
            rsum = 0
            gsum = 0
            bsum = 0
            yp = -radius * w
            for (idx in -radius..radius) {
                yi = maxOf(0, yp) + curX
                rsum += r[yi]
                gsum += g[yi]
                bsum += b[yi]
                yp += w
            }
            yi = curX
            for (curY in 0 until h) {
                pix[yi] = (0xFF shl 24) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                if (curX == 0) {
                    vmin[curY] = minOf(curY + radius + 1, hm) * w
                }
                p = curX + vmin[curY]
                val p2 = curX + maxOf(curY - radius, 0) * w

                rsum += r[p] - r[p2]
                gsum += g[p] - g[p2]
                bsum += b[p] - b[p2]

                yi += w
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }

    fun estimateOutputSize(
        width: Int,
        height: Int,
        format: ExportFormat,
        quality: Int
    ): Long {
        val pixels = width.toLong() * height.toLong()
        return when (format) {
            ExportFormat.JPEG -> {
                val bpp = 0.05 + (quality.toDouble() / 100.0) * 0.35
                (pixels * bpp).toLong().coerceAtLeast(10_240L)
            }
            ExportFormat.PNG -> {
                (pixels * 1.8).toLong().coerceAtLeast(20_480L)
            }
            ExportFormat.WEBP -> {
                val bpp = 0.03 + (quality.toDouble() / 100.0) * 0.25
                (pixels * bpp).toLong().coerceAtLeast(8_192L)
            }
            ExportFormat.BMP -> {
                (pixels * 3) + 54L
            }
            ExportFormat.TIFF -> {
                (pixels * 3) + 1024L
            }
            ExportFormat.PDF -> {
                val bpp = 0.06 + (quality.toDouble() / 100.0) * 0.3
                (pixels * bpp + 4096).toLong()
            }
        }
    }

    suspend fun exportImage(
        context: Context,
        source: Bitmap,
        options: ExportOptions,
        outputName: String,
        themeHex: Long = 0xFF1868F8
    ): File = withContext(Dispatchers.IO) {
        val (targetW, targetH) = computeTargetDimensions(
            source.width,
            source.height,
            options.resolution,
            options.customWidth,
            options.customHeight,
            options.resolutionScale,
            options.customAspectRatio
        )

        val processedBitmap = renderCanvasWithFitMode(
            source = source,
            targetW = targetW,
            targetH = targetH,
            fitMode = options.fitMode,
            themeAccentHex = themeHex
        )

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val filename = if (outputName.contains('.')) outputName else "$outputName.${options.format.extension}"
        val targetFile = File(exportDir, filename)

        when (options.format) {
            ExportFormat.JPEG -> {
                FileOutputStream(targetFile).use { out ->
                    processedBitmap.compress(Bitmap.CompressFormat.JPEG, options.quality.coerceIn(1, 100), out)
                }
            }
            ExportFormat.PNG -> {
                FileOutputStream(targetFile).use { out ->
                    processedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
            ExportFormat.WEBP -> {
                FileOutputStream(targetFile).use { out ->
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        val format = if (options.quality >= 100) Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.WEBP_LOSSY
                        processedBitmap.compress(format, options.quality.coerceIn(1, 100), out)
                    } else {
                        @Suppress("DEPRECATION")
                        processedBitmap.compress(Bitmap.CompressFormat.WEBP, options.quality.coerceIn(1, 100), out)
                    }
                }
            }
            ExportFormat.BMP -> {
                writeBmpFile(processedBitmap, targetFile)
            }
            ExportFormat.TIFF -> {
                writeTiffFile(processedBitmap, targetFile)
            }
            ExportFormat.PDF -> {
                return@withContext PdfExporter.exportToPdf(
                    context = context,
                    bitmaps = listOf(processedBitmap),
                    outputFileName = filename
                )
            }
        }

        try {
            saveToMediaStore(context, targetFile, filename, options.format.mimeType)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        targetFile
    }

    private fun saveToMediaStore(context: Context, file: File, displayName: String, mimeType: String) {
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, mimeType)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/APOLLO")
                put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { input -> input.copyTo(out) }
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                values.clear()
                values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }
        }
    }

    private fun writeBmpFile(bitmap: Bitmap, file: File) {
        val w = bitmap.width
        val h = bitmap.height
        val rowPadding = (4 - (w * 3) % 4) % 4
        val imageSize = (w * 3 + rowPadding) * h
        val fileSize = 54 + imageSize

        val buffer = ByteBuffer.allocate(fileSize).order(ByteOrder.LITTLE_ENDIAN)

        // BMP Header
        buffer.put('B'.code.toByte())
        buffer.put('M'.code.toByte())
        buffer.putInt(fileSize)
        buffer.putShort(0)
        buffer.putShort(0)
        buffer.putInt(54)

        // DIB Header
        buffer.putInt(40)
        buffer.putInt(w)
        buffer.putInt(h)
        buffer.putShort(1)
        buffer.putShort(24)
        buffer.putInt(0)
        buffer.putInt(imageSize)
        buffer.putInt(2835)
        buffer.putInt(2835)
        buffer.putInt(0)
        buffer.putInt(0)

        val pixels = IntArray(w)
        for (y in h - 1 downTo 0) {
            bitmap.getPixels(pixels, 0, w, 0, y, w, 1)
            for (x in 0 until w) {
                val color = pixels[x]
                buffer.put((color and 0xFF).toByte())
                buffer.put(((color shr 8) and 0xFF).toByte())
                buffer.put(((color shr 16) and 0xFF).toByte())
            }
            for (p in 0 until rowPadding) {
                buffer.put(0.toByte())
            }
        }

        FileOutputStream(file).use { it.write(buffer.array()) }
    }

    private fun writeTiffFile(bitmap: Bitmap, file: File) {
        val w = bitmap.width
        val h = bitmap.height
        val stripBytes = w * h * 3
        val ifdOffset = 8 + stripBytes
        val numEntries: Short = 9
        val extraDataOffset = ifdOffset + 2 + (numEntries * 12) + 4
        val totalSize = extraDataOffset + 6

        val buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)

        buffer.put('I'.code.toByte())
        buffer.put('I'.code.toByte())
        buffer.putShort(42)
        buffer.putInt(ifdOffset)

        val pixels = IntArray(w)
        for (y in 0 until h) {
            bitmap.getPixels(pixels, 0, w, 0, y, w, 1)
            for (x in 0 until w) {
                val c = pixels[x]
                buffer.put(((c shr 16) and 0xFF).toByte())
                buffer.put(((c shr 8) and 0xFF).toByte())
                buffer.put((c and 0xFF).toByte())
            }
        }

        buffer.putShort(numEntries)

        fun putTag(tag: Int, type: Int, count: Int, valueOrOffset: Int) {
            buffer.putShort(tag.toShort())
            buffer.putShort(type.toShort())
            buffer.putInt(count)
            buffer.putInt(valueOrOffset)
        }

        putTag(256, 4, 1, w)
        putTag(257, 4, 1, h)
        putTag(258, 3, 3, extraDataOffset)
        putTag(259, 3, 1, 1)
        putTag(262, 3, 1, 2)
        putTag(273, 4, 1, 8)
        putTag(277, 3, 1, 3)
        putTag(278, 4, 1, h)
        putTag(279, 4, 1, stripBytes)

        buffer.putInt(0)

        buffer.putShort(8)
        buffer.putShort(8)
        buffer.putShort(8)

        FileOutputStream(file).use { it.write(buffer.array()) }
    }
}
