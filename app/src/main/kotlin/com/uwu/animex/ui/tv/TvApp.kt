@file:OptIn(ExperimentalFoundationApi::class)

package com.uwu.animex.ui.tv

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uwu.animex.data.local.Appearance
import com.uwu.animex.ui.navigation.NotificationRouter
import com.uwu.animex.ui.player.MiniPlayerController
import com.uwu.animex.ui.player.PlayerHost
import com.uwu.animex.ui.theme.rememberAppDarkTheme
import kotlinx.coroutines.delay

private val tvRailEntries =
    listOf(
        TvRailEntry(TvTab.HOME, "Beranda", Icons.Outlined.Home),
        TvRailEntry(TvTab.SCHEDULE, "Jadwal", Icons.Outlined.DateRange),
        TvRailEntry(TvTab.EXPLORE, "Explore", Icons.Outlined.Explore),
        TvRailEntry(TvTab.LIBRARY, "Pustaka", Icons.Outlined.VideoLibrary),
    )

@Composable
fun TvApp() {
    val backdrop by TvPrefs.backdrop.collectAsStateWithLifecycle()
    val settings by Appearance.settings.collectAsStateWithLifecycle()
    val darkTheme = rememberAppDarkTheme(settings.mode)
    val tvColors = if (darkTheme) TvColorsDark else TvColorsLight
    var tab by remember { mutableStateOf(TvTab.HOME) }
    var railFocused by remember { mutableStateOf(false) }
    var detailId by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showExitDialog by remember { mutableStateOf(false) }
    var backPressCount by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val activity = context as? Activity
    val tabFocus = remember { TvTab.entries.associateWith { FocusRequester() } }
    val homeListFocus = remember { FocusRequester() }
    val contentFocus = remember { FocusRequester() }
    val exitCancelFocus = remember { FocusRequester() }
    val railEnabled = detailId == null && !showSearch && !showSettings && !MiniPlayerController.expanded

    LaunchedEffect(tab) {
        backPressCount = 0
        showExitDialog = false
    }
    LaunchedEffect(backPressCount) {
        if (backPressCount > 0) {
            delay(2500)
            backPressCount = 0
        }
    }
    LaunchedEffect(showExitDialog) {
        if (showExitDialog) runCatching { exitCancelFocus.requestFocus() }
    }

    fun focusContent(target: TvTab = tab) {
        runCatching {
            if (target == TvTab.HOME || target == TvTab.SEARCH) {
                homeListFocus.requestFocus()
            } else {
                contentFocus.requestFocus()
            }
        }
    }

    val pendingDetail by NotificationRouter.pendingDetail.collectAsStateWithLifecycle()
    LaunchedEffect(pendingDetail) {
        val id = pendingDetail ?: return@LaunchedEffect
        NotificationRouter.pendingDetail.value = null
        showSettings = false
        showSearch = false
        detailId = id
    }

    val pendingEpisode by NotificationRouter.pendingEpisode.collectAsStateWithLifecycle()
    LaunchedEffect(pendingEpisode) {
        val link = pendingEpisode ?: return@LaunchedEffect
        NotificationRouter.pendingEpisode.value = null
        showSettings = false
        showSearch = false
        MiniPlayerController.open(
            epId = link.epId,
            title = link.title,
            movieId = link.movieId,
            epIndex = link.epIndex,
            startAtMs = link.startAtMs,
        )
    }

    BackHandler {
        when {
            showExitDialog -> showExitDialog = false
            MiniPlayerController.expanded -> MiniPlayerController.minimize()
            showSettings -> {
                showSettings = false
                backPressCount = 0
                focusContent()
            }
            showSearch -> {
                showSearch = false
                backPressCount = 0
                focusContent()
            }
            detailId != null -> {
                detailId = null
                backPressCount = 0
                focusContent()
            }
            tab == TvTab.HOME -> {
                focusContent()
                if (backPressCount > 0) {
                    showExitDialog = true
                    backPressCount = 0
                } else {
                    backPressCount = 1
                }
            }
            railFocused -> {
                backPressCount = 0
                focusContent()
            }
            else -> {
                backPressCount = 0
                activity?.finish()
            }
        }
    }

    CompositionLocalProvider(LocalTvColors provides tvColors) {
        Box(Modifier.fillMaxSize().background(LocalTvColors.current.Background)) {
            TvBackdrop(style = backdrop)

            Row(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(if (railFocused) TvDimens.RailExpandedWidth else TvDimens.RailCollapsedWidth),
                ) {
                    TvRail(
                        entries = tvRailEntries,
                        selected = tab,
                        onSelect = { target ->
                            if (target != tab) {
                                detailId = null
                                showSearch = false
                            }
                            tab = target
                            backPressCount = 0
                            focusContent(target)
                        },
                        onSettings = { showSettings = true },
                        focusRequesters = tabFocus,
                        enabled = railEnabled,
                        expanded = railFocused,
                        onFocusChange = { railFocused = it },
                    )
                }

                Box(Modifier.fillMaxHeight().weight(1f)) {
                    // Tiny focus anchor at the content edge. Keeps LEFT navigation able to
                    // reach the rail (a full-screen focusable Box would overlap it and trap focus).
                    Box(
                        Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = TvDimens.ContentStart)
                            .size(1.dp)
                            .focusable()
                            .focusRequester(contentFocus),
                    )
                    val openDetail: (String) -> Unit = { id -> if (id.isNotBlank()) detailId = id }
                    val openSearch: (String) -> Unit = { query ->
                        searchQuery = query
                        showSearch = true
                    }
                    val id = detailId
                    if (id != null) {
                        TvDetailScreen(
                            id = id,
                            onBack = {
                                detailId = null
                                focusContent()
                            },
                            onOpen = openDetail,
                        )
                    } else if (showSearch) {
                        TvSearchScreen(
                            initialQuery = searchQuery,
                            onOpen = openDetail,
                            onBack = {
                                showSearch = false
                                focusContent()
                            },
                        )
                    } else {
                        when (tab) {
                            TvTab.SEARCH ->
                                TvHomeScreen(
                                    onOpen = openDetail,
                                    onSearch = openSearch,
                                    listFocusRequester = homeListFocus,
                                )
                            TvTab.HOME ->
                                TvHomeScreen(
                                    onOpen = openDetail,
                                    onSearch = openSearch,
                                    listFocusRequester = homeListFocus,
                                )
                            TvTab.SCHEDULE -> TvScheduleScreen(onOpen = openDetail)
                            TvTab.EXPLORE -> TvExploreScreen(onOpen = openDetail)
                            TvTab.LIBRARY -> TvLibraryScreen(onOpen = openDetail)
                        }
                    }
                }
            }

            if (showSettings) {
                TvSettingsOverlay(
                    onDismiss = {
                        showSettings = false
                        focusContent()
                    },
                )
            }

            if (showExitDialog) {
                val dialogColors = LocalTvColors.current
                AlertDialog(
                    onDismissRequest = { showExitDialog = false },
                    properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
                    title = {
                        Text(
                            "Keluar dari AnimeX?",
                            color = dialogColors.Text,
                            fontSize = 24.sp,
                        )
                    },
                    text = {
                        Text(
                            "Yakin ingin keluar dari aplikasi?",
                            color = dialogColors.Muted,
                            fontSize = 16.sp,
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showExitDialog = false
                                activity?.finish()
                            },
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = dialogColors.FocusFill,
                                    contentColor = dialogColors.FocusContent,
                                ),
                        ) {
                            Text("Ya")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showExitDialog = false },
                            modifier = Modifier.focusRequester(exitCancelFocus),
                            colors = ButtonDefaults.textButtonColors(contentColor = dialogColors.Text),
                        ) {
                            Text("Tidak")
                        }
                    },
                )
            }

            PlayerHost(
                hasFloatingBottomBar = false,
                tvMode = true,
                onOpenAnime = { animeId ->
                    MiniPlayerController.minimize()
                    if (animeId.isNotBlank()) detailId = animeId
                },
                onNeedLogin = {},
            )
        }
    }
}
