package com.uwu.animex.ui.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uwu.animex.core.network.toUserMessage
import com.uwu.animex.data.api.Api
import com.uwu.animex.data.local.SearchHistory
import com.uwu.animex.data.model.Movie
import com.uwu.animex.ui.common.UiState
import com.uwu.animex.ui.viewmodel.SearchViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun TvSearchScreen(
    onOpen: (String) -> Unit,
    onBack: () -> Unit = {},
    initialQuery: String = "",
    viewModel: SearchViewModel = viewModel(),
) {
    val scope = rememberCoroutineScope()
    val searchFocus = remember { FocusRequester() }
    BackHandler(onBack = onBack)
    val history by SearchHistory.items.collectAsStateWithLifecycle()
    val suggestionsState by viewModel.suggestions.collectAsStateWithLifecycle()
    val suggestions = (suggestionsState as? UiState.Ready)?.value.orEmpty()

    var query by rememberSaveable { mutableStateOf(initialQuery) }
    var results by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var searched by remember { mutableStateOf(false) }

    fun submit(text: String = query) {
        val q = text.trim()
        if (q.isBlank()) return
        query = q
        searched = true
        loading = true
        error = null
        SearchHistory.record(q)
        scope.launch {
            try {
                results = Api.search(q)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.toUserMessage()
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank() && query != initialQuery) submit(initialQuery)
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
        TvScreenTitle(title = "Cari", subtitle = "Temukan anime favoritmu")
        Spacer(Modifier.height(18.dp))

        TvSearchBar(
            value = query,
            onValueChange = {
                query = it
                searched = false
                viewModel.onQueryChanged(it)
            },
            onSubmit = { submit() },
            modifier = Modifier.fillMaxWidth(),
            focusRequester = searchFocus,
        )

        Spacer(Modifier.height(20.dp))

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                loading -> TvLoading()
                error != null -> TvErrorState(error.orEmpty(), onRetry = { submit() })
                searched ->
                    if (results.isEmpty()) {
                        TvEmptyState(
                            title = "Tidak ditemukan",
                            message = "Coba kata kunci lain.",
                        )
                    } else {
                        TvMovieGrid(
                            movies = results,
                            modifier = Modifier.fillMaxSize(),
                            onOpen = { movie -> movie.id?.let(onOpen) },
                        )
                    }
                else ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(TvDimens.SectionGap),
                    ) {
                        if (history.isNotEmpty()) {
                            Column(Modifier.fillMaxWidth()) {
                                TvSectionTitle("Pencarian terakhir")
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    history.forEach { item ->
                                        TvChip(
                                            label = item,
                                            selected = false,
                                            onClick = { submit(item) },
                                        )
                                    }
                                }
                            }
                        }
                        TvPosterRow(
                            title = "Saran",
                            movies = suggestions,
                            onOpen = { movie -> movie.id?.let(onOpen) },
                        )
                    }
            }
        }
    }
}
