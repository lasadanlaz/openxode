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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uwu.animex.core.network.toUserMessage
import com.uwu.animex.data.api.Api
import com.uwu.animex.data.model.ExploreData
import com.uwu.animex.data.model.ExploreItem
import com.uwu.animex.data.model.Movie
import com.uwu.animex.ui.common.UiState
import com.uwu.animex.ui.viewmodel.ExploreViewModel
import kotlinx.coroutines.CancellationException

private data class TvExploreKind(
    val key: String,
    val label: String,
)

private val tvExploreKinds =
    listOf(
        TvExploreKind("genre", "Genre"),
        TvExploreKind("studio", "Studio"),
        TvExploreKind("year", "Tahun"),
        TvExploreKind("type", "Tipe"),
    )

private fun ExploreData.itemsFor(kind: String): List<ExploreItem> =
    when (kind) {
        "studio" -> studio
        "year" -> year
        "type" -> typeOrDefault
        else -> genre
    }

@Composable
fun TvExploreScreen(
    onOpen: (String) -> Unit,
    viewModel: ExploreViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.revalidate() }

    var kindIndex by rememberSaveable { mutableStateOf(0) }
    val kind = tvExploreKinds[kindIndex]
    var selectedItemId by remember(kind.key) { mutableStateOf<String?>(null) }
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }

    val data = (state as? UiState.Ready)?.value
    val items = data?.itemsFor(kind.key).orEmpty()

    LaunchedEffect(kind.key, items) {
        if (items.isEmpty()) return@LaunchedEffect
        if (selectedItemId == null || items.none { it.filterKey() == selectedItemId }) {
            selectedItemId = items.firstOrNull()?.filterKey()
        }
    }

    LaunchedEffect(kind.key, selectedItemId, reloadKey) {
        val value = selectedItemId ?: return@LaunchedEffect
        loading = true
        error = null
        try {
            val item = items.firstOrNull { it.filterKey() == value }
            movies =
                Api.exploreMovies(
                    kind = kind.key,
                    idOrName = value,
                    title = item?.displayName.orEmpty(),
                    sort = "views",
                )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.toUserMessage()
        } finally {
            loading = false
        }
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
            title = "Explore",
            subtitle = "Jelajahi anime berdasarkan genre, studio, tahun, dan tipe",
        )
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            tvExploreKinds.forEachIndexed { index, entry ->
                TvChip(
                    label = entry.label,
                    selected = kindIndex == index,
                    onClick = { kindIndex = index },
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
            items.forEach { item ->
                val key = item.filterKey()
                TvChip(
                    label = item.displayName,
                    selected = selectedItemId == key,
                    onClick = { selectedItemId = key },
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        when {
            state is UiState.Error -> TvErrorState(
                (state as UiState.Error).msg,
                onRetry = { viewModel.refresh() },
                modifier = Modifier.weight(1f),
            )
            state is UiState.Loading -> TvLoading(Modifier.weight(1f))
            loading && movies.isEmpty() -> TvLoading(Modifier.weight(1f))
            error != null -> TvErrorState(
                error.orEmpty(),
                onRetry = { reloadKey++ },
                modifier = Modifier.weight(1f),
            )
            movies.isEmpty() -> TvEmptyState(
                title = "Tidak ada hasil",
                message = "Coba pilih kategori lain.",
                modifier = Modifier.weight(1f),
            )
            else -> TvMovieGrid(
                movies = movies,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                onOpen = { movie -> movie.id?.let(onOpen) },
            )
        }
    }
}

private fun ExploreItem.filterKey(): String = id?.takeIf { it.isNotBlank() } ?: displayName
