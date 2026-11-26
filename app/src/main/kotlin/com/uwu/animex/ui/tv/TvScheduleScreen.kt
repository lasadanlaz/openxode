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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uwu.animex.ui.common.UiState
import com.uwu.animex.ui.viewmodel.ScheduleViewModel
import java.util.Calendar

private val tvDays = listOf("SENIN", "SELASA", "RABU", "KAMIS", "JUMAT", "SABTU", "MINGGU")

private fun todayIndex(): Int = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7

@Composable
fun TvScheduleScreen(
    onOpen: (String) -> Unit,
    viewModel: ScheduleViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf(todayIndex()) }
    val firstChipFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { viewModel.revalidate() }

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
            title = "Jadwal",
            subtitle = "Rilis anime per hari dalam seminggu",
        )
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            tvDays.forEachIndexed { index, day ->
                TvChip(
                    label = day.lowercase().replaceFirstChar { it.uppercase() },
                    selected = selected == index,
                    onClick = { selected = index },
                    focusRequester = if (index == 0) firstChipFocus else null,
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        when (val s = state) {
            is UiState.Ready -> {
                val movies =
                    s.value
                        .filter { it.day.equals(tvDays[selected], ignoreCase = true) }
                        .distinctBy { it.id }
                if (movies.isEmpty()) {
                    TvEmptyState(
                        title = "Belum ada jadwal",
                        message = "Tidak ada anime yang rilis pada hari ini.",
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    TvMovieGrid(
                        movies = movies,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        badge = { it.time?.takeIf { t -> t.isNotBlank() } },
                        onOpen = { movie -> movie.id?.let(onOpen) },
                    )
                }
            }
            is UiState.Error -> TvErrorState(s.msg, onRetry = { viewModel.refresh() }, modifier = Modifier.weight(1f))
            else -> TvLoading(Modifier.weight(1f))
        }
    }
}
