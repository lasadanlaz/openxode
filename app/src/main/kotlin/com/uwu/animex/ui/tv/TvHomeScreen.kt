package com.uwu.animex.ui.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uwu.animex.data.model.HomeData
import com.uwu.animex.ui.common.UiState
import com.uwu.animex.ui.viewmodel.HomeViewModel

@Composable
fun TvHomeScreen(
    onOpen: (String) -> Unit,
    onSearch: (String) -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.revalidate() }

    when (val s = state) {
        is UiState.Ready -> TvHomeContent(s.value, onOpen, onSearch)
        is UiState.Error -> TvErrorState(s.msg, onRetry = { viewModel.refresh() })
        else -> TvLoading()
    }
}

@Composable
private fun TvHomeContent(
    data: HomeData,
    onOpen: (String) -> Unit,
    onSearch: (String) -> Unit,
) {
    val rows =
        remember(data) {
            buildList {
                add("Lanjut Nonton" to data.history)
                add("Episode Baru" to data.update)
                add("Populer" to data.popular)
                add("Sedang Hangat" to data.hot)
                add("Anime Baru" to data.new)
                add("Rilis Hari Ini" to data.today)
            }.filter { (_, list) -> list.isNotEmpty() }
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
        TvSearchBar(
            value = "",
            onValueChange = {},
            onSubmit = { onSearch("") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
        )
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(TvDimens.SectionGap),
        ) {
            rows.forEach { (title, movies) ->
                TvPosterRow(
                    title = title,
                    movies = movies,
                    onOpen = { movie -> movie.id?.let(onOpen) },
                )
            }
        }
    }
}
