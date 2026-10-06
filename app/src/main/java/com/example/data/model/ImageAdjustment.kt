package com.example.data.model

data class ImageAdjustment(
    val brightness: Float = 0f,      // -100 to +100
    val contrast: Float = 0f,        // -100 to +100
    val saturation: Float = 0f,      // -100 to +100
    val temperature: Float = 0f,     // -100 to +100 (Warm / Cool)
    val highlights: Float = 0f,      // -100 to +100
    val shadows: Float = 0f,         // -100 to +100
    val sharpness: Float = 0f,       // 0 to 100
    val vignette: Float = 0f,        // 0 to 100
    val rotationDegrees: Int = 0,    // 0, 90, 180, 270
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false
) {
    val isDefault: Boolean
        get() = brightness == 0f &&
                contrast == 0f &&
                saturation == 0f &&
                temperature == 0f &&
                highlights == 0f &&
                shadows == 0f &&
                sharpness == 0f &&
                vignette == 0f &&
                rotationDegrees == 0 &&
                !flipHorizontal &&
                !flipVertical
}
