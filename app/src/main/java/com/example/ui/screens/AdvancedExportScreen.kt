package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.CanvasFitMode
import com.example.data.model.CustomAspectRatio
import com.example.data.model.ExportFormat
import com.example.data.model.MetadataPolicy
import com.example.data.model.ResolutionPreset
import com.example.data.processing.ImageProcessor
import com.example.ui.components.CustomAspectRatioDialog
import com.example.ui.theme.LocalAppScale
import com.example.viewmodel.ApolloViewModel

@Composable
fun AdvancedExportScreen(
    viewModel: ApolloViewModel,
    modifier: Modifier = Modifier
) {
    val selectedImageState by viewModel.selectedImage.collectAsState()
    val options by viewModel.exportOptions.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val lastExportedFile by viewModel.lastExportedFile.collectAsState()
    val customRatios by viewModel.customAspectRatios.collectAsState()
    val scale = LocalAppScale.current
    val context = LocalContext.current

    val selectedImage = selectedImageState ?: return

    var formatDropdownExpanded by remember { mutableStateOf(false) }
    var metadataDropdownExpanded by remember { mutableStateOf(false) }
    var fitModeDropdownExpanded by remember { mutableStateOf(false) }
    var showCustomRatioDialog by remember { mutableStateOf(false) }

    var widthInput by remember { mutableStateOf((options.customWidth ?: selectedImage.width).toString()) }
    var heightInput by remember { mutableStateOf((options.customHeight ?: selectedImage.height).toString()) }

    val (targetW, targetH) = ImageProcessor.computeTargetDimensions(
        currentW = selectedImage.width,
        currentH = selectedImage.height,
        preset = options.resolution,
        customW = options.customWidth ?: widthInput.toIntOrNull(),
        customH = options.customHeight ?: heightInput.toIntOrNull(),
        scaleMultiplier = options.resolutionScale,
        customRatio = options.customAspectRatio
    )

    val estimatedBytes = ImageProcessor.estimateOutputSize(
        width = targetW,
        height = targetH,
        format = options.format,
        quality = options.quality
    )

    val estimatedMbFormatted = String.format(java.util.Locale.US, "%.1f MB", estimatedBytes / 1_048_576.0)
    val isUpscaled = targetW > selectedImage.width || targetH > selectedImage.height

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = scale.horizontalPadding, vertical = if (scale.isCompact) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier.testTag("export_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Advanced Export",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = scale.titleLargeSize,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Custom Resolution & Aspect Ratios",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = scale.captionSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = scale.horizontalPadding, vertical = if (scale.isCompact) 6.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(scale.cardSpacing)
        ) {
            // 1. LIVE IMAGE BANNER & FRAME PREVIEW
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (scale.isCompact) 140.dp else 190.dp)
                        .clip(RoundedCornerShape(scale.cardCornerRadius))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    val editedBmp = viewModel.editedPreviewBitmap.value ?: viewModel.previewBitmap.value
                    if (editedBmp != null && !editedBmp.isRecycled) {
                        Image(
                            bitmap = editedBmp.asImageBitmap(),
                            contentDescription = selectedImage.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (selectedImage.drawableResId != null) {
                        Image(
                            painter = painterResource(id = selectedImage.drawableResId),
                            contentDescription = selectedImage.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(selectedImage.uri).build(),
                            contentDescription = selectedImage.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Live Resolution Overlay Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                    ) {
                        val targetMp = String.format(java.util.Locale.US, "%.1f MP", (targetW * targetH) / 1_000_000.0)
                        Text(
                            text = "$targetW × $targetH • $targetMp • ${options.format.name}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 2. ASPECT RATIO SELECTOR (BUILT-IN + CUSTOM)
            item {
                Surface(
                    shape = RoundedCornerShape(scale.cardCornerRadius),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(if (scale.isCompact) 12.dp else 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = Icons.Default.AspectRatio,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(scale.iconSize)
                                )
                                Column {
                                    Text(text = "Aspect Ratio", fontSize = scale.captionSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = options.customAspectRatio?.label ?: "Original (${selectedImage.width}:${selectedImage.height})",
                                        fontSize = scale.titleMediumSize,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Button(
                                onClick = { showCustomRatioDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Custom", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Ratio Chips Scroll
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(customRatios) { ratio ->
                                val isSelected = options.customAspectRatio?.id == ratio.id || (options.customAspectRatio == null && ratio.id == "orig")
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.setExportAspectRatio(ratio)
                                        if (ratio.isFixed && options.lockAspectRatio) {
                                            val w = widthInput.toIntOrNull() ?: selectedImage.width
                                            val newH = (w / (ratio.ratioWidth / ratio.ratioHeight)).toInt().coerceAtLeast(16)
                                            heightInput = newH.toString()
                                            viewModel.setExportCustomDimensions(w, newH, true)
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
                    }
                }
            }

            // 3. RESOLUTION & MANUAL PIXEL DIMENSIONS
            item {
                Surface(
                    shape = RoundedCornerShape(scale.cardCornerRadius),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(if (scale.isCompact) 12.dp else 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Crop,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(scale.iconSize)
                                )
                                Column {
                                    Text(text = "Resolution Dimensions", fontSize = scale.captionSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    val targetMp = String.format(java.util.Locale.US, "%.1f MP", (targetW * targetH) / 1_000_000.0)
                                    Text(
                                        text = "$targetW × $targetH ($targetMp)",
                                        fontSize = scale.titleMediumSize,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (isUpscaled) {
                                Surface(
                                    color = Color(0xFFFF9800).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Upscaled",
                                        color = Color(0xFFE65100),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Resolution Presets
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf(
                                ResolutionPreset.ORIGINAL,
                                ResolutionPreset.HD_720P,
                                ResolutionPreset.FHD_1080P,
                                ResolutionPreset.QHD_2K,
                                ResolutionPreset.UHD_4K,
                                ResolutionPreset.UHD_8K,
                                ResolutionPreset.CUSTOM
                            )) { res ->
                                FilterChip(
                                    selected = options.resolution == res,
                                    onClick = {
                                        viewModel.setExportResolutionPreset(res)
                                        if (res != ResolutionPreset.CUSTOM) {
                                            val (w, h) = ImageProcessor.computeTargetDimensions(
                                                selectedImage.width,
                                                selectedImage.height,
                                                res,
                                                scaleMultiplier = options.resolutionScale,
                                                customRatio = options.customAspectRatio
                                            )
                                            widthInput = w.toString()
                                            heightInput = h.toString()
                                        }
                                    },
                                    label = { Text(res.label, fontSize = 11.sp, fontWeight = if (options.resolution == res) FontWeight.Bold else FontWeight.Normal) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Manual Pixel Inputs with Aspect Ratio Lock
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = widthInput,
                                onValueChange = {
                                    val digits = it.filter { c -> c.isDigit() }
                                    widthInput = digits
                                    val w = digits.toIntOrNull()
                                    if (w != null && options.lockAspectRatio) {
                                        val activeRatio = options.customAspectRatio?.ratioValue
                                            ?: (selectedImage.width.toFloat() / selectedImage.height.toFloat())
                                        val h = (w / activeRatio).toInt().coerceAtLeast(16)
                                        heightInput = h.toString()
                                        viewModel.setExportCustomDimensions(w, h, true)
                                    } else {
                                        viewModel.setExportCustomDimensions(w, heightInput.toIntOrNull(), options.lockAspectRatio)
                                    }
                                },
                                label = { Text("Width px", fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Lock Toggle Button
                            IconButton(
                                onClick = {
                                    val newLock = !options.lockAspectRatio
                                    viewModel.updateExportOptions { it.copy(lockAspectRatio = newLock) }
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = if (options.lockAspectRatio) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Lock Ratio",
                                    tint = if (options.lockAspectRatio) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Swap W / H
                            IconButton(
                                onClick = {
                                    val temp = widthInput
                                    widthInput = heightInput
                                    heightInput = temp
                                    viewModel.setExportCustomDimensions(widthInput.toIntOrNull(), heightInput.toIntOrNull(), options.lockAspectRatio)
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Swap Width and Height",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            OutlinedTextField(
                                value = heightInput,
                                onValueChange = {
                                    val digits = it.filter { c -> c.isDigit() }
                                    heightInput = digits
                                    val h = digits.toIntOrNull()
                                    if (h != null && options.lockAspectRatio) {
                                        val activeRatio = options.customAspectRatio?.ratioValue
                                            ?: (selectedImage.width.toFloat() / selectedImage.height.toFloat())
                                        val w = (h * activeRatio).toInt().coerceAtLeast(16)
                                        widthInput = w.toString()
                                        viewModel.setExportCustomDimensions(w, h, true)
                                    } else {
                                        viewModel.setExportCustomDimensions(widthInput.toIntOrNull(), h, options.lockAspectRatio)
                                    }
                                },
                                label = { Text("Height px", fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Scale Multiplier Selector (0.5x, 1x, 2x, 4x)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Scale Multiplier", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${options.resolutionScale}x", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            items(listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.5f, 2.0f, 3.0f, 4.0f)) { s ->
                                FilterChip(
                                    selected = options.resolutionScale == s,
                                    onClick = { viewModel.setExportScale(s) },
                                    label = { Text("${s}x", fontSize = 10.sp) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 4. CANVAS FIT & FILL MODE
            item {
                Surface(
                    shape = RoundedCornerShape(scale.cardCornerRadius),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(if (scale.isCompact) 12.dp else 16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { fitModeDropdownExpanded = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Canvas Fit Mode", fontSize = scale.captionSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = options.fitMode.label,
                                    fontSize = scale.titleMediumSize,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = options.fitMode.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = fitModeDropdownExpanded,
                            onDismissRequest = { fitModeDropdownExpanded = false },
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            CanvasFitMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(mode.label, fontWeight = FontWeight.SemiBold)
                                            Text(mode.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    onClick = {
                                        viewModel.updateExportOptions { it.copy(fitMode = mode) }
                                        fitModeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 5. FORMAT CARD
            item {
                Surface(
                    shape = RoundedCornerShape(scale.cardCornerRadius),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(if (scale.isCompact) 12.dp else 16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { formatDropdownExpanded = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Photo,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(scale.iconSize)
                                )
                                Column {
                                    Text(text = "Format", fontSize = scale.captionSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = options.format.name,
                                        fontSize = scale.titleMediumSize,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = formatDropdownExpanded,
                            onDismissRequest = { formatDropdownExpanded = false },
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            ExportFormat.values().forEach { fmt ->
                                DropdownMenuItem(
                                    text = { Text("${fmt.name} (.${fmt.extension})") },
                                    onClick = {
                                        viewModel.updateExportOptions { it.copy(format = fmt) }
                                        formatDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 6. QUALITY SLIDER CARD
            item {
                Surface(
                    shape = RoundedCornerShape(scale.cardCornerRadius),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(if (scale.isCompact) 12.dp else 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(scale.iconSize)
                                )
                                Column {
                                    Text(text = "Quality", fontSize = scale.captionSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${options.quality}%",
                                        fontSize = scale.titleMediumSize,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            if (options.format.lossless) {
                                Text(
                                    text = "Lossless",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (options.format.supportsQuality) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(
                                value = options.quality.toFloat(),
                                onValueChange = { viewModel.updateExportOptions { opt -> opt.copy(quality = it.toInt()) } },
                                valueRange = 1f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            // 7. METADATA POLICY CARD
            item {
                Surface(
                    shape = RoundedCornerShape(scale.cardCornerRadius),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(if (scale.isCompact) 12.dp else 16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { metadataDropdownExpanded = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(scale.iconSize)
                                )
                                Column {
                                    Text(text = "Metadata & Privacy", fontSize = scale.captionSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = options.metadataPolicy.label,
                                        fontSize = scale.titleMediumSize,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = metadataDropdownExpanded,
                            onDismissRequest = { metadataDropdownExpanded = false },
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            MetadataPolicy.values().forEach { pol ->
                                DropdownMenuItem(
                                    text = { Text("${pol.label} - ${pol.description}") },
                                    onClick = {
                                        viewModel.updateExportOptions { it.copy(metadataPolicy = pol) }
                                        metadataDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 8. ESTIMATED SIZE & METADATA INSPECT
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Estimated File Size",
                            fontSize = scale.captionSize,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "~ $estimatedMbFormatted",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { viewModel.inspectMetadata(selectedImage) }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Inspect original info",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // BOTTOM EXPORT BUTTON
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = scale.horizontalPadding, vertical = if (scale.isCompact) 8.dp else 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.exportCurrentImage()
                    },
                    enabled = !isExporting,
                    modifier = Modifier
                        .weight(1f)
                        .height(scale.buttonHeight)
                        .testTag("export_primary_button"),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exporting...", fontSize = scale.bodyMediumSize, fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(imageVector = Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(scale.iconSize))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export ($targetW × $targetH)", fontSize = scale.bodyMediumSize, fontWeight = FontWeight.SemiBold)
                    }
                }

                val exportedFile = lastExportedFile
                if (exportedFile != null) {
                    Button(
                        onClick = {
                            viewModel.shareExportedFile(context, exportedFile)
                        },
                        modifier = Modifier
                            .height(scale.buttonHeight)
                            .testTag("share_exported_button"),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
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
