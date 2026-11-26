package com.uwu.animex.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.uwu.animex.data.local.Bookmarks
import com.uwu.animex.data.local.WatchStatus
import com.uwu.animex.data.model.Episode
import com.uwu.animex.data.model.Movie
import com.uwu.animex.ui.common.UiState
import com.uwu.animex.ui.player.MiniPlayerController
import com.uwu.animex.ui.viewmodel.DetailPayload
import com.uwu.animex.ui.viewmodel.DetailViewModel

@Composable
fun TvDetailScreen(
    id: String,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val viewModel: DetailViewModel =
        viewModel(key = "tv-detail-$id", factory = DetailViewModel.factory(id))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val episodeList by viewModel.episodeList.collectAsStateWithLifecycle()
    val backFocus = remember { FocusRequester() }

    LaunchedEffect(id) { backFocus.requestFocus() }

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
        TvBackChip(onBack, focusRequester = backFocus)
        Spacer(Modifier.height(14.dp))

        when (val s = state) {
            is UiState.Ready ->
                TvDetailContent(
                    payload = s.value,
                    episodes = s.value.episodes.ifEmpty { episodeList.episodes },
                    onOpen = onOpen,
                    modifier = Modifier.weight(1f),
                )
            is UiState.Error -> TvErrorState(
                s.msg,
                onRetry = { viewModel.refresh() },
                modifier = Modifier.weight(1f),
            )
            else -> TvLoading(Modifier.weight(1f))
        }
    }
}

@Composable
private fun TvDetailContent(
    payload: DetailPayload,
    episodes: List<Episode>,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val movie = payload.movie
    if (movie == null) {
        TvEmptyState(
            title = "Detail tidak tersedia",
            message = "Coba buka lagi nanti.",
            modifier = modifier,
        )
        return
    }

    val bookmarks by Bookmarks.entries.collectAsStateWithLifecycle()
    val saved = movie.id?.let { bookmarks.containsKey(it) } == true

    val play: (Episode) -> Unit = { episode ->
        MiniPlayerController.open(
            epId = episode.id.orEmpty(),
            title = "${movie.title.orEmpty()} - Ep ${episode.index.orEmpty()}",
            movieId = movie.id,
            epIndex = episode.index,
        )
    }

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .width(186.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(posterBrush(movie.title)),
            ) {
                Text(
                    posterInitials(movie.title),
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                )
                AsyncImage(
                    model = movie.image_poster,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Spacer(Modifier.width(26.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    movie.title.orEmpty(),
                    color = TvColors.Text,
                    fontSize = 36.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    metaLine(movie),
                    color = TvColors.Text.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                )
                val genres = genreLine(movie)
                if (genres.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        genres,
                        color = TvColors.Accent,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                movie.synopsis?.takeIf { it.isNotBlank() }?.let { synopsis ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        synopsis,
                        color = TvColors.Muted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(0.82f),
                    )
                }
                Spacer(Modifier.height(18.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val first = payload.firstEpisode ?: episodes.firstOrNull()
                    TvActionButton(
                        label = "Tonton",
                        filled = true,
                        onClick = { first?.let(play) },
                    )
                    TvActionButton(
                        label = if (saved) "Hapus dari Pustaka" else "Simpan",
                        filled = false,
                        onClick = {
                            Bookmarks.setStatus(
                                movie,
                                if (saved) null else WatchStatus.PLAN_TO_WATCH,
                            )
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(26.dp))
        TvEpisodeRow(
            title = "Episode",
            episodes = episodes,
            onPlay = play,
        )

        val seasons = payload.seasons
        if (seasons.isNotEmpty()) {
            Spacer(Modifier.height(26.dp))
            TvPosterRow(
                title = "Season Lain",
                movies = seasons,
                onOpen = { season -> season.id?.let(onOpen) },
            )
        }
    }
}

private fun metaLine(movie: Movie): String =
    listOfNotNull(
        movie.score?.takeIf { it.isNotBlank() }?.let { "★ $it" },
        movie.type?.takeIf { it.isNotBlank() },
        movie.year?.takeIf { it.isNotBlank() },
        movie.status?.takeIf { it.isNotBlank() },
        movie.studio?.takeIf { it.isNotBlank() },
    ).joinToString("  •  ")

private fun genreLine(movie: Movie): String =
    movie.genre
        .orEmpty()
        .split(",", "•")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString("  •  ")
