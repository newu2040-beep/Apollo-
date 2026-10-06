package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class UiScaleMode(val label: String, val description: String) {
    AUTO("Auto Detect", "Adapts automatically to small screens and compact displays"),
    COMPACT("Compact Mode", "High density layout tailored for small phones & one-hand use"),
    STANDARD("Standard Mode", "Balanced modern spacing and default proportions"),
    COMFORTABLE("Comfortable", "Spacious layout with larger touch targets and padding")
}

data class AppScaleConfig(
    val mode: UiScaleMode = UiScaleMode.AUTO,
    val isCompact: Boolean = false,
    val isComfortable: Boolean = false,
    val horizontalPadding: Dp = 20.dp,
    val screenSpacing: Dp = 16.dp,
    val cardCornerRadius: Dp = 20.dp,
    val cardSpacing: Dp = 14.dp,
    val bannerHeight: Dp = 175.dp,
    val iconSize: Dp = 22.dp,
    val smallIconSize: Dp = 16.dp,
    val navHorizontalPadding: Dp = 20.dp,
    val navVerticalPadding: Dp = 10.dp,
    val gridMinSize: Dp = 130.dp,
    val buttonHeight: Dp = 52.dp,
    val textScale: Float = 1.0f,
    val titleLargeSize: TextUnit = 22.sp,
    val titleMediumSize: TextUnit = 16.sp,
    val bodyMediumSize: TextUnit = 14.sp,
    val captionSize: TextUnit = 12.sp,
    val screenWidthDp: Int = 390,
    val screenHeightDp: Int = 844
)

val LocalAppScale = compositionLocalOf { AppScaleConfig() }

@Composable
fun rememberAppScale(mode: UiScaleMode): AppScaleConfig {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    val isSmallScreen = screenWidth < 380 || screenHeight < 700

    val isCompact = when (mode) {
        UiScaleMode.AUTO -> isSmallScreen
        UiScaleMode.COMPACT -> true
        UiScaleMode.STANDARD -> false
        UiScaleMode.COMFORTABLE -> false
    }

    val isComfortable = mode == UiScaleMode.COMFORTABLE

    return remember(mode, screenWidth, screenHeight, isSmallScreen) {
        when {
            isCompact -> AppScaleConfig(
                mode = mode,
                isCompact = true,
                isComfortable = false,
                horizontalPadding = 12.dp,
                screenSpacing = 10.dp,
                cardCornerRadius = 14.dp,
                cardSpacing = 8.dp,
                bannerHeight = 125.dp,
                iconSize = 18.dp,
                smallIconSize = 14.dp,
                navHorizontalPadding = 12.dp,
                navVerticalPadding = 6.dp,
                gridMinSize = 95.dp,
                buttonHeight = 44.dp,
                textScale = 0.90f,
                titleLargeSize = 18.sp,
                titleMediumSize = 14.sp,
                bodyMediumSize = 12.sp,
                captionSize = 10.sp,
                screenWidthDp = screenWidth,
                screenHeightDp = screenHeight
            )
            isComfortable -> AppScaleConfig(
                mode = mode,
                isCompact = false,
                isComfortable = true,
                horizontalPadding = 26.dp,
                screenSpacing = 20.dp,
                cardCornerRadius = 24.dp,
                cardSpacing = 18.dp,
                bannerHeight = 220.dp,
                iconSize = 26.dp,
                smallIconSize = 18.dp,
                navHorizontalPadding = 24.dp,
                navVerticalPadding = 12.dp,
                gridMinSize = 150.dp,
                buttonHeight = 56.dp,
                textScale = 1.08f,
                titleLargeSize = 24.sp,
                titleMediumSize = 18.sp,
                bodyMediumSize = 15.sp,
                captionSize = 13.sp,
                screenWidthDp = screenWidth,
                screenHeightDp = screenHeight
            )
            else -> AppScaleConfig(
                mode = mode,
                isCompact = false,
                isComfortable = false,
                horizontalPadding = 18.dp,
                screenSpacing = 14.dp,
                cardCornerRadius = 20.dp,
                cardSpacing = 14.dp,
                bannerHeight = 175.dp,
                iconSize = 22.dp,
                smallIconSize = 16.dp,
                navHorizontalPadding = 20.dp,
                navVerticalPadding = 10.dp,
                gridMinSize = 130.dp,
                buttonHeight = 52.dp,
                textScale = 1.0f,
                titleLargeSize = 22.sp,
                titleMediumSize = 16.sp,
                bodyMediumSize = 14.sp,
                captionSize = 12.sp,
                screenWidthDp = screenWidth,
                screenHeightDp = screenHeight
            )
        }
    }
}
