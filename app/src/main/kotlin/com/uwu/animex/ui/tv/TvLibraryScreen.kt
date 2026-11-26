package com.uwu.animex.ui.tv

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uwu.animex.data.local.Bookmarks
import com.uwu.animex.data.local.WatchStatus

private data class TvLibraryFilter(
    val label: String,
    val status: WatchStatus?,
)

private val tvLibraryFilters =
    listOf(
        TvLibraryFilter("Semua", null),
        TvLibraryFilter(WatchStatus.WATCHING.label, WatchStatus.WATCHING),
        TvLibraryFilter(WatchStatus.COMPLETED.label, WatchStatus.COMPLETED),
        TvLibraryFilter(WatchStatus.ON_HOLD.label, WatchStatus.ON_HOLD),
        TvLibraryFilter(WatchStatus.DROPPED.label, WatchStatus.DROPPED),
        TvLibraryFilter(WatchStatus.PLAN_TO_WATCH.label, WatchStatus.PLAN_TO_WATCH),
    )

@Composable
fun TvLibraryScreen(onOpen: (String) -> Unit) {
    val entries by Bookmarks.entries.collectAsStateWithLifecycle()
    var filterIndex by rememberSaveable { mutableStateOf(0) }
    val firstChipFocus = remember { FocusRequester() }
    val filter = tvLibraryFilters[filterIndex]

    val statusById =
        remember(entries) {
            entries.values.associate { it.movie.id.orEmpty() to it.status?.label }
        }
    val movies =
        remember(entries, filter) {
            entries.values
                .filter { filter.status == null || it.status == filter.status }
                .map { it.movie }
                .filter { !it.id.isNullOrBlank() }
                .distinctBy { it.id }
                .sortedBy { it.title.orEmpty() }
        }

    Column(
        Modifier
            .fillMaxSize()
            .padding(
                start = TvDimens.ContentStart,
                end = TvDimens.ContentEnd,
                top = TvDimens.ContentTop,
                bottom = TvDimens.ContentBottom,
            ),
    ) {
        TvScreenTitle(
            title = "Pustaka",
            subtitle = "${entries.size} anime tersimpan di perangkat",
        )
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            tvLibraryFilters.forEachIndexed { index, entry ->
                TvChip(
                    label = entry.label,
                    selected = filterIndex == index,
                    onClick = { filterIndex = index },
                    focusRequester = if (index == 0) firstChipFocus else null,
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        if (movies.isEmpty()) {
            TvEmptyState(
                title = "Pustaka kosong",
                message = "Tambahkan anime ke pustaka dari halaman detail.",
                modifier = Modifier.weight(1f),
            )
        } else {
            TvMovieGrid(
                movies = movies,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                badge = { movie -> statusById[movie.id] },
                onOpen = { movie -> movie.id?.let(onOpen) },
            )
        }
    }
}
