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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uwu.animex.core.pairing.PairingLink
import com.uwu.animex.core.pairing.PairingRouter
import com.uwu.animex.ui.navigation.NotificationRouter
import com.uwu.animex.ui.player.MiniPlayerController
import com.uwu.animex.ui.player.PlayerHost

private val tvRailEntries =
    listOf(
        TvRailEntry(TvTab.HOME, "Beranda", Icons.Outlined.Home),
        TvRailEntry(TvTab.SEARCH, "Cari", Icons.Outlined.Search),
        TvRailEntry(TvTab.SCHEDULE, "Jadwal", Icons.Outlined.DateRange),
        TvRailEntry(TvTab.EXPLORE, "Explore", Icons.Outlined.Explore),
        TvRailEntry(TvTab.LIBRARY, "Pustaka", Icons.Outlined.VideoLibrary),
    )

@Composable
fun TvApp() {
    val backdrop by TvPrefs.backdrop.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(TvTab.HOME) }
    var railExpanded by remember { mutableStateOf(false) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var pairPrefill by remember { mutableStateOf<PairingLink?>(null) }
    val context = LocalContext.current
    val tabFocus = remember { TvTab.entries.associateWith { FocusRequester() } }
    fun focusTab(target: TvTab) {
        tabFocus[target]?.requestFocus()
    }
    val initialTab = rememberSaveable { tab }
    LaunchedEffect(Unit) {
        tab = initialTab
        focusTab(initialTab)
    }

    val pendingDetail by NotificationRouter.pendingDetail.collectAsStateWithLifecycle()
    LaunchedEffect(pendingDetail) {
        val id = pendingDetail ?: return@LaunchedEffect
        NotificationRouter.pendingDetail.value = null
        showSettings = false
        detailId = id
    }

    val pendingEpisode by NotificationRouter.pendingEpisode.collectAsStateWithLifecycle()
    LaunchedEffect(pendingEpisode) {
        val link = pendingEpisode ?: return@LaunchedEffect
        NotificationRouter.pendingEpisode.value = null
        showSettings = false
        MiniPlayerController.open(
            epId = link.epId,
            title = link.title,
            movieId = link.movieId,
            epIndex = link.epIndex,
            startAtMs = link.startAtMs,
        )
    }

    val pendingPair by PairingRouter.pending.collectAsStateWithLifecycle()
    LaunchedEffect(pendingPair) {
        val link = pendingPair ?: return@LaunchedEffect
        PairingRouter.pending.value = null
        detailId = null
        pairPrefill = link
        showSettings = true
    }

    BackHandler {
        when {
            MiniPlayerController.expanded -> MiniPlayerController.minimize()
            showSettings -> {
                showSettings = false
                pairPrefill = null
                focusTab(tab)
            }
            detailId != null -> {
                detailId = null
                focusTab(tab)
            }
            railExpanded -> (context as? Activity)?.finish()
            else -> focusTab(tab)
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
            val id = detailId
            if (id != null) {
                TvDetailScreen(
                    id = id,
                    onBack = {
                        detailId = null
                        focusTab(tab)
                    },
                    onOpen = openDetail,
                )
            } else {
                when (tab) {
                    TvTab.SEARCH -> TvSearchScreen(onOpen = openDetail)
                    TvTab.HOME -> TvHomeScreen(onOpen = openDetail)
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
                    if (target != tab) detailId = null
                    tab = target
                },
                onSettings = { showSettings = true },
                focusRequesters = tabFocus,
                onExpandedChange = { railExpanded = it },
            )
        }

        if (showSettings) {
            TvSettingsOverlay(
                initialPair = pairPrefill,
                onPairConsumed = { pairPrefill = null },
                onDismiss = {
                    showSettings = false
                    pairPrefill = null
                    focusTab(tab)
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
