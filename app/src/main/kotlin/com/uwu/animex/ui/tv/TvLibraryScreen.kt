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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uwu.animex.data.anilist.AniList
import com.uwu.animex.data.anilist.AniListLibrary
import com.uwu.animex.data.api.TitleMatcher
import com.uwu.animex.data.local.Bookmarks
import com.uwu.animex.data.local.WatchStatus
import com.uwu.animex.data.mal.Mal
import com.uwu.animex.data.mal.MalLibrary
import com.uwu.animex.data.model.Movie
import com.uwu.animex.data.tracker.SyncSource
import com.uwu.animex.data.tracker.SyncSourcePref
import com.uwu.animex.sync.LibraryItem
import kotlinx.coroutines.launch

private data class TvLibraryFilter(
    val label: String,
    val status: WatchStatus?,
)

private data class TvLibraryCard(
    val movie: Movie,
    val statusLabel: String?,
    val remote: LibraryItem? = null,
    val remoteSource: SyncSource? = null,
    val linked: Boolean = false,
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
    val scope = rememberCoroutineScope()
    val entries by Bookmarks.entries.collectAsStateWithLifecycle()
    val pref by SyncSourcePref.source.collectAsStateWithLifecycle()
    val malLogged by Mal.loggedIn.collectAsStateWithLifecycle()
    val aniLogged by AniList.loggedIn.collectAsStateWithLifecycle()
    val source = SyncSourcePref.effective(pref, malLogged, aniLogged)

    val malItems by MalLibrary.items.collectAsStateWithLifecycle()
    val aniItems by AniListLibrary.items.collectAsStateWithLifecycle()
    val malError by MalLibrary.error.collectAsStateWithLifecycle()
    val aniError by AniListLibrary.error.collectAsStateWithLifecycle()
    val malRefreshing by MalLibrary.refreshing.collectAsStateWithLifecycle()
    val aniRefreshing by AniListLibrary.refreshing.collectAsStateWithLifecycle()

    val remoteItems =
        when (source) {
            SyncSource.MAL -> malItems
            SyncSource.ANILIST -> aniItems
            SyncSource.LOCAL -> emptyList()
        }
    val remoteError =
        when (source) {
            SyncSource.MAL -> malError
            SyncSource.ANILIST -> aniError
            SyncSource.LOCAL -> null
        }
    val remoteRefreshing =
        when (source) {
            SyncSource.MAL -> malRefreshing
            SyncSource.ANILIST -> aniRefreshing
            SyncSource.LOCAL -> false
        }

    var filterIndex by rememberSaveable { mutableStateOf(0) }
    val filter = tvLibraryFilters[filterIndex]

    LaunchedEffect(source) {
        filterIndex = 0
        when (source) {
            SyncSource.MAL -> MalLibrary.refresh()
            SyncSource.ANILIST -> AniListLibrary.refresh()
            SyncSource.LOCAL -> Unit
        }
    }

    val cards =
        remember(entries, remoteItems, source, filter) {
            buildList {
                val linkedIds = mutableSetOf<String>()
                if (source != SyncSource.LOCAL) {
                    remoteItems.forEach { remote ->
                        if (filter.status != null && remote.status.label != filter.status.label) return@forEach
                        val remoteNumericId = remote.syncId.toIntOrNull()
                        val localId =
                            if (source == SyncSource.ANILIST) {
                                remoteNumericId?.let(AniList::movieIdFor)
                            } else {
                                remoteNumericId?.let(Mal::movieIdFor)
                            }
                        if (localId != null) linkedIds += localId
                        val key = localId ?: "${source.name.lowercase()}-${remote.syncId}"
                        add(
                            TvLibraryCard(
                                movie =
                                    Movie(
                                        id = key,
                                        title = remote.name,
                                        image_poster = remote.posterUrl,
                                        type = remote.mediaType,
                                        score = remote.personalRating?.takeIf { it > 0 }?.toString(),
                                    ),
                                statusLabel = remote.status.label,
                                remote = remote,
                                remoteSource = source,
                                linked = localId != null,
                            ),
                        )
                    }
                }
                entries.values.forEach { entry ->
                    val id = entry.movie.id
                    if (id.isNullOrBlank() || id in linkedIds) return@forEach
                    if (filter.status != null && entry.status != filter.status) return@forEach
                    add(TvLibraryCard(movie = entry.movie, statusLabel = entry.status?.label))
                }
            }.distinctBy { it.movie.id }.sortedBy { it.movie.title.orEmpty() }
        }
    val statusById =
        remember(cards) {
            cards.associate { it.movie.id.orEmpty() to it.statusLabel }
        }
    val movies = remember(cards) { cards.map { it.movie } }

    val openCard: (Movie) -> Unit = { movie ->
        val card = cards.firstOrNull { it.movie.id == movie.id }
        val remote = card?.remote
        when {
            card == null || remote == null -> movie.id?.let(onOpen)
            card.linked -> movie.id?.let(onOpen)
            else ->
                scope.launch {
                    val match =
                        TitleMatcher.find(
                            titles = listOf(remote.name) + remote.synonyms,
                            wantedType = remote.mediaType,
                        ).best
                    val id = match?.id ?: return@launch
                    val remoteId = remote.syncId.toIntOrNull() ?: return@launch
                    if (card.remoteSource == SyncSource.ANILIST) {
                        AniList.link(id, remoteId)
                    } else {
                        Mal.link(id, remoteId)
                    }
                    onOpen(id)
                }
        }
    }

    val subtitle =
        when {
            source == SyncSource.LOCAL && pref != SyncSource.LOCAL ->
                "${movies.size} anime lokal • ${pref.label} belum tersambung"
            source == SyncSource.LOCAL -> "${movies.size} anime tersimpan di perangkat"
            else -> "${movies.size} anime dari ${source.label}"
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
        TvScreenTitle(title = "Pustaka", subtitle = subtitle)
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            listOf(SyncSource.LOCAL, SyncSource.MAL, SyncSource.ANILIST).forEach { option ->
                val connected =
                    when (option) {
                        SyncSource.LOCAL -> true
                        SyncSource.MAL -> malLogged
                        SyncSource.ANILIST -> aniLogged
                    }
                val label = option.label + if (connected && option != SyncSource.LOCAL) " • tersambung" else ""
                TvChip(
                    label = label,
                    selected = pref == option,
                    onClick = { SyncSourcePref.set(option) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
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
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        if (movies.isEmpty()) {
            val message =
                when {
                    remoteRefreshing -> "Memuat pustaka ${source.label}…"
                    remoteError != null -> "Gagal muat pustaka ${source.label}: $remoteError"
                    source == SyncSource.LOCAL -> "Tambahkan anime ke pustaka dari halaman detail."
                    else -> "Belum ada anime di pustaka ${source.label}."
                }
            TvEmptyState(
                title = if (remoteError != null) "Pustaka belum termuat" else "Pustaka kosong",
                message = message,
                modifier = Modifier.weight(1f),
            )
        } else {
            TvMovieGrid(
                movies = movies,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                badge = { movie -> statusById[movie.id] },
                onOpen = openCard,
            )
        }
    }
}
