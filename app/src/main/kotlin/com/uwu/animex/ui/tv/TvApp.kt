@file:OptIn(ExperimentalFoundationApi::class)

package com.uwu.animex.ui.tv

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uwu.animex.ui.navigation.NotificationRouter
import com.uwu.animex.ui.player.MiniPlayerController
import com.uwu.animex.ui.player.PlayerHost

private val tvRailEntries =
    listOf(
        TvRailEntry(TvTab.HOME, "Beranda", Icons.Outlined.Home),
        TvRailEntry(TvTab.SCHEDULE, "Jadwal", Icons.Outlined.DateRange),
        TvRailEntry(TvTab.EXPLORE, "Explore", Icons.Outlined.Explore),
        TvRailEntry(TvTab.LIBRARY, "Pustaka", Icons.Outlined.VideoLibrary),
    )

private val tvRailTabs = tvRailEntries.map { it.tab }.toSet()

@Composable
fun TvApp() {
    val backdrop by TvPrefs.backdrop.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(TvTab.HOME) }
    var railExpanded by remember { mutableStateOf(false) }
    var detailId by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val tabFocus = remember { TvTab.entries.associateWith { FocusRequester() } }
    val homeListFocus = remember { FocusRequester() }
    fun focusTab(target: TvTab) {
        tabFocus[target]?.requestFocus()
    }
    fun focusCurrentTab() {
        val current = if (tab in tvRailTabs) tab else TvTab.HOME
        if (current == TvTab.HOME) {
            homeListFocus.requestFocus()
        } else {
            focusTab(current)
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
            MiniPlayerController.expanded -> MiniPlayerController.minimize()
            showSettings -> {
                showSettings = false
                focusCurrentTab()
            }
            showSearch -> {
                showSearch = false
                focusCurrentTab()
            }
            detailId != null -> {
                detailId = null
                focusCurrentTab()
            }
            railExpanded -> (context as? Activity)?.finish()
            else -> focusCurrentTab()
        }
    }

    Box(Modifier.fillMaxSize().background(TvColors.Background)) {
        TvBackdrop(style = backdrop)

        Box(
            Modifier
                .fillMaxSize()
                .padding(
                    start =
                        if (railExpanded) TvDimens.ContentStartExpanded - TvDimens.ContentStart
                        else 0.dp,
                )
        ) {
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
                        focusCurrentTab()
                    },
                    onOpen = openDetail,
                )
            } else if (showSearch) {
                TvSearchScreen(
                    initialQuery = searchQuery,
                    onOpen = openDetail,
                    onBack = {
                        showSearch = false
                        focusCurrentTab()
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

        Box(Modifier.align(Alignment.CenterStart)) {
            TvRail(
                entries = tvRailEntries,
                selected = tab,
                onSelect = { target ->
                    if (target != tab) {
                        detailId = null
                        showSearch = false
                    }
                    tab = target
                },
                onSettings = { showSettings = true },
                focusRequesters = tabFocus,
                onExpandedChange = { railExpanded = it },
            )
        }

        if (showSettings) {
            TvSettingsOverlay(
                onDismiss = {
                    showSettings = false
                    focusCurrentTab()
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
