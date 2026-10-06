package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAppScale
import com.example.viewmodel.ApolloScreen

data class NavItem(
    val screen: ApolloScreen,
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun FloatingBottomNav(
    currentScreen: ApolloScreen,
    onTabSelected: (ApolloScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = LocalAppScale.current

    val items = listOf(
        NavItem(ApolloScreen.PICTURES, "Photos", Icons.Default.Photo, "nav_photos"),
        NavItem(ApolloScreen.ALBUMS, "Albums", Icons.Default.FolderCopy, "nav_albums"),
        NavItem(ApolloScreen.COLLECTIONS, "Collections", Icons.Default.Layers, "nav_collections"),
        NavItem(ApolloScreen.MENU, "More", Icons.Default.Menu, "nav_more")
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = scale.navHorizontalPadding, vertical = scale.navVerticalPadding),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(36.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            ),
            modifier = Modifier.shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(36.dp),
                spotColor = Color.Black.copy(alpha = 0.15f)
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = if (scale.isCompact) 6.dp else 10.dp, vertical = if (scale.isCompact) 5.dp else 8.dp),
                horizontalArrangement = Arrangement.spacedBy(if (scale.isCompact) 4.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = when (item.screen) {
                        ApolloScreen.PICTURES -> currentScreen == ApolloScreen.PICTURES || currentScreen == ApolloScreen.VIEWER
                        ApolloScreen.ALBUMS -> currentScreen == ApolloScreen.ALBUMS
                        ApolloScreen.COLLECTIONS -> currentScreen == ApolloScreen.COLLECTIONS
                        ApolloScreen.MENU -> currentScreen == ApolloScreen.MENU
                        else -> false
                    }

                    val activeBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) activeBg else Color.Transparent,
                        animationSpec = tween(250),
                        label = "nav_bg"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        animationSpec = tween(250),
                        label = "nav_content"
                    )

                    Box(
                        modifier = Modifier
                            .testTag(item.testTag)
                            .clip(CircleShape)
                            .background(bgColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onTabSelected(item.screen)
                            }
                            .padding(horizontal = if (scale.isCompact) 10.dp else 14.dp, vertical = if (scale.isCompact) 6.dp else 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = contentColor,
                                modifier = Modifier.size(if (scale.isCompact) 18.dp else 20.dp)
                            )
                            if (isSelected) {
                                Text(
                                    text = item.title,
                                    color = contentColor,
                                    fontSize = if (scale.isCompact) 11.sp else 13.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
