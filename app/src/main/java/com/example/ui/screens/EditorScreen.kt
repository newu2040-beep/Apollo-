package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CropAspectRatio
import com.example.data.model.CustomAspectRatio
import com.example.data.model.FilterType
import com.example.ui.components.BeforeAfterSlider
import com.example.ui.components.CustomAspectRatioDialog
import com.example.ui.theme.LocalAppScale
import com.example.viewmodel.ApolloScreen
import com.example.viewmodel.ApolloViewModel
import com.example.viewmodel.EditorToolTab
import kotlin.math.roundToInt

@Composable
fun EditorScreen(
    viewModel: ApolloViewModel,
    modifier: Modifier = Modifier
) {
    val beforeBitmap by viewModel.previewBitmap.collectAsState()
    val editedPreview by viewModel.editedPreviewBitmap.collectAsState()
    val afterBitmap = editedPreview ?: beforeBitmap
    val activeTab by viewModel.activeEditorTab.collectAsState()
    val adjustment by viewModel.currentAdjustment.collectAsState()
    val filter by viewModel.currentFilter.collectAsState()
    val cropRatio by viewModel.currentCropRatio.collectAsState()
    val activeCustomRatio by viewModel.activeCustomRatio.collectAsState()
    val customRatios by viewModel.customAspectRatios.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val splitFraction by viewModel.beforeAfterSplit.collectAsState()
    val scale = LocalAppScale.current

    var showCustomRatioDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. TOP BAR: Back, "Edit", Undo, Redo, Save
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = scale.horizontalPadding, vertical = if (scale.isCompact) 6.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier.testTag("editor_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Edit Studio",
                style = MaterialTheme.typography.titleLarge,
                fontSize = scale.titleLargeSize,
                fontWeight = FontWeight.Bold
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { viewModel.undo() },
                    enabled = canUndo,
                    modifier = Modifier.testTag("editor_undo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

                IconButton(
                    onClick = { viewModel.redo() },
                    enabled = canRedo,
                    modifier = Modifier.testTag("editor_redo_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

                // Done Checkmark Pill
                Box(
                    modifier = Modifier
                        .size(if (scale.isCompact) 32.dp else 36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { viewModel.navigateTo(ApolloScreen.EXPORT) }
                        .testTag("editor_done_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done",
                        tint = Color.White,
                        modifier = Modifier.size(if (scale.isCompact) 18.dp else 20.dp)
                    )
                }
            }
        }

        // 2. CENTER: Before / After Slider Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = scale.horizontalPadding, vertical = 4.dp)
        ) {
            BeforeAfterSlider(
                beforeBitmap = beforeBitmap,
                afterBitmap = afterBitmap,
                initialSplit = splitFraction,
                onSplitChange = { viewModel.setBeforeAfterSplit(it) },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3. TOOL TABS: [Adjust] [Filters] [Crop] [Effects]
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(top = if (scale.isCompact) 6.dp else 10.dp, bottom = if (scale.isCompact) 8.dp else 14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = scale.horizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EditorTabButton(
                        title = "Adjust",
                        isSelected = activeTab == EditorToolTab.ADJUST,
                        modifier = Modifier.weight(1f)
                    ) { viewModel.setEditorTab(EditorToolTab.ADJUST) }

                    EditorTabButton(
                        title = "Filters",
                        isSelected = activeTab == EditorToolTab.FILTERS,
                        modifier = Modifier.weight(1f)
                    ) { viewModel.setEditorTab(EditorToolTab.FILTERS) }

                    EditorTabButton(
                        title = "Crop & Ratio",
                        isSelected = activeTab == EditorToolTab.CROP,
                        modifier = Modifier.weight(1.1f)
                    ) { viewModel.setEditorTab(EditorToolTab.CROP) }

                    EditorTabButton(
                        title = "Effects",
                        isSelected = activeTab == EditorToolTab.EFFECTS,
                        modifier = Modifier.weight(1f)
                    ) { viewModel.setEditorTab(EditorToolTab.EFFECTS) }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // TOOL CONTENT PANEL
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220)) + slideInVertically(animationSpec = tween(220), initialOffsetY = { 16 }))
                            .togetherWith(fadeOut(animationSpec = tween(140)))
                    },
                    label = "editor_tool_panel"
                ) { tab ->
                    when (tab) {
                        EditorToolTab.ADJUST -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = scale.horizontalPadding)
                            ) {
                                AdjustmentSliderRow(
                                    title = "Brightness",
                                    icon = Icons.Default.WbSunny,
                                    value = adjustment.brightness,
                                    valueRange = -100f..100f,
                                    onValueChange = { v -> viewModel.updateAdjustmentLive { it.copy(brightness = v) } },
                                    onValueChangeFinished = { viewModel.pushUndoState() }
                                )

                                AdjustmentSliderRow(
                                    title = "Contrast",
                                    icon = Icons.Default.Contrast,
                                    value = adjustment.contrast,
                                    valueRange = -100f..100f,
                                    onValueChange = { v -> viewModel.updateAdjustmentLive { it.copy(contrast = v) } },
                                    onValueChangeFinished = { viewModel.pushUndoState() }
                                )

                                AdjustmentSliderRow(
                                    title = "Saturation",
                                    icon = Icons.Default.Gradient,
                                    value = adjustment.saturation,
                                    valueRange = -100f..100f,
                                    onValueChange = { v -> viewModel.updateAdjustmentLive { it.copy(saturation = v) } },
                                    onValueChangeFinished = { viewModel.pushUndoState() }
                                )

                                AdjustmentSliderRow(
                                    title = "Temperature",
                                    icon = Icons.Default.Thermostat,
                                    value = adjustment.temperature,
                                    valueRange = -100f..100f,
                                    onValueChange = { v -> viewModel.updateAdjustmentLive { it.copy(temperature = v) } },
                                    onValueChangeFinished = { viewModel.pushUndoState() }
                                )
                            }
                        }

                        EditorToolTab.FILTERS -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = scale.horizontalPadding)
                            ) {
                                AdjustmentSliderRow(
                                    title = "Filter Intensity",
                                    icon = Icons.Default.Brightness6,
                                    value = filter.intensity * 100f,
                                    valueRange = 0f..100f,
                                    onValueChange = { v ->
                                        viewModel.setFilter(filter.type, v / 100f)
                                    }
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(FilterType.values()) { fType ->
                                        FilterMiniThumbnail(
                                            filterType = fType,
                                            isSelected = filter.type == fType,
                                            onClick = { viewModel.setFilter(fType, filter.intensity) }
                                        )
                                    }
                                }
                            }
                        }

                        EditorToolTab.CROP -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = scale.horizontalPadding)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Aspect Ratios (${if (activeCustomRatio != null) activeCustomRatio!!.label else cropRatio.label})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Text(
                                        text = "+ Custom Ratio",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { showCustomRatioDialog = true }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(customRatios) { ratio ->
                                        val isSelected = (activeCustomRatio?.id == ratio.id) ||
                                                (activeCustomRatio == null && cropRatio.toCustomAspectRatio().label == ratio.label)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                if (ratio.isBuiltIn) {
                                                    val matchEnum = CropAspectRatio.values().find { it.label == ratio.label }
                                                    if (matchEnum != null) {
                                                        viewModel.setCropRatio(matchEnum)
                                                    } else {
                                                        viewModel.setCustomCropRatio(ratio)
                                                    }
                                                } else {
                                                    viewModel.setCustomCropRatio(ratio)
                                                }
                                            },
                                            label = { Text(ratio.label, fontSize = 11.sp) },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TransformButton(
                                        label = "Rotate 90°",
                                        icon = Icons.Default.RotateRight,
                                        modifier = Modifier.weight(1f)
                                    ) { viewModel.rotate90() }

                                    TransformButton(
                                        label = "Flip H",
                                        icon = Icons.Default.Flip,
                                        modifier = Modifier.weight(1f)
                                    ) { viewModel.flipHorizontal() }

                                    TransformButton(
                                        label = "Flip V",
                                        icon = Icons.Default.Flip,
                                        modifier = Modifier.weight(1f)
                                    ) { viewModel.flipVertical() }
                                }
                            }
                        }

                        EditorToolTab.EFFECTS -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = scale.horizontalPadding)
                            ) {
                                AdjustmentSliderRow(
                                    title = "Vignette",
                                    icon = Icons.Default.InvertColors,
                                    value = adjustment.vignette,
                                    valueRange = 0f..100f,
                                    onValueChange = { v -> viewModel.updateAdjustment { it.copy(vignette = v) } }
                                )

                                AdjustmentSliderRow(
                                    title = "Sharpness",
                                    icon = Icons.Default.FilterVintage,
                                    value = adjustment.sharpness,
                                    valueRange = 0f..100f,
                                    onValueChange = { v -> viewModel.updateAdjustment { it.copy(sharpness = v) } }
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { viewModel.resetEditorAdjustments() }
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestartAlt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Reset All Adjustments",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomRatioDialog) {
        CustomAspectRatioDialog(
            initialWidth = 16f,
            initialHeight = 9f,
            onDismiss = { showCustomRatioDialog = false },
            onSaveRatio = { label, rw, rh ->
                viewModel.addCustomAspectRatio(label, rw, rh)
            }
        )
    }
}

@Composable
private fun EditorTabButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AdjustmentSliderRow(
    title: String,
    icon: ImageVector,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(76.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value.roundToInt().toString(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(28.dp)
        )
    }
}

@Composable
private fun FilterMiniThumbnail(
    filterType: FilterType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
            modifier = Modifier.size(54.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = filterType.name.take(3),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = filterType.displayName,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TransformButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
