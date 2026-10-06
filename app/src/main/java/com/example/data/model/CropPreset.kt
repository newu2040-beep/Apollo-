package com.example.data.model

enum class CropAspectRatio(val label: String, val ratioWidth: Float, val ratioHeight: Float) {
    ORIGINAL("Original", -1f, -1f),
    FREE("Free", 0f, 0f),
    SQUARE_1_1("1:1", 1f, 1f),
    PORTRAIT_4_5("4:5", 4f, 5f),
    PORTRAIT_9_16("9:16", 9f, 16f),
    LANDSCAPE_16_9("16:9", 16f, 9f),
    PORTRAIT_3_4("3:4", 3f, 4f),
    LANDSCAPE_4_3("4:3", 4f, 3f),
    PORTRAIT_2_3("2:3", 2f, 3f),
    LANDSCAPE_3_2("3:2", 3f, 2f),
    CINEMATIC_21_9("21:9", 21f, 9f),
    TALL_1_2("1:2", 1f, 2f),
    PRINT_5_4("5:4", 5f, 4f),
    PRINT_5_7("5:7", 5f, 7f),
    A4("A4", 1f, 1.414f),
    BANNER_3_1("3:1", 3f, 1f),
    CUSTOM("Custom", 0f, 0f);

    val isFixed: Boolean
        get() = ratioWidth > 0f && ratioHeight > 0f

    fun toCustomAspectRatio(customW: Float = 1f, customH: Float = 1f): CustomAspectRatio {
        if (this == CUSTOM) {
            return CustomAspectRatio(label = "Custom", ratioWidth = customW, ratioHeight = customH)
        }
        return CustomAspectRatio(label = label, ratioWidth = ratioWidth, ratioHeight = ratioHeight, isBuiltIn = true)
    }
}
