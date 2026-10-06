package com.example.data.model

enum class ExportFormat(val extension: String, val mimeType: String, val supportsQuality: Boolean, val lossless: Boolean) {
    JPEG("jpg", "image/jpeg", true, false),
    PNG("png", "image/png", false, true),
    WEBP("webp", "image/webp", true, false),
    TIFF("tiff", "image/tiff", false, true),
    BMP("bmp", "image/bmp", false, true),
    PDF("pdf", "application/pdf", true, false)
}

enum class ResolutionPreset(val label: String, val targetWidth: Int?, val targetHeight: Int?) {
    ORIGINAL("Original", null, null),
    HD_720P("720p HD", 1280, 720),
    FHD_1080P("1080p FHD", 1920, 1080),
    QHD_2K("2K QHD", 2560, 1440),
    UHD_4K("4K UHD", 3840, 2160),
    UHD_5K("5K Retina", 5120, 2880),
    UHD_8K("8K Ultra", 7680, 4320),
    CUSTOM("Custom Pixels", null, null)
}

enum class MetadataPolicy(val label: String, val description: String) {
    KEEP_ALL("Keep All", "Preserve all camera, lens, and location data"),
    REMOVE_ALL("Remove All", "Strip all personal, device, and GPS metadata"),
    REMOVE_GPS_ONLY("Remove GPS", "Retain camera settings but remove coordinates"),
    REMOVE_CAMERA_INFO("Remove Camera", "Remove make, model, lens, and serial numbers")
}

data class ExportOptions(
    val format: ExportFormat = ExportFormat.JPEG,
    val resolution: ResolutionPreset = ResolutionPreset.ORIGINAL,
    val customWidth: Int? = null,
    val customHeight: Int? = null,
    val resolutionScale: Float = 1.0f, // 0.25x to 4.0x
    val lockAspectRatio: Boolean = true,
    val quality: Int = 90, // 1 to 100
    val cropRatio: CropAspectRatio = CropAspectRatio.ORIGINAL,
    val customAspectRatio: CustomAspectRatio? = null,
    val fitMode: CanvasFitMode = CanvasFitMode.SMART_CROP,
    val targetDpi: Int = 300,
    val metadataPolicy: MetadataPolicy = MetadataPolicy.KEEP_ALL,
    val customFileName: String? = null
)
