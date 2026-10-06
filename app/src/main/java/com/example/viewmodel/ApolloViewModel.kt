package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ApolloRepository
import com.example.data.db.ExportHistoryEntity
import com.example.data.model.ApolloAlbum
import com.example.data.model.ApolloImage
import com.example.data.model.ApolloTheme
import com.example.data.model.CanvasFitMode
import com.example.data.model.CropAspectRatio
import com.example.data.model.CustomAspectRatio
import com.example.data.model.ExportFormat
import com.example.data.model.ExportOptions
import com.example.data.model.ExportPreset
import com.example.data.model.FilterConfig
import com.example.data.model.FilterType
import com.example.data.model.ImageAdjustment
import com.example.data.model.ResolutionPreset
import com.example.data.processing.ExifInspector
import com.example.data.processing.ImageMetadataInfo
import com.example.data.processing.ImageProcessor
import com.example.data.processing.PdfExporter
import com.example.ui.theme.UiScaleMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class ApolloScreen {
    PICTURES,
    VIEWER,
    EDITOR,
    EXPORT,
    ALBUMS,
    COLLECTIONS,
    MENU,
    BATCH,
    PDF_STUDIO,
    HISTORY
}

enum class EditorToolTab {
    ADJUST,
    FILTERS,
    CROP,
    EFFECTS
}

class ApolloViewModel(application: Application) : AndroidViewModel(application) {

    val repository = ApolloRepository(application)

    val images: StateFlow<List<ApolloImage>> = repository.images
    val albums: StateFlow<List<ApolloAlbum>> = repository.albums
    val presets: StateFlow<List<ExportPreset>> = repository.presets
    val exportHistory: StateFlow<List<ExportHistoryEntity>> = repository.historyDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Navigation
    private val _currentScreen = MutableStateFlow(ApolloScreen.PICTURES)
    val currentScreen = _currentScreen.asStateFlow()

    private val _navigationStack = mutableListOf<ApolloScreen>()

    // Current selected image
    private val _selectedImage = MutableStateFlow<ApolloImage?>(null)
    val selectedImage = _selectedImage.asStateFlow()

    // Loaded Bitmap for editing / viewing
    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap = _previewBitmap.asStateFlow()

    private val _editedPreviewBitmap = MutableStateFlow<Bitmap?>(null)
    val editedPreviewBitmap = _editedPreviewBitmap.asStateFlow()

    // Editor States
    private val _currentAdjustment = MutableStateFlow(ImageAdjustment())
    val currentAdjustment = _currentAdjustment.asStateFlow()

    private val _currentFilter = MutableStateFlow(FilterConfig())
    val currentFilter = _currentFilter.asStateFlow()

    private val _currentCropRatio = MutableStateFlow(CropAspectRatio.ORIGINAL)
    val currentCropRatio = _currentCropRatio.asStateFlow()

    private val _activeCustomRatio = MutableStateFlow<CustomAspectRatio?>(null)
    val activeCustomRatio = _activeCustomRatio.asStateFlow()

    private val _activeEditorTab = MutableStateFlow(EditorToolTab.ADJUST)
    val activeEditorTab = _activeEditorTab.asStateFlow()

    private val _beforeAfterSplit = MutableStateFlow(0.5f)
    val beforeAfterSplit = _beforeAfterSplit.asStateFlow()

    private val undoStack = mutableListOf<ImageAdjustment>()
    private val redoStack = mutableListOf<ImageAdjustment>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo = _canRedo.asStateFlow()

    // Aspect Ratio Library (Built-in + Custom user ratios)
    private val _customAspectRatios = MutableStateFlow<List<CustomAspectRatio>>(CustomAspectRatio.DEFAULT_BUILT_IN)
    val customAspectRatios = _customAspectRatios.asStateFlow()

    // Export States
    private val _exportOptions = MutableStateFlow(ExportOptions())
    val exportOptions = _exportOptions.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting = _isExporting.asStateFlow()

    private val _lastExportedFile = MutableStateFlow<File?>(null)
    val lastExportedFile = _lastExportedFile.asStateFlow()

    // Metadata Dialog State
    private val _metadataInfo = MutableStateFlow<ImageMetadataInfo?>(null)
    val metadataInfo = _metadataInfo.asStateFlow()

    private val _showMetadataDialog = MutableStateFlow(false)
    val showMetadataDialog = _showMetadataDialog.asStateFlow()

    // Appearance State & Compact Mode
    private val _activeTheme = MutableStateFlow(ApolloTheme.SKY)
    val activeTheme = _activeTheme.asStateFlow()

