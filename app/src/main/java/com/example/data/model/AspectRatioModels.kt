package com.example.data.model

import java.util.UUID

enum class CanvasFitMode(val label: String, val description: String) {
    SMART_CROP("Smart Crop", "Center crop to fill exact aspect ratio"),
    FIT_PAD_BLUR("Fit + Blur Canvas", "Fit full image, blur background to fill aspect ratio"),
    FIT_PAD_BLACK("Fit + Black Canvas", "Fit full image with clean black border bars"),
    FIT_PAD_WHITE("Fit + White Canvas", "Fit full image with clean white border bars"),
    FIT_PAD_THEME("Fit + Theme Canvas", "Fit full image with current theme accent border"),
    FIT_PAD_TRANSPARENT("Fit + Transparent", "Fit full image with alpha transparency (PNG/WEBP)"),
    STRETCH("Exact Stretch", "Stretch image to fill exact width and height")
}

data class CustomAspectRatio(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val ratioWidth: Float,
    val ratioHeight: Float,
    val isBuiltIn: Boolean = false,
    val category: String = "Custom"
) {
    val ratioValue: Float
        get() = if (ratioHeight > 0f) ratioWidth / ratioHeight else 1f

    val isFixed: Boolean
        get() = ratioWidth > 0f && ratioHeight > 0f

    val formattedRatio: String
        get() {
            if (ratioWidth <= 0f || ratioHeight <= 0f) return label
            return if (ratioWidth % 1f == 0f && ratioHeight % 1f == 0f) {
                "${ratioWidth.toInt()}:${ratioHeight.toInt()}"
            } else {
                String.format(java.util.Locale.US, "%.2f:%.2f", ratioWidth, ratioHeight)
            }
        }

    companion object {
        val ORIGINAL = CustomAspectRatio("orig", "Original", -1f, -1f, true, "Standard")
        val FREE = CustomAspectRatio("free", "Free Form", 0f, 0f, true, "Standard")
        val SQUARE_1_1 = CustomAspectRatio("1_1", "1:1 Square", 1f, 1f, true, "Social")
        val PORTRAIT_4_5 = CustomAspectRatio("4_5", "4:5 IG Feed", 4f, 5f, true, "Social")
        val PORTRAIT_9_16 = CustomAspectRatio("9_16", "9:16 Story/Reels", 9f, 16f, true, "Social")
        val LANDSCAPE_16_9 = CustomAspectRatio("16_9", "16:9 YouTube/TV", 16f, 9f, true, "Video")
        val PORTRAIT_3_4 = CustomAspectRatio("3_4", "3:4 Portrait", 3f, 4f, true, "Standard")
        val LANDSCAPE_4_3 = CustomAspectRatio("4_3", "4:3 Classic TV", 4f, 3f, true, "Standard")
        val PORTRAIT_2_3 = CustomAspectRatio("2_3", "2:3 35mm Portrait", 2f, 3f, true, "Photography")
        val LANDSCAPE_3_2 = CustomAspectRatio("3_2", "3:2 35mm Classic", 3f, 2f, true, "Photography")
        val CINEMATIC_21_9 = CustomAspectRatio("21_9", "21:9 Ultrawide", 21f, 9f, true, "Cinema")
        val TALL_1_2 = CustomAspectRatio("1_2", "1:2 Tall Banner", 1f, 2f, true, "Social")
        val PRINT_5_4 = CustomAspectRatio("5_4", "5:4 Large Format", 5f, 4f, true, "Print")
        val PRINT_5_7 = CustomAspectRatio("5_7", "5:7 Photo Print", 5f, 7f, true, "Print")
        val DOCUMENT_A4 = CustomAspectRatio("a4", "A4 Document", 1f, 1.414f, true, "Print")
        val BANNER_3_1 = CustomAspectRatio("3_1", "3:1 Header", 3f, 1f, true, "Web")

        val DEFAULT_BUILT_IN = listOf(
            ORIGINAL,
            FREE,
            SQUARE_1_1,
            PORTRAIT_4_5,
            PORTRAIT_9_16,
            LANDSCAPE_16_9,
            PORTRAIT_3_4,
            LANDSCAPE_4_3,
            LANDSCAPE_3_2,
            PORTRAIT_2_3,
            CINEMATIC_21_9,
            TALL_1_2,
            PRINT_5_4,
            PRINT_5_7,
            DOCUMENT_A4,
            BANNER_3_1
        )
    }
}
