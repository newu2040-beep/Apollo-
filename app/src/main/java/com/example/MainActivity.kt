package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.ui.components.FloatingBottomNav
import com.example.ui.components.MetadataDialog
import com.example.ui.screens.AdvancedExportScreen
import com.example.ui.screens.AlbumsScreen
import com.example.ui.screens.BatchConvertScreen
import com.example.ui.screens.CollectionsScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ImageViewerScreen
import com.example.ui.screens.MenuScreen
import com.example.ui.screens.PdfStudioScreen
import com.example.ui.screens.PicturesScreen
import com.example.ui.theme.ApolloAppTheme
import com.example.ui.theme.LocalAppScale
import com.example.ui.theme.rememberAppScale
import com.example.viewmodel.ApolloScreen
import com.example.viewmodel.ApolloViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ApolloViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val activeTheme by viewModel.activeTheme.collectAsState()
            val forceDark by viewModel.isDarkMode.collectAsState()
            val uiScaleMode by viewModel.uiScaleMode.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()
            val showMetadata by viewModel.showMetadataDialog.collectAsState()
            val metadataInfo by viewModel.metadataInfo.collectAsState()

            val isDark = forceDark ?: isSystemInDarkTheme()
            val appScale = rememberAppScale(uiScaleMode)

            // Synchronize status bar and navigation bar icon light/dark mode cleanly
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as? Activity)?.window
                    if (window != null) {
                        WindowCompat.getInsetsController(window, view).apply {
                            isAppearanceLightStatusBars = !isDark
                            isAppearanceLightNavigationBars = !isDark
                        }
                    }
                }
            }

            BackHandler(enabled = currentScreen != ApolloScreen.PICTURES) {
                viewModel.navigateBack()
            }

            ApolloAppTheme(
                activeTheme = activeTheme,
                forceDark = forceDark
            ) {
                CompositionLocalProvider(LocalAppScale provides appScale) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.onBackground,
                            bottomBar = {
                                val showNav = currentScreen == ApolloScreen.PICTURES ||
                                        currentScreen == ApolloScreen.ALBUMS ||
                                        currentScreen == ApolloScreen.COLLECTIONS ||
                                        currentScreen == ApolloScreen.MENU

                                AnimatedVisibility(
                                    visible = showNav,
                                    enter = slideInVertically(
                                        initialOffsetY = { it },
                                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                                    ) + fadeIn(animationSpec = tween(240)),
                                    exit = slideOutVertically(
                                        targetOffsetY = { it },
                                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                                    ) + fadeOut(animationSpec = tween(180))
                                ) {
                                    FloatingBottomNav(
                                        currentScreen = currentScreen,
                                        onTabSelected = { tab ->
                                            viewModel.selectTab(tab)
                                        }
                                    )
                                }
                            }
                        ) { _ ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                AnimatedContent(
                                    targetState = currentScreen,
                                    transitionSpec = {
                                        val tabScreens = setOf(
                                            ApolloScreen.PICTURES,
                                            ApolloScreen.ALBUMS,
                                            ApolloScreen.COLLECTIONS,
                                            ApolloScreen.MENU
                                        )
                                        val isTabSwitch = initialState in tabScreens && targetState in tabScreens

                                        if (isTabSwitch) {
                                            (fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing)) +
                                                    scaleIn(initialScale = 0.98f, animationSpec = tween(260, easing = FastOutSlowInEasing)))
                                                .togetherWith(
                                                    fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                                                            scaleOut(targetScale = 0.98f, animationSpec = tween(200, easing = FastOutSlowInEasing))
                                                )
                                        } else {
                                            val isOpeningDetail = targetState !in tabScreens
                                            if (isOpeningDetail) {
                                                (slideInHorizontally(
                                                    initialOffsetX = { fullWidth -> (fullWidth * 0.12f).toInt() },
                                                    animationSpec = tween(320, easing = FastOutSlowInEasing)
                                                ) + fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)))
                                                    .togetherWith(
                                                        slideOutHorizontally(
                                                            targetOffsetX = { fullWidth -> (-fullWidth * 0.08f).toInt() },
                                                            animationSpec = tween(280, easing = FastOutSlowInEasing)
                                                        ) + fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing))
                                                    )
                                            } else {
                                                (slideInHorizontally(
                                                    initialOffsetX = { fullWidth -> (-fullWidth * 0.08f).toInt() },
                                                    animationSpec = tween(280, easing = FastOutSlowInEasing)
                                                ) + fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing)))
                                                    .togetherWith(
                                                        slideOutHorizontally(
                                                            targetOffsetX = { fullWidth -> (fullWidth * 0.12f).toInt() },
                                                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                                                        ) + fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing))
                                                    )
                                            }
                                        }
                                    },
                                    label = "screen_transition"
                                ) { screen ->
                                    when (screen) {
                                        ApolloScreen.PICTURES -> PicturesScreen(viewModel = viewModel)
                                        ApolloScreen.VIEWER -> ImageViewerScreen(viewModel = viewModel)
                                        ApolloScreen.EDITOR -> EditorScreen(viewModel = viewModel)
                                        ApolloScreen.EXPORT -> AdvancedExportScreen(viewModel = viewModel)
                                        ApolloScreen.ALBUMS -> AlbumsScreen(viewModel = viewModel)
                                        ApolloScreen.COLLECTIONS -> CollectionsScreen(viewModel = viewModel)
                                        ApolloScreen.MENU -> MenuScreen(viewModel = viewModel)
                                        ApolloScreen.BATCH -> BatchConvertScreen(viewModel = viewModel)
                                        ApolloScreen.PDF_STUDIO -> PdfStudioScreen(viewModel = viewModel)
                                        ApolloScreen.HISTORY -> HistoryScreen(viewModel = viewModel)
                                    }
                                }

                                if (showMetadata && metadataInfo != null) {
                                    MetadataDialog(
                                        metadata = metadataInfo,
                                        onDismiss = { viewModel.dismissMetadataDialog() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
