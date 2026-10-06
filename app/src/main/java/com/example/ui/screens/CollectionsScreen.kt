package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.R
import com.example.data.model.ExportPreset
import com.example.ui.theme.LocalAppScale
import com.example.viewmodel.ApolloScreen
import com.example.viewmodel.ApolloViewModel

@Composable
fun CollectionsScreen(
    viewModel: ApolloViewModel,
    modifier: Modifier = Modifier
) {
    val presets by viewModel.presets.collectAsState()
    val images by viewModel.images.collectAsState()
    val scale = LocalAppScale.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        // 1. TOP HEADER
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = scale.horizontalPadding, vertical = if (scale.isCompact) 10.dp else 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Collections",
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = scale.titleLargeSize,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Your presets, recipes and resources.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = scale.captionSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { /* Search */ },
                    modifier = Modifier.testTag("collections_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 2. 2x2 COLLECTION HUBS GRID
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = scale.horizontalPadding, vertical = if (scale.isCompact) 4.dp else 8.dp),
                verticalArrangement = Arrangement.spacedBy(scale.cardSpacing)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(scale.cardSpacing)
                ) {
                    CollectionHubCard(
                        title = "Presets",
                        count = "12 items",
                        icon = Icons.Default.Layers,
                        iconTint = Color(0xFF1868F8),
                        scale = scale,
                        modifier = Modifier.weight(1f)
                    ) {
                        presets.firstOrNull()?.let { viewModel.applyPreset(it) }
                    }

                    CollectionHubCard(
                        title = "Export Recipes",
                        count = "8 items",
                        icon = Icons.Default.Description,
                        iconTint = Color(0xFF0EA568),
                        scale = scale,
                        modifier = Modifier.weight(1f)
                    ) {
                        presets.getOrNull(1)?.let { viewModel.applyPreset(it) }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(scale.cardSpacing)
                ) {
                    CollectionHubCard(
                        title = "Filters",
                        count = "20 items",
                        icon = Icons.Default.InvertColors,
                        iconTint = Color(0xFF0284C7),
                        scale = scale,
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.navigateTo(ApolloScreen.EDITOR)
                    }

                    CollectionHubCard(
                        title = "Resources",
                        count = "6 items",
                        icon = Icons.Default.Folder,
                        iconTint = Color(0xFF7C3AED),
                        scale = scale,
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.navigateTo(ApolloScreen.ALBUMS)
                    }
                }
            }
        }

        // 3. TOOLS SECTION
        item {
            Text(
                text = "Tools",
                style = MaterialTheme.typography.titleMedium,
                fontSize = scale.titleMediumSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = scale.horizontalPadding, top = if (scale.isCompact) 14.dp else 20.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = scale.horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(scale.cardSpacing)
            ) {
                ToolOptionCard(
                    title = "Batch Converter",
                    subtitle = "Process multiple files",
                    icon = Icons.Default.Layers,
                    scale = scale,
                    modifier = Modifier.weight(1f),
                    testTag = "tool_batch_card"
                ) {
                    viewModel.setBatchImages(images)
                    viewModel.navigateTo(ApolloScreen.BATCH)
                }

                ToolOptionCard(
                    title = "PDF Studio",
                    subtitle = "Create PDF from images",
                    icon = Icons.Default.PictureAsPdf,
                    scale = scale,
                    modifier = Modifier.weight(1f),
                    testTag = "tool_pdf_card"
                ) {
                    viewModel.navigateTo(ApolloScreen.PDF_STUDIO)
                }
            }
        }

        // 4. PRESETS LIST
        item {
            Text(
                text = "Export Recipes & Presets",
                style = MaterialTheme.typography.titleMedium,
                fontSize = scale.titleMediumSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = scale.horizontalPadding, top = if (scale.isCompact) 16.dp else 22.dp, bottom = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = scale.horizontalPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    PresetListItem(
                        preset = preset,
                        scale = scale,
                        onClick = { viewModel.applyPreset(preset) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionHubCard(
    title: String,
    count: String,
    icon: ImageVector,
    iconTint: Color,
    scale: com.example.ui.theme.AppScaleConfig,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(scale.cardCornerRadius),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(if (scale.isCompact) 10.dp else 14.dp)) {
            Box(
                modifier = Modifier
                    .size(if (scale.isCompact) 32.dp else 38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(if (scale.isCompact) 18.dp else 22.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontSize = scale.titleMediumSize,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = count,
                fontSize = scale.captionSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ToolOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    scale: com.example.ui.theme.AppScaleConfig,
    modifier: Modifier = Modifier,
    testTag: String = "",
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(scale.cardCornerRadius),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = modifier
            .testTag(testTag)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(if (scale.isCompact) 10.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (scale.isCompact) 36.dp else 42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(scale.iconSize)
                )
            }
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = scale.bodyMediumSize
                )
                Text(
                    text = subtitle,
                    fontSize = scale.captionSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PresetListItem(
    preset: ExportPreset,
    scale: com.example.ui.theme.AppScaleConfig,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(scale.cardCornerRadius),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(if (scale.isCompact) 10.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = scale.bodyMediumSize
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${preset.format.name} • ${preset.resolution.label} • ${preset.quality}% • ${preset.metadataPolicy.label}",
                    fontSize = scale.captionSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
