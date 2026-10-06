package com.example.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.R
import com.example.data.db.ApolloDatabase
import com.example.data.db.ExportHistoryDao
import com.example.data.db.ExportHistoryEntity
import com.example.data.model.ApolloAlbum
import com.example.data.model.ApolloImage
import com.example.data.model.ExportFormat
import com.example.data.model.ExportOptions
import com.example.data.model.ExportPreset
import com.example.data.model.MetadataPolicy
import com.example.data.model.ResolutionPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.InputStream

class ApolloRepository(private val context: Context) {

    private val db = ApolloDatabase.getInstance(context)
    val historyDao: ExportHistoryDao = db.exportHistoryDao()

    private val _images = MutableStateFlow<List<ApolloImage>>(emptyList())
    val images = _images.asStateFlow()

    private val _albums = MutableStateFlow<List<ApolloAlbum>>(emptyList())
    val albums = _albums.asStateFlow()

    private val _presets = MutableStateFlow<List<ExportPreset>>(emptyList())
    val presets = _presets.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        val initialList = mutableListOf<ApolloImage>()

        // Add pre-loaded sample images from res/drawable (Alpine Lake, Macro Flower, City Sunset)
        try {
            initialList.add(
                ApolloImage(
                    id = "sample_lake_1",
                    uri = Uri.parse("android.resource://${context.packageName}/${R.drawable.sample_lake}"),
                    name = "IMG_20261005_1245.jpg",
                    format = "JPEG",
                    width = 4032,
                    height = 3024,
                    sizeBytes = 12_800_000L,
                    isFavorite = true,
                    album = "Camera",
                    drawableResId = R.drawable.sample_lake
                )
            )
            initialList.add(
                ApolloImage(
                    id = "sample_flower_2",
                    uri = Uri.parse("android.resource://${context.packageName}/${R.drawable.sample_flower}"),
                    name = "macro_flora.png",
                    format = "PNG",
                    width = 2800,
                    height = 2100,
                    sizeBytes = 2_850_000L,
                    isFavorite = false,
                    album = "Screenshots",
                    drawableResId = R.drawable.sample_flower
                )
            )
            initialList.add(
                ApolloImage(
                    id = "sample_city_3",
                    uri = Uri.parse("android.resource://${context.packageName}/${R.drawable.sample_city}"),
                    name = "metropolis_dusk.webp",
                    format = "WEBP",
                    width = 3840,
                    height = 2160,
                    sizeBytes = 1_920_000L,
                    isFavorite = true,
                    album = "Downloads",
                    drawableResId = R.drawable.sample_city
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        _images.value = initialList

        // Initial Albums
        _albums.value = listOf(
            ApolloAlbum("album_camera", "Camera", 12, R.drawable.sample_lake, true),
            ApolloAlbum("album_screenshots", "Screenshots", 8, R.drawable.sample_flower, true),
            ApolloAlbum("album_downloads", "Downloads", 5, R.drawable.sample_city, true),
            ApolloAlbum("album_apollo", "APOLLO Exports", 14, R.drawable.sample_lake, true),
            ApolloAlbum("album_favorites", "Favorites", 7, R.drawable.sample_city, true)
        )

        // Predefined Export Presets / Recipes
        _presets.value = listOf(
            ExportPreset(
                id = "preset_insta",
                title = "Instagram 4:5",
                description = "1080 × 1350 • High Quality JPEG • Privacy stripped",
                format = ExportFormat.JPEG,
                resolution = ResolutionPreset.FHD_1080P,
                quality = 92,
                metadataPolicy = MetadataPolicy.REMOVE_ALL,
                count = 12
            ),
            ExportPreset(
                id = "preset_4k_archive",
                title = "4K Master Archive",
                description = "3840 × 2160 • Uncompressed TIFF • Keep EXIF",
                format = ExportFormat.TIFF,
                resolution = ResolutionPreset.UHD_4K,
                quality = 100,
                metadataPolicy = MetadataPolicy.KEEP_ALL,
                count = 8
            ),
            ExportPreset(
                id = "preset_web_fast",
                title = "Web Optimizer",
                description = "1080p • Lossy WebP 85% • Fast page load",
                format = ExportFormat.WEBP,
                resolution = ResolutionPreset.FHD_1080P,
                quality = 85,
                metadataPolicy = MetadataPolicy.REMOVE_ALL,
                count = 20
            ),
            ExportPreset(
                id = "preset_print_pdf",
                title = "Print PDF (A4)",
                description = "High-res document conversion • Print ready",
                format = ExportFormat.PDF,
                resolution = ResolutionPreset.ORIGINAL,
                quality = 95,
                metadataPolicy = MetadataPolicy.KEEP_ALL,
                count = 6
            ),
            ExportPreset(
                id = "preset_clean_png",
                title = "Lossless PNG",
                description = "Original resolution • Crisp graphic export",
                format = ExportFormat.PNG,
                resolution = ResolutionPreset.ORIGINAL,
                quality = 100,
                metadataPolicy = MetadataPolicy.REMOVE_GPS_ONLY,
                count = 15
            ),
            ExportPreset(
                id = "preset_8k_cinema",
                title = "8K Super-Res",
                description = "7680 × 4320 • Ultra high-definition master",
                format = ExportFormat.JPEG,
                resolution = ResolutionPreset.UHD_8K,
                quality = 98,
                metadataPolicy = MetadataPolicy.KEEP_ALL,
                count = 4
            )
        )
    }

    fun addImportedUris(uris: List<Uri>) {
        val current = _images.value.toMutableList()
        for (uri in uris) {
            val image = inspectUri(uri)
            current.add(0, image)
        }
        _images.value = current
    }

    private fun inspectUri(uri: Uri): ApolloImage {
        var w = 1920
        var h = 1080
        var size = 2_400_000L
        var name = "IMG_${System.currentTimeMillis().toString().takeLast(6)}.jpg"
        var format = "JPEG"

        try {
            // Determine name
            uri.lastPathSegment?.let { segment ->
                val clean = segment.substringAfterLast('/')
                if (clean.isNotBlank()) name = clean
            }
            if (name.contains('.')) {
                format = name.substringAfterLast('.').uppercase()
            }

            // Determine dimensions safely
            var inputStream: InputStream? = null
            try {
                inputStream = context.contentResolver.openInputStream(uri)
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(inputStream, null, opts)
                if (opts.outWidth > 0 && opts.outHeight > 0) {
                    w = opts.outWidth
                    h = opts.outHeight
                }
            } finally {
                inputStream?.close()
            }

            // Determine size
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                if (afd.length > 0) size = afd.length
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ApolloImage(
            id = "imported_${System.currentTimeMillis()}_${(100..999).random()}",
            uri = uri,
            name = name,
            format = format,
            width = w,
            height = h,
            sizeBytes = size,
            album = "Imported"
        )
    }

    fun toggleFavorite(imageId: String) {
        val current = _images.value.map {
            if (it.id == imageId) it.copy(isFavorite = !it.isFavorite) else it
        }
        _images.value = current
    }

    fun deleteImage(imageId: String) {
        _images.value = _images.value.filterNot { it.id == imageId }
    }

    suspend fun recordExport(
        originalName: String,
        outputName: String,
        outputFile: File,
        sourceFormat: String,
        targetFormat: String,
        width: Int,
        height: Int
    ): Long {
        val entity = ExportHistoryEntity(
            originalName = originalName,
            outputName = outputName,
            outputUriString = Uri.fromFile(outputFile).toString(),
            sourceFormat = sourceFormat,
            targetFormat = targetFormat,
            width = width,
            height = height,
            sizeBytes = outputFile.length(),
            timestamp = System.currentTimeMillis()
        )
        return historyDao.insert(entity)
    }

    fun loadDeviceGallery(): Int {
        val list = mutableListOf<ApolloImage>()
        try {
            val projection = arrayOf(
                android.provider.MediaStore.Images.Media._ID,
                android.provider.MediaStore.Images.Media.DISPLAY_NAME,
                android.provider.MediaStore.Images.Media.WIDTH,
                android.provider.MediaStore.Images.Media.HEIGHT,
                android.provider.MediaStore.Images.Media.SIZE,
                android.provider.MediaStore.Images.Media.MIME_TYPE,
                android.provider.MediaStore.Images.Media.DATE_MODIFIED
            )

            context.contentResolver.query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${android.provider.MediaStore.Images.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DISPLAY_NAME)
                val wCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.WIDTH)
                val hCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.HEIGHT)
                val sizeCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.MIME_TYPE)

                var count = 0
                while (cursor.moveToNext() && count < 100) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "IMG_$id.jpg"
                    val w = cursor.getInt(wCol).coerceAtLeast(1)
                    val h = cursor.getInt(hCol).coerceAtLeast(1)
                    val size = cursor.getLong(sizeCol).coerceAtLeast(1024L)
                    val mime = cursor.getString(mimeCol) ?: "image/jpeg"
                    val contentUri = android.content.ContentUris.withAppendedId(
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val format = when {
                        mime.contains("png", true) -> "PNG"
                        mime.contains("webp", true) -> "WEBP"
                        mime.contains("gif", true) -> "GIF"
                        mime.contains("heic", true) || mime.contains("heif", true) -> "HEIF"
                        else -> "JPEG"
                    }

                    list.add(
                        ApolloImage(
                            id = "gallery_$id",
                            uri = contentUri,
                            name = name,
                            format = format,
                            width = w,
                            height = h,
                            sizeBytes = size,
                            album = "Gallery"
                        )
                    )
                    count++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (list.isNotEmpty()) {
            val existing = _images.value.filter { it.drawableResId != null || !it.id.startsWith("gallery_") }
            _images.value = list + existing
        }
        return list.size
    }
}
