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
import com.uwu.animex.ui.navigation.NotificationRouter
import com.uwu.animex.ui.player.MiniPlayerController
import com.uwu.animex.ui.player.PlayerHost

private val tvRailEntries =
    listOf(
        TvRailEntry(TvTab.SEARCH, "Cari", Icons.Outlined.Search),
        TvRailEntry(TvTab.HOME, "Beranda", Icons.Outlined.Home),
        TvRailEntry(TvTab.SCHEDULE, "Jadwal", Icons.Outlined.DateRange),
        TvRailEntry(TvTab.EXPLORE, "Explore", Icons.Outlined.Explore),
        TvRailEntry(TvTab.LIBRARY, "Pustaka", Icons.Outlined.VideoLibrary),
    )

@Composable
fun TvApp() {
    val backdrop by TvPrefs.backdrop.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(TvTab.HOME) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val homeFocus = remember { FocusRequester() }

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

    BackHandler {
        when {
            MiniPlayerController.expanded -> MiniPlayerController.minimize()
            showSettings -> {
                showSettings = false
                homeFocus.requestFocus()
            }
            detailId != null -> {
                detailId = null
                homeFocus.requestFocus()
            }
            tab != TvTab.HOME -> {
                tab = TvTab.HOME
                homeFocus.requestFocus()
            }
            else -> (context as? Activity)?.finish()
        }
    }

    Box(Modifier.fillMaxSize().background(TvColors.Background)) {
        TvBackdrop(style = backdrop)

        Box(Modifier.fillMaxSize()) {
            val openDetail: (String) -> Unit = { id -> if (id.isNotBlank()) detailId = id }
            val id = detailId
            if (id != null) {
                TvDetailScreen(
                    id = id,
                    onBack = {
                        detailId = null
                        homeFocus.requestFocus()
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
                onSelect = {
                    detailId = null
                    tab = it
                },
                onSettings = { showSettings = true },
                homeFocus = homeFocus,
            )
        }

        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 30.dp, bottom = 16.dp),
        ) {
            TvCaption(captionFor(tab, detailId, showSettings))
        }

        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 28.dp, bottom = 72.dp),
        ) {
            TvDpadHint()
        }

        if (showSettings) {
            TvSettingsOverlay(
                onDismiss = {
                    showSettings = false
                    homeFocus.requestFocus()
                },
            )
        }

        PlayerHost(
            hasFloatingBottomBar = false,
            onOpenAnime = { animeId ->
                MiniPlayerController.minimize()
                if (animeId.isNotBlank()) detailId = animeId
            },
            onNeedLogin = {},
        )
    }
}

private fun captionFor(
    tab: TvTab,
    detailId: String?,
    settings: Boolean,
): String =
    when {
        settings -> "BACK untuk menutup setelan"
        detailId != null -> "OK untuk memutar episode · BACK untuk kembali"
        tab == TvTab.SEARCH -> "Ketik judul, lalu tekan OK untuk mencari"
        tab == TvTab.HOME -> "Geser ke atas/bawah untuk pindah baris · OK untuk membuka"
        tab == TvTab.SCHEDULE -> "Pilih hari, lalu pilih anime"
        tab == TvTab.EXPLORE -> "Pilih kategori untuk menjelajah"
        tab == TvTab.LIBRARY -> "Pustaka tersimpan di perangkat ini"
        else -> "OK untuk memilih"
    }
