package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.ApolloImage
import com.example.data.model.ApolloTheme
import com.example.data.model.CanvasFitMode
import com.example.data.model.CustomAspectRatio
import com.example.data.model.ResolutionPreset
import com.example.ui.components.CustomResolutionImportBottomSheet
import com.example.ui.theme.LocalAppScale
import com.example.ui.theme.PastelBatchBg
import com.example.ui.theme.PastelBatchIcon
import com.example.ui.theme.PastelCompressBg
import com.example.ui.theme.PastelCompressIcon
import com.example.ui.theme.PastelConvertBg
import com.example.ui.theme.PastelConvertIcon
import com.example.ui.theme.PastelEditBg
import com.example.ui.theme.PastelEditIcon
import com.example.ui.theme.PastelPdfBg
import com.example.ui.theme.PastelPdfIcon
import com.example.ui.theme.PastelResizeBg
import com.example.ui.theme.PastelResizeIcon
import com.example.ui.theme.UiScaleMode
import com.example.viewmodel.ApolloScreen
import com.example.viewmodel.ApolloViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PicturesScreen(
    viewModel: ApolloViewModel,
    modifier: Modifier = Modifier
) {
    val images by viewModel.images.collectAsState()
    val activeTheme by viewModel.activeTheme.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val uiScaleMode by viewModel.uiScaleMode.collectAsState()
    val customRatios by viewModel.customAspectRatios.collectAsState()
    val scale = LocalAppScale.current

    var filterMode by remember { mutableStateOf("All Photos") }
    var showImportSheet by remember { mutableStateOf(false) }
    var showCustomResolutionImportSheet by remember { mutableStateOf(false) }
    var pendingCustomImportUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Multi-photo visual picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importUris(uris)
        }
    }

    // Custom Resolution Photo Picker
    val customResolutionPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            pendingCustomImportUris = uris
            showCustomResolutionImportSheet = true
        }
    }

    // Storage Access Framework Document/File Picker
    val safFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importUris(uris)
        }
    }

    // Permission Launcher for Gallery Access
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.any { it }
        if (granted) {
            viewModel.scanGallery()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
            )
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
        viewModel.scanGallery()
    }

    val filteredImages = when (filterMode) {
        "Recent" -> images.take(5)
        "Favorites" -> images.filter { it.isFavorite }
        "Gallery" -> images.filter { it.album == "Gallery" }
        "Imported" -> images.filter { it.album == "Imported" }
        else -> images
    }

    fun ensureImageSelected(): Boolean {
        if (viewModel.selectedImage.value == null) {
            val first = images.firstOrNull()
            if (first != null) {
                viewModel.selectImage(first, navigate = false)
                return true
            } else {
                showImportSheet = true
                return false
            }
        }
        return true
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        // 1. TOP HEADER (Planet Apollo logo, Compact Mode toggle pill, actions)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = scale.horizontalPadding, vertical = if (scale.isCompact) 8.dp else 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (scale.isCompact) 32.dp else 40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_apollo_circle_fg),
                            contentDescription = "APOLLO Logo",
                            modifier = Modifier.size(if (scale.isCompact) 26.dp else 32.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "APOLLO",
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = scale.titleLargeSize,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = activeTheme.displayName,
                            fontSize = scale.captionSize,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Compact Mode Quick Switcher Pill
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (scale.isCompact) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, if (scale.isCompact) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .clickable {
                                val nextMode = when (uiScaleMode) {
                                    UiScaleMode.AUTO -> UiScaleMode.COMPACT
                                    UiScaleMode.COMPACT -> UiScaleMode.STANDARD
                                    UiScaleMode.STANDARD -> UiScaleMode.AUTO
                                    UiScaleMode.COMFORTABLE -> UiScaleMode.AUTO
                                }
                                viewModel.setUiScaleMode(nextMode)
                            }
                            .testTag("compact_mode_toggle_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = "Layout scale",
                                tint = if (scale.isCompact) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (scale.isCompact) "Compact" else if (uiScaleMode == UiScaleMode.AUTO) "Auto" else "Standard",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (scale.isCompact) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
                            } else {
                                permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                            }
                            viewModel.scanGallery()
                        },
                        modifier = Modifier.testTag("scan_gallery_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Scan Gallery",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(scale.iconSize)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo(ApolloScreen.MENU) },
                        modifier = Modifier.testTag("settings_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(scale.iconSize)
                        )
                    }
                }
            }
        }

        // 2. REAL-TIME PASTEL THEME SELECTOR BAR
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (scale.isCompact) 6.dp else 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = scale.horizontalPadding, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(scale.smallIconSize)
                        )
                        Text(
                            text = "Pastel Color Themes",
                            fontSize = scale.captionSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Real-time",
                        fontSize = scale.captionSize,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = scale.horizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(ApolloTheme.values()) { theme ->
                        val isSelected = activeTheme == theme
                        val themeColor = Color(theme.primaryHex)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            tonalElevation = if (isSelected) 4.dp else 1.dp,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .clickable { viewModel.setTheme(theme) }
                                .testTag("theme_chip_${theme.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(themeColor)
                                )
                                Text(
                                    text = theme.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. HERO HEROIC CANVAS BANNER
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = scale.horizontalPadding, vertical = 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(scale.cardCornerRadius),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(scale.bannerHeight)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.apollo_hero_banner),
                            contentDescription = "APOLLO Studio Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Dark gradient scrim
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(if (scale.isCompact) 12.dp else 16.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "PREMIUM IMAGE WORKSPACE",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Custom Resolution & Frame Studio",
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = scale.titleMediumSize,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Export with custom aspect ratios, pixels, 4K/8K presets & lossless quality",
                                fontSize = scale.captionSize,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // 4. ACTION BUTTONS: [Import Photos] & [Custom Resolution Import]
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = scale.horizontalPadding, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showImportSheet = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(scale.buttonHeight)
                        .testTag("import_photos_button"),
                    shape = RoundedCornerShape(scale.buttonHeight / 2),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(scale.iconSize)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Import Photos",
                        fontSize = scale.bodyMediumSize,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = {
                        customResolutionPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .weight(1.1f)
                        .height(scale.buttonHeight)
                        .testTag("custom_resolution_import_button"),
                    shape = RoundedCornerShape(scale.buttonHeight / 2),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = null,
                        modifier = Modifier.size(scale.iconSize)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Custom Ratio Import",
                        fontSize = scale.bodyMediumSize,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 5. QUICK MASTER PRESETS (4K UHD, 8K Super-Res)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = scale.horizontalPadding, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(scale.cardCornerRadius),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (ensureImageSelected()) {
                                viewModel.updateExportOptions {
                                    it.copy(
                                        resolution = ResolutionPreset.UHD_4K,
                                        quality = 95
                                    )
                                }
                                viewModel.navigateTo(ApolloScreen.EXPORT)
                            }
                        }
                        .testTag("quick_4k_preset")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = if (scale.isCompact) 8.dp else 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HighQuality,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(scale.iconSize)
                        )
                        Column {
                            Text("4K Ultra-HD", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Text("3840 × 2160 • Master", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(scale.cardCornerRadius),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (ensureImageSelected()) {
                                viewModel.updateExportOptions {
                                    it.copy(
                                        resolution = ResolutionPreset.UHD_8K,
                                        quality = 98
                                    )
                                }
                                viewModel.navigateTo(ApolloScreen.EXPORT)
                            }
                        }
                        .testTag("quick_8k_preset")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = if (scale.isCompact) 8.dp else 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(scale.iconSize)
                        )
                        Column {
                            Text("8K Super-Res", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Text("7680 × 4320 • Studio", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // 6. QUICK ACTION CARDS (Convert, Edit, Resize, Compress, PDF, Batch)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = scale.horizontalPadding, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionCard(
                        title = "Convert",
                        icon = Icons.Default.SwapHoriz,
                        iconColor = PastelConvertIcon,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_convert_action"
                    ) {
                        if (ensureImageSelected()) {
                            viewModel.navigateTo(ApolloScreen.EXPORT)
                        }
                    }

                    QuickActionCard(
                        title = "Edit",
                        icon = Icons.Default.Tune,
                        iconColor = PastelEditIcon,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_edit_action"
                    ) {
                        if (ensureImageSelected()) {
                            viewModel.navigateTo(ApolloScreen.EDITOR)
                        }
                    }

                    QuickActionCard(
                        title = "Resize",
                        icon = Icons.Default.AspectRatio,
                        iconColor = PastelResizeIcon,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_resize_action"
                    ) {
                        if (ensureImageSelected()) {
                            viewModel.navigateTo(ApolloScreen.EXPORT)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionCard(
                        title = "Compress",
                        icon = Icons.Default.Compress,
                        iconColor = PastelCompressIcon,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_compress_action"
                    ) {
                        if (ensureImageSelected()) {
                            viewModel.navigateTo(ApolloScreen.EXPORT)
                        }
                    }

                    QuickActionCard(
                        title = "PDF",
                        icon = Icons.Default.PictureAsPdf,
                        iconColor = PastelPdfIcon,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_pdf_action"
                    ) {
                        viewModel.navigateTo(ApolloScreen.PDF_STUDIO)
                    }

                    QuickActionCard(
                        title = "Batch",
                        icon = Icons.Default.Layers,
                        iconColor = PastelBatchIcon,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_batch_action"
                    ) {
                        viewModel.setBatchImages(images)
                        viewModel.navigateTo(ApolloScreen.BATCH)
                    }
                }
            }
        }

        // 7. RECENT SECTION
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = scale.horizontalPadding, end = scale.horizontalPadding, top = 10.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Photos",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = scale.titleMediumSize,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { filterMode = "Recent" }) {
                    Text("View All (${images.size})", fontSize = scale.captionSize)
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = scale.horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(scale.cardSpacing)
            ) {
                items(images.take(6)) { img ->
                    RecentImageCard(image = img) {
                        viewModel.selectImage(img)
                    }
                }
            }
        }

        // 8. FILTER CHIPS (All, Favorites, Gallery, Imported)
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = scale.horizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Photo Library",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = scale.titleMediumSize,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${filteredImages.size} items",
                    fontSize = scale.captionSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = scale.horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("All Photos", "Favorites", "Gallery", "Imported")) { tag ->
                    val isSelected = filterMode == tag
                    FilterChip(
                        selected = isSelected,
                        onClick = { filterMode = tag },
                        label = { Text(tag, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(14.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // 9. PHOTO GRID ITEMS
        if (filteredImages.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No photos in this category",
                            fontSize = scale.captionSize,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Render photo cards in rows of 2
            val chunked = filteredImages.chunked(2)
            items(chunked) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = scale.horizontalPadding, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(scale.cardSpacing)
                ) {
                    PhotoGridItem(
                        image = pair[0],
                        isFavorite = pair[0].isFavorite,
                        onToggleFav = { viewModel.toggleFavorite(pair[0]) },
                        onClick = { viewModel.selectImage(pair[0]) },
                        modifier = Modifier.weight(1f)
                    )

                    if (pair.size > 1) {
                        PhotoGridItem(
                            image = pair[1],
                            isFavorite = pair[1].isFavorite,
                            onToggleFav = { viewModel.toggleFavorite(pair[1]) },
                            onClick = { viewModel.selectImage(pair[1]) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    // BOTTOM SHEET FOR IMPORT OPTIONS
    if (showImportSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showImportSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Import Visuals into APOLLO",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Select from photo library, custom resolution framing, device storage, or scan media.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                ImportOptionTile(
                    title = "Photo Gallery Picker",
                    subtitle = "Select multiple photos via Android visual picker",
                    icon = Icons.Default.PhotoLibrary,
                    tint = MaterialTheme.colorScheme.primary
                ) {
                    showImportSheet = false
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }

                ImportOptionTile(
                    title = "Custom Resolution & Aspect Import",
                    subtitle = "Pick photos & preset target aspect ratio, canvas fit, or exact pixels",
                    icon = Icons.Default.AspectRatio,
                    tint = Color(0xFFF97316)
                ) {
                    showImportSheet = false
                    customResolutionPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }

                ImportOptionTile(
                    title = "Browse Device Files & Storage",
                    subtitle = "Select JPG, PNG, WebP, TIFF, BMP, PDF from storage",
                    icon = Icons.Default.FolderOpen,
                    tint = Color(0xFF7C3AED)
                ) {
                    showImportSheet = false
                    safFilePickerLauncher.launch(arrayOf("image/*", "application/pdf"))
                }

                ImportOptionTile(
                    title = "Scan Device Gallery",
                    subtitle = "Automatically load all camera & downloaded photos",
                    icon = Icons.Default.Sync,
                    tint = Color(0xFF059669)
                ) {
                    showImportSheet = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
                    } else {
                        permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                    }
                    viewModel.scanGallery()
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }

    // CUSTOM RESOLUTION IMPORT BOTTOM SHEET
    if (showCustomResolutionImportSheet && pendingCustomImportUris.isNotEmpty()) {
        CustomResolutionImportBottomSheet(
            selectedUris = pendingCustomImportUris,
            customRatios = customRatios,
            onDismiss = {
                showCustomResolutionImportSheet = false
                pendingCustomImportUris = emptyList()
            },
            onAddCustomRatio = { label, rw, rh ->
                viewModel.addCustomAspectRatio(label, rw, rh)
            },
            onImportConfirmed = { targetRatio, targetW, targetH, fitMode ->
                viewModel.importWithCustomResolution(
                    uris = pendingCustomImportUris,
                    targetRatio = targetRatio,
                    targetW = targetW,
                    targetH = targetH,
                    fitMode = fitMode
                )
                showCustomResolutionImportSheet = false
                pendingCustomImportUris = emptyList()
            }
        )
    }
}

@Composable
private fun ImportOptionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    testTag: String = "",
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val cardBg by animateColorAsState(
        targetValue = if (isDark) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        } else {
            iconColor.copy(alpha = 0.08f)
        },
        animationSpec = tween(250),
        label = "quick_card_bg"
    )

    val cardBorder by animateColorAsState(
        targetValue = if (isDark) {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        } else {
            iconColor.copy(alpha = 0.22f)
        },
        animationSpec = tween(250),
        label = "quick_card_border"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, cardBorder),
        modifier = modifier
            .testTag(testTag)
            .height(72.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun RecentImageCard(
    image: ApolloImage,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier
            .width(130.dp)
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            ) {
                if (image.drawableResId != null) {
                    Image(
                        painter = painterResource(id = image.drawableResId),
                        contentDescription = image.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(image.uri).crossfade(true).build(),
                        contentDescription = image.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = image.name,
                    maxLines = 1,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${image.format} • ${image.formattedSize}",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PhotoGridItem(
    image: ApolloImage,
    isFavorite: Boolean,
    onToggleFav: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.25f)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            ) {
                if (image.drawableResId != null) {
                    Image(
                        painter = painterResource(id = image.drawableResId),
                        contentDescription = image.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(image.uri).crossfade(true).build(),
                        contentDescription = image.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Favorite icon button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable(onClick = onToggleFav),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFFF4B6E) else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = image.name,
                    maxLines = 1,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${image.width} × ${image.height} • ${image.formattedSize}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