    private val _isDarkMode = MutableStateFlow<Boolean?>(null) // null = system default
    val isDarkMode = _isDarkMode.asStateFlow()

    private val _uiScaleMode = MutableStateFlow(UiScaleMode.AUTO)
    val uiScaleMode = _uiScaleMode.asStateFlow()

    // Batch Convert State
    private val _batchSelectedImages = MutableStateFlow<List<ApolloImage>>(emptyList())
    val batchSelectedImages = _batchSelectedImages.asStateFlow()

    private val _batchProgress = MutableStateFlow(0)
    val batchProgress = _batchProgress.asStateFlow()

    private val _batchTotal = MutableStateFlow(0)
    val batchTotal = _batchTotal.asStateFlow()

    private val _batchIsRunning = MutableStateFlow(false)
    val batchIsRunning = _batchIsRunning.asStateFlow()

    private val _batchResults = MutableStateFlow<List<File>>(emptyList())
    val batchResults = _batchResults.asStateFlow()

    init {
        // Select first image by default when loaded
        viewModelScope.launch {
            images.collect { list ->
                if (_selectedImage.value == null && list.isNotEmpty()) {
                    selectImage(list.first(), navigate = false)
                }
            }
        }
    }

    // --- NAVIGATION ---
    fun navigateTo(screen: ApolloScreen) {
        if (_currentScreen.value != screen) {
            _navigationStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (_navigationStack.isNotEmpty()) {
            val prev = _navigationStack.removeAt(_navigationStack.size - 1)
            _currentScreen.value = prev
            return true
        } else if (_currentScreen.value != ApolloScreen.PICTURES) {
            _currentScreen.value = ApolloScreen.PICTURES
            return true
        }
        return false
    }

    fun selectTab(screen: ApolloScreen) {
        _navigationStack.clear()
        _currentScreen.value = screen
    }

    // --- IMAGE SELECTION ---
    fun selectImage(image: ApolloImage, navigate: Boolean = true) {
        _selectedImage.value = image
        loadBitmapsForImage(image)
        resetEditorAdjustments()
        if (navigate) {
            navigateTo(ApolloScreen.VIEWER)
        }
    }

    fun nextImage() {
        val list = images.value
        val curr = _selectedImage.value ?: return
        val idx = list.indexOfFirst { it.id == curr.id }
        if (idx >= 0 && idx < list.size - 1) {
            selectImage(list[idx + 1], navigate = false)
        }
    }

    fun previousImage() {
        val list = images.value
        val curr = _selectedImage.value ?: return
        val idx = list.indexOfFirst { it.id == curr.id }
        if (idx > 0) {
            selectImage(list[idx - 1], navigate = false)
        }
    }

    fun importUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        repository.addImportedUris(uris)
        viewModelScope.launch {
            val list = images.value
            if (list.isNotEmpty()) {
                selectImage(list.first(), navigate = false)
            }
        }
    }

