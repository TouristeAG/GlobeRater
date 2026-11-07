package com.example.currencywidget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.currencywidget.ui.screen.SettingsScreen
import com.example.currencywidget.ui.screen.TodayScreen
import com.example.currencywidget.ui.screen.WidgetsScreen
import com.example.currencywidget.ui.theme.CurrencyWidgetTheme
import com.example.currencywidget.ui.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        const val ACTION_WIDGET_COLORS = "com.example.currencywidget.WIDGET_COLORS"
        const val EXTRA_PROVIDER = "provider"
    }

    /** Widget type whose colour panel should open (set when launched from a widget tap). */
    private val colorsRequest = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handle(intent)
        setContent {
            CurrencyWidgetTheme {
                GlobeRaterApp(colorsRequest.value) { colorsRequest.value = null }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: android.content.Intent?) {
        if (intent?.action == ACTION_WIDGET_COLORS) colorsRequest.value = intent.getStringExtra(EXTRA_PROVIDER)
    }
}

private enum class Tab(val label: Int, val selected: ImageVector, val unselected: ImageVector) {
    TODAY(R.string.tab_today, Icons.Filled.Today, Icons.Outlined.Today),
    WIDGETS(R.string.tab_widgets, Icons.Filled.Widgets, Icons.Outlined.Widgets),
    SETTINGS(R.string.tab_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
private fun GlobeRaterApp(colorsRequest: String?, onColorsHandled: () -> Unit) {
    val home: HomeViewModel = hiltViewModel()
    val state by home.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    val snackbar = remember { SnackbarHostState() }

    LifecycleResumeEffect(Unit) {
        home.onResume()
        onPauseOrDispose { }
    }
    BackHandler(enabled = tab != Tab.TODAY) { tab = Tab.TODAY }
    androidx.compose.runtime.LaunchedEffect(colorsRequest) { if (colorsRequest != null) tab = Tab.WIDGETS }

    fun select(t: Tab) {
        // Settings may have changed what Today / Widgets show.
        if (tab == Tab.SETTINGS && t != Tab.SETTINGS) home.onResume()
        tab = t
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column {
                androidx.compose.material3.HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    Tab.values().forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { select(t) },
                            icon = { Icon(if (tab == t) t.selected else t.unselected, contentDescription = null) },
                            label = { Text(androidx.compose.ui.res.stringResource(t.label), style = MaterialTheme.typography.labelMedium) },
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                // Slide in the direction of the tab you moved to.
                val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (slideInHorizontally(tween(320)) { w -> dir * w / 6 } + fadeIn(tween(320))) togetherWith
                    (slideOutHorizontally(tween(260)) { w -> -dir * w / 6 } + fadeOut(tween(200)))
            },
            label = "tab"
        ) { current ->
            when (current) {
                Tab.TODAY -> TodayScreen(state, home, padding, onOpenSettings = { select(Tab.SETTINGS) })
                Tab.WIDGETS -> WidgetsScreen(state.version, padding, snackbar, colorsRequest, onColorsHandled)
                Tab.SETTINGS -> SettingsScreen(padding)
            }
        }
        // Opaque status bar area so scrolled content doesn't run under the clock/icons.
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(MaterialTheme.colorScheme.surface)
        )
    }
}
