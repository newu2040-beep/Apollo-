package com.example.data.model

enum class FilterType(val displayName: String) {
    ORIGINAL("Original"),
    WARM("Warm"),
    COOL("Cool"),
    VINTAGE("Vintage"),
    BW("B&W"),
    CINEMA("Cinema"),
    PASTEL("Pastel"),
    DRAMATIC("Dramatic"),
    RETRO("Retro"),
    MONO("Mono"),
    NATURAL("Natural"),
    FADE("Fade")
}

data class FilterConfig(
    val type: FilterType = FilterType.ORIGINAL,
    val intensity: Float = 1.0f // 0.0 to 1.0
)