    fun importWithCustomResolution(
        uris: List<Uri>,
        targetRatio: CustomAspectRatio?,
        targetW: Int?,
        targetH: Int?,
        fitMode: CanvasFitMode
    ) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                for (uri in uris) {
                    val rawStream = getApplication<Application>().contentResolver.openInputStream(uri)
                    val originalBitmap = rawStream?.use { android.graphics.BitmapFactory.decodeStream(it) }
                    if (originalBitmap != null) {
                        val finalW: Int
                        val finalH: Int

                        if (targetW != null && targetH != null && targetW > 0 && targetH > 0) {
                            finalW = targetW
                            finalH = targetH
                        } else if (targetRatio != null && targetRatio.isFixed) {
                            val ratio = targetRatio.ratioWidth / targetRatio.ratioHeight
                            if (originalBitmap.width > originalBitmap.height) {
                                finalW = originalBitmap.width
                                finalH = (finalW / ratio).toInt().coerceAtLeast(16)
                            } else {
                                finalH = originalBitmap.height
                                finalW = (finalH * ratio).toInt().coerceAtLeast(16)
                            }
                        } else {
                            finalW = originalBitmap.width
                            finalH = originalBitmap.height
                        }

                        val processed = ImageProcessor.renderCanvasWithFitMode(
                            source = originalBitmap,
                            targetW = finalW,
                            targetH = finalH,
                            fitMode = fitMode,
                            themeAccentHex = _activeTheme.value.primaryHex
                        )

                        val importDir = File(getApplication<Application>().cacheDir, "imported").apply { mkdirs() }
                        val file = File(importDir, "import_custom_${System.currentTimeMillis()}.jpg")
                        java.io.FileOutputStream(file).use { out ->
                            processed.compress(Bitmap.CompressFormat.JPEG, 95, out)
                        }

                        val fileUri = Uri.fromFile(file)
                        repository.addImportedUris(listOf(fileUri))
                    } else {
                        repository.addImportedUris(listOf(uri))
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Imported ${uris.size} image(s) with custom aspect ratio/resolution", Toast.LENGTH_SHORT).show()
                    val list = images.value
                    if (list.isNotEmpty()) {
                        selectImage(list.first(), navigate = false)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Import with custom resolution: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun toggleFavorite(image: ApolloImage) {
        repository.toggleFavorite(image.id)
        if (_selectedImage.value?.id == image.id) {
            _selectedImage.value = _selectedImage.value?.copy(isFavorite = !image.isFavorite)
        }
    }

    fun deleteCurrentImage() {
        val curr = _selectedImage.value ?: return
        repository.deleteImage(curr.id)
        val remaining = images.value.filterNot { it.id == curr.id }
        if (remaining.isNotEmpty()) {
            selectImage(remaining.first(), navigate = false)
        } else {
            _selectedImage.value = null
            _previewBitmap.value = null
            _editedPreviewBitmap.value = null
        }
        navigateTo(ApolloScreen.PICTURES)
    }

    // --- BITMAP LOADING & EDITING ---
    private fun loadBitmapsForImage(image: ApolloImage) {
        viewModelScope.launch {
            val bmp = ImageProcessor.loadBitmap(getApplication(), image, maxDimension = 1200)
            _previewBitmap.value = bmp
            updateEditedPreview()
        }
    }

    fun scanGallery() {
        viewModelScope.launch(Dispatchers.IO) {
            val count = repository.loadDeviceGallery()
            withContext(Dispatchers.Main) {
                if (count > 0) {
                    Toast.makeText(getApplication(), "Loaded $count photos from device storage", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun setEditorTab(tab: EditorToolTab) {
        _activeEditorTab.value = tab
    }

    fun setBeforeAfterSplit(split: Float) {
        _beforeAfterSplit.value = split.coerceIn(0.05f, 0.95f)
    }

    fun updateAdjustmentLive(transform: (ImageAdjustment) -> ImageAdjustment) {
        val updated = transform(_currentAdjustment.value)
        _currentAdjustment.value = updated
        updateEditedPreview()
    }

    fun pushUndoState() {
        undoStack.add(_currentAdjustment.value)
        redoStack.clear()
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = false
    }

    fun updateAdjustment(transform: (ImageAdjustment) -> ImageAdjustment) {
        val old = _currentAdjustment.value
        undoStack.add(old)
        redoStack.clear()
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = false

        val updated = transform(old)
        _currentAdjustment.value = updated
        updateEditedPreview()
    }

    fun setFilter(filterType: FilterType, intensity: Float = 1.0f) {
        _currentFilter.value = FilterConfig(filterType, intensity)
        updateEditedPreview()
    }

    fun setCropRatio(ratio: CropAspectRatio) {
        _currentCropRatio.value = ratio
        _activeCustomRatio.value = null
        updateEditedPreview()
    }

    fun setCustomCropRatio(ratio: CustomAspectRatio) {
        _activeCustomRatio.value = ratio
        _currentCropRatio.value = CropAspectRatio.CUSTOM
        updateEditedPreview()
    }

    fun addCustomAspectRatio(label: String, rw: Float, rh: Float) {
        if (rw <= 0f || rh <= 0f) return
        val newRatio = CustomAspectRatio(
            label = label.ifBlank { "${rw.toInt()}:${rh.toInt()}" },
            ratioWidth = rw,
            ratioHeight = rh,
            isBuiltIn = false,
            category = "User Defined"
        )
        val list = _customAspectRatios.value.toMutableList()
        list.add(newRatio)
        _customAspectRatios.value = list
        setCustomCropRatio(newRatio)
        updateExportOptions { it.copy(customAspectRatio = newRatio) }
        Toast.makeText(getApplication(), "Added aspect ratio ${newRatio.label} (${newRatio.formattedRatio})", Toast.LENGTH_SHORT).show()
    }

    fun removeCustomAspectRatio(id: String) {
        val list = _customAspectRatios.value.filterNot { it.id == id && !it.isBuiltIn }
        _customAspectRatios.value = list
        if (_activeCustomRatio.value?.id == id) {
            _activeCustomRatio.value = null
            _currentCropRatio.value = CropAspectRatio.ORIGINAL
        }
    }

    fun rotate90() {
        updateAdjustment {
            val deg = (it.rotationDegrees + 90) % 360
            it.copy(rotationDegrees = deg)
        }
    }

    fun flipHorizontal() {
        updateAdjustment {
            it.copy(flipHorizontal = !it.flipHorizontal)
        }
    }

    fun flipVertical() {
        updateAdjustment {
            it.copy(flipVertical = !it.flipVertical)
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(_currentAdjustment.value)
            _currentAdjustment.value = prev
            _canUndo.value = undoStack.isNotEmpty()
            _canRedo.value = true
            updateEditedPreview()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(_currentAdjustment.value)
            _currentAdjustment.value = next
            _canUndo.value = true
            _canRedo.value = redoStack.isNotEmpty()
            updateEditedPreview()
        }
    }

    fun resetEditorAdjustments() {
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
        _currentAdjustment.value = ImageAdjustment()
        _currentFilter.value = FilterConfig()
        _currentCropRatio.value = CropAspectRatio.ORIGINAL
        _activeCustomRatio.value = null
        updateEditedPreview()
    }

    private fun updateEditedPreview() {
        val base = _previewBitmap.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val adjusted = ImageProcessor.applyAdjustmentsAndFilters(
                source = base,
                adjustment = _currentAdjustment.value,
                filter = _currentFilter.value,
                cropRatio = _currentCropRatio.value,
                customRatio = _activeCustomRatio.value
            )
            _editedPreviewBitmap.value = adjusted
        }
    }

    // --- METADATA ---
    fun inspectMetadata(image: ApolloImage) {
        viewModelScope.launch(Dispatchers.IO) {
            val info = ExifInspector.extractMetadata(
                context = getApplication(),
                uri = image.uri,
                fileName = image.name,
                fileSizeFormatted = image.formattedSize,
                width = image.width,
                height = image.height,
                format = image.format
            )
            _metadataInfo.value = info
            _showMetadataDialog.value = true
        }
    }

    fun dismissMetadataDialog() {
        _showMetadataDialog.value = false
    }

    // --- EXPORT OPTIONS ---
    fun updateExportOptions(transform: (ExportOptions) -> ExportOptions) {
        _exportOptions.value = transform(_exportOptions.value)
    }

    fun setExportResolutionPreset(preset: ResolutionPreset) {
        _exportOptions.value = _exportOptions.value.copy(resolution = preset)
    }

    fun setExportScale(scale: Float) {
        _exportOptions.value = _exportOptions.value.copy(resolutionScale = scale)
    }

    fun setExportCustomDimensions(w: Int?, h: Int?, lock: Boolean = true) {
        _exportOptions.value = _exportOptions.value.copy(
            resolution = ResolutionPreset.CUSTOM,
            customWidth = w,
            customHeight = h,
            lockAspectRatio = lock
        )
    }

    fun setExportAspectRatio(ratio: CustomAspectRatio) {
        _exportOptions.value = _exportOptions.value.copy(
            customAspectRatio = ratio,
            cropRatio = if (ratio.isBuiltIn) {
                CropAspectRatio.values().find { it.label == ratio.label } ?: CropAspectRatio.CUSTOM
            } else CropAspectRatio.CUSTOM
        )
    }

    fun applyPreset(preset: ExportPreset) {
        _exportOptions.value = _exportOptions.value.copy(
            format = preset.format,
            resolution = preset.resolution,
            quality = preset.quality,
            metadataPolicy = preset.metadataPolicy
        )
        navigateTo(ApolloScreen.EXPORT)
    }

    fun exportCurrentImage(onComplete: (File?) -> Unit = {}) {
        val curr = _selectedImage.value ?: return
        val opts = _exportOptions.value

        viewModelScope.launch {
            _isExporting.value = true
            try {
                val fullSource = ImageProcessor.loadBitmap(getApplication(), curr) ?: _previewBitmap.value
                if (fullSource != null) {
                    val adjusted = ImageProcessor.applyAdjustmentsAndFilters(
                        source = fullSource,
                        adjustment = _currentAdjustment.value,
                        filter = _currentFilter.value,
                        cropRatio = _currentCropRatio.value,
                        customRatio = _activeCustomRatio.value
                    )

                    val baseName = (opts.customFileName ?: curr.name).substringBeforeLast('.')
                    val outName = "${baseName}_${opts.format.name.lowercase()}_${System.currentTimeMillis().toString().takeLast(4)}"
                    val exportedFile = ImageProcessor.exportImage(
                        context = getApplication(),
                        source = adjusted,
                        options = opts,
                        outputName = outName,
                        themeHex = _activeTheme.value.primaryHex
                    )

                    _lastExportedFile.value = exportedFile

                    val (w, h) = ImageProcessor.computeTargetDimensions(
                        adjusted.width,
                        adjusted.height,
                        opts.resolution,
                        opts.customWidth,
                        opts.customHeight,
                        opts.resolutionScale,
                        opts.customAspectRatio
                    )

                    repository.recordExport(
                        originalName = curr.name,
                        outputName = exportedFile.name,
                        outputFile = exportedFile,
                        sourceFormat = curr.format,
                        targetFormat = opts.format.name,
                        width = w,
                        height = h
                    )

                    Toast.makeText(getApplication(), "Exported to ${exportedFile.name} ($w × $h)", Toast.LENGTH_SHORT).show()
                    onComplete(exportedFile)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(getApplication(), "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                onComplete(null)
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun shareExportedFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = when (file.extension.lowercase()) {
                    "pdf" -> "application/pdf"
                    else -> "image/*"
                }
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Exported Image"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // --- BATCH CONVERSION ---
    fun setBatchImages(imgs: List<ApolloImage>) {
        _batchSelectedImages.value = imgs
        _batchTotal.value = imgs.size
        _batchProgress.value = 0
        _batchResults.value = emptyList()
    }

    fun toggleBatchSelect(img: ApolloImage) {
        val current = _batchSelectedImages.value.toMutableList()
        if (current.any { it.id == img.id }) {
            current.removeAll { it.id == img.id }
        } else {
            current.add(img)
        }
        _batchSelectedImages.value = current
        _batchTotal.value = current.size
    }

    fun selectAllForBatch() {
        _batchSelectedImages.value = images.value
        _batchTotal.value = images.value.size
    }

    fun startBatchExport(opts: ExportOptions) {
        val selected = _batchSelectedImages.value
        if (selected.isEmpty()) return

        viewModelScope.launch {
            _batchIsRunning.value = true
            _batchProgress.value = 0
            val results = mutableListOf<File>()

            for ((index, item) in selected.withIndex()) {
                try {
                    val bmp = ImageProcessor.loadBitmap(getApplication(), item)
                    if (bmp != null) {
                        val baseName = item.name.substringBeforeLast('.')
                        val outName = "${baseName}_batch_${index + 1}"
                        val file = ImageProcessor.exportImage(
                            context = getApplication(),
                            source = bmp,
                            options = opts,
                            outputName = outName,
                            themeHex = _activeTheme.value.primaryHex
                        )
                        results.add(file)

                        val (w, h) = ImageProcessor.computeTargetDimensions(
                            bmp.width,
                            bmp.height,
                            opts.resolution,
                            opts.customWidth,
                            opts.customHeight,
                            opts.resolutionScale,
                            opts.customAspectRatio
                        )

                        repository.recordExport(
                            originalName = item.name,
                            outputName = file.name,
                            outputFile = file,
                            sourceFormat = item.format,
                            targetFormat = opts.format.name,
                            width = w,
                            height = h
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                _batchProgress.value = index + 1
            }

            _batchResults.value = results
            _batchIsRunning.value = false
            Toast.makeText(getApplication(), "Batch finished: ${results.size} exported", Toast.LENGTH_SHORT).show()
        }
    }

    // --- PDF STUDIO ---
    fun exportPdfFromImages(selected: List<ApolloImage>, pageSize: PdfExporter.PageSize) {
        if (selected.isEmpty()) return
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val bitmaps = mutableListOf<Bitmap>()
                for (item in selected) {
                    ImageProcessor.loadBitmap(getApplication(), item, maxDimension = 1920)?.let {
                        bitmaps.add(it)
                    }
                }
                if (bitmaps.isNotEmpty()) {
                    val file = PdfExporter.exportToPdf(
                        context = getApplication(),
                        bitmaps = bitmaps,
                        pageSize = pageSize
                    )
                    _lastExportedFile.value = file
                    Toast.makeText(getApplication(), "PDF Created: ${file.name}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(getApplication(), "PDF failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                _isExporting.value = false
            }
        }
    }

    // --- THEME, SCALE & SETTINGS ---
    fun setTheme(theme: ApolloTheme) {
        _activeTheme.value = theme
    }

    fun setDarkMode(dark: Boolean?) {
        _isDarkMode.value = dark
    }

    fun setUiScaleMode(mode: UiScaleMode) {
        _uiScaleMode.value = mode
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = File(getApplication<Application>().cacheDir, "exports")
            dir.deleteRecursively()
            withContext(Dispatchers.Main) {
                Toast.makeText(getApplication(), "Export cache cleared", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.historyDao.clearAll()
            Toast.makeText(getApplication(), "History cleared", Toast.LENGTH_SHORT).show()
        }
    }
}
