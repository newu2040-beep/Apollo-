package com.example.data.model

import android.net.Uri

data class ApolloImage(
    val id: String,
    val uri: Uri,
    val name: String,
    val format: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val dateModified: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val album: String = "Imported",
    val drawableResId: Int? = null
) {
    val megapixels: Double
        get() = (width * height).toDouble() / 1_000_000.0

    val formattedSize: String
        get() {
            return when {
                sizeBytes >= 1_048_576 -> String.format(java.util.Locale.US, "%.1f MB", sizeBytes / 1_048_576.0)
                sizeBytes >= 1024 -> String.format(java.util.Locale.US, "%.0f KB", sizeBytes / 1024.0)
                else -> "$sizeBytes B"
            }
        }

    val dimensionsText: String
        get() = "$width × $height"
}

enum class ApolloTheme(
    val displayName: String,
    val primaryHex: Long,
    val backgroundHex: Long,
    val containerHex: Long,
    val darkBackgroundHex: Long = 0xFF0E1116,
    val darkSurfaceHex: Long = 0xFF161B22
) {
    SKY("Sky Blue", 0xFF1868F8, 0xFFF4F7FC, 0xFFE8F0FE, 0xFF0B101B, 0xFF121826),
    LAVENDER("Lavender", 0xFF8B5CF6, 0xFFF7F4FD, 0xFFEFE8FD, 0xFF100D1B, 0xFF181427),
    MINT("Mint Fresh", 0xFF10B981, 0xFFF2FAF6, 0xFFE1F7EC, 0xFF0A130F, 0xFF111F18),
    PEACH("Peach Sunset", 0xFFF97316, 0xFFFEF6F2, 0xFFFDEEE5, 0xFF140E0A, 0xFF201611),
    BUTTER("Buttercream", 0xFFEAB308, 0xFFFEFDF0, 0xFFFCF9DC, 0xFF131109, 0xFF1E1B10),
    ROSE("Rose Quartz", 0xFFEC4899, 0xFFFDF2F7, 0xFFFCE4EE, 0xFF140A10, 0xFF20121C),
    SAGE("Sage Green", 0xFF059669, 0xFFF3F7F4, 0xFFE4EFE7, 0xFF0A120D, 0xFF121C16),
    CHERRY_SAKURA("Sakura Pink", 0xFFF43F5E, 0xFFFFF1F4, 0xFFFFE2E8, 0xFF150A0E, 0xFF211118),
    PERIWINKLE("Periwinkle", 0xFF6366F1, 0xFFF3F4FE, 0xFFE4E6FD, 0xFF0D0E1C, 0xFF15162B),
    LEMON_CHIFFON("Lemon Chiffon", 0xFFD97706, 0xFFFEFCE8, 0xFFFEF9C3, 0xFF121008, 0xFF1D190E),
    MATCHA("Matcha Tea", 0xFF65A30D, 0xFFF6FCEB, 0xFFECF8D6, 0xFF0D1309, 0xFF151F10),
    AQUA_SKY("Aqua Breeze", 0xFF06B6D4, 0xFFECFEFF, 0xFFCEFAFE, 0xFF071317, 0xFF0F1D22)
}
