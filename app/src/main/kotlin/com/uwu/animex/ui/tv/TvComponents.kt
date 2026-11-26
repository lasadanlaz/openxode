@file:OptIn(ExperimentalFoundationApi::class)

package com.uwu.animex.ui.tv

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.uwu.animex.data.api.Api
import com.uwu.animex.data.model.Episode
import com.uwu.animex.data.model.Movie

enum class TvTab { SEARCH, HOME, SCHEDULE, EXPLORE, LIBRARY }

data class TvRailEntry(
    val tab: TvTab,
    val label: String,
    val icon: ImageVector,
)

private val posterPalette =
    listOf(
        Color(0xFF8A7A3A) to Color(0xFF4A3F17),
        Color(0xFF4E8C5A) to Color(0xFF1F3D28),
        Color(0xFF3E7FA6) to Color(0xFF1B3444),
        Color(0xFF5F9E57) to Color(0xFF243F20),
        Color(0xFF7C5FB0) to Color(0xFF2E2345),
        Color(0xFFB05F8C) to Color(0xFF452234),
        Color(0xFF4FA39B) to Color(0xFF1C3C39),
        Color(0xFFA65F5F) to Color(0xFF3F2020),
        Color(0xFFB08A4A) to Color(0xFF44341B),
        Color(0xFF5F6FB0) to Color(0xFF242B45),
    )

internal fun posterBrush(title: String?): Brush {
    val index = ((title?.hashCode() ?: 0) and 0x7FFFFFFF) % posterPalette.size
    val (a, b) = posterPalette[index]
    return Brush.linearGradient(listOf(a, b), start = Offset.Zero, end = Offset(220f, 320f))
}

internal fun posterInitials(title: String?): String {
    val words = title.orEmpty().trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        words.size >= 2 -> "${words[0].first().uppercaseChar()}${words[1].first().uppercaseChar()}"
        words.size == 1 -> words[0].take(2).uppercase()
        else -> "AX"
    }
}

@Composable
fun TvSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        title,
        modifier = modifier,
        color = TvColors.Text,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
fun TvScreenTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(modifier) {
        Text(title, color = TvColors.Text, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = TvColors.Muted, fontSize = 14.sp)
        }
    }
}

@Composable
fun TvRailItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    expanded: Boolean,
    focusRequester: FocusRequester? = null,
    onFocus: (Boolean) -> Unit = {},
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val highlighted = focused || (selected && !expanded)
    val background =
        when {
            highlighted -> Color.White
            else -> Color.Transparent
        }
    val content =
        when {
            highlighted -> Color.Black
            selected -> TvColors.Text
            else -> TvColors.Muted
        }
    Row(
        modifier =
            Modifier
                .height(52.dp)
                .then(if (expanded) Modifier.fillMaxWidth() else Modifier.width(52.dp))
                .clip(RoundedCornerShape(50))
                .background(background)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { onFocus(it.isFocused) }
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .padding(horizontal = if (expanded) 16.dp else 0.dp),
        horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = content, modifier = Modifier.size(22.dp))
        if (expanded) {
            Spacer(Modifier.width(14.dp))
            Text(
                label,
                color = content,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun TvRail(
    entries: List<TvRailEntry>,
    selected: TvTab,
    onSelect: (TvTab) -> Unit,
    onSettings: () -> Unit,
    homeFocus: FocusRequester? = null,
    onExpandedChange: (Boolean) -> Unit = {},
) {
    var focusedTab by remember { mutableStateOf<TvTab?>(null) }
    var settingsFocused by remember { mutableStateOf(false) }
    val expanded = focusedTab != null || settingsFocused

    LaunchedEffect(expanded) { onExpandedChange(expanded) }

    Column(
        Modifier
            .width(if (expanded) TvDimens.RailWidth else TvDimens.RailCollapsedWidth)
            .fillMaxHeight()
            .background(if (expanded) TvColors.Rail else Color.Transparent)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.Start,
    ) {
        entries.forEach { entry ->
            TvRailItem(
                label = entry.label,
                icon = entry.icon,
                selected = selected == entry.tab,
                expanded = expanded,
                focusRequester = if (entry.tab == TvTab.HOME) homeFocus else null,
                onFocus = { focused ->
                    if (focused) {
                        focusedTab = entry.tab
                    } else if (focusedTab == entry.tab) {
                        focusedTab = null
                    }
                },
            ) { onSelect(entry.tab) }
        }
        TvRailItem(
            label = "Setelan",
            icon = Icons.Outlined.Settings,
            selected = false,
            expanded = expanded,
            onFocus = { settingsFocused = it },
            onClick = onSettings,
        )
    }
}

@Composable
fun TvPosterCard(
    movie: Movie,
    modifier: Modifier = Modifier,
    badge: String? = null,
    onFocused: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val domain = Api.activeDomain
    val poster = remember(movie.image_poster, domain) { Api.absUrl(movie.image_poster) }
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(14.dp))
                .background(posterBrush(movie.title))
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .onFocusChanged { state -> if (state.isFocused) onFocused?.invoke() },
        ) {
            Text(
                posterInitials(movie.title),
                modifier = Modifier.align(Alignment.Center),
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
            )
            AsyncImage(
                model = poster,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            if (badge != null) {
                Text(
                    badge,
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(TvColors.Purple)
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
            if (focused) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .border(3.dp, TvColors.FocusRing, RoundedCornerShape(14.dp)),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            movie.title.orEmpty(),
            color = TvColors.Text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp,
        )
    }
}

@Composable
fun TvPosterRow(
    title: String,
    movies: List<Movie>,
    onOpen: (Movie) -> Unit,
    onFocused: (Movie) -> Unit = {},
) {
    val unique = remember(movies) { movies.filter { !it.id.isNullOrBlank() }.distinctBy { it.id } }
    if (unique.isEmpty()) return
    Column(Modifier.fillMaxWidth()) {
        TvSectionTitle(title)
        Spacer(Modifier.height(10.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TvDimens.CardGap),
        ) {
            items(unique, key = { it.id ?: it.title.orEmpty() }) { movie ->
                TvPosterCard(
                    movie = movie,
                    modifier = Modifier.width(TvDimens.CardWidth),
                    badge = movie.episode_index?.takeIf { it.isNotBlank() }?.let { "Ep $it" },
                    onFocused = { onFocused(movie) },
                ) { onOpen(movie) }
            }
        }
    }
}

@Composable
fun TvEpisodeRow(
    title: String,
    episodes: List<Episode>,
    onPlay: (Episode) -> Unit,
    fallbackImage: String? = null,
) {
    if (episodes.isEmpty()) return
    Column(Modifier.fillMaxWidth()) {
        TvSectionTitle(title)
        Spacer(Modifier.height(10.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TvDimens.CardGap),
        ) {
            items(episodes, key = { it.id ?: (it.index.orEmpty() + "#" + it.hashCode()) }) { ep ->
                TvEpisodeCard(ep, fallbackImage) { onPlay(ep) }
            }
        }
    }
}

@Composable
fun TvEpisodeCard(
    ep: Episode,
    fallbackImage: String? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val label = ep.index?.takeIf { it.isNotBlank() }?.let { "Ep $it" } ?: "Episode"
    val domain = Api.activeDomain
    val image =
        remember(ep.image, fallbackImage, domain) {
            Api.absUrl(
                ep.image?.takeIf { it.isNotBlank() }
                    ?: fallbackImage?.takeIf { it.isNotBlank() },
            )
        }
    Column(Modifier.width(252.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(14.dp))
                .background(posterBrush(ep.title ?: label))
                .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        ) {
            Text(
                label,
                modifier = Modifier.align(Alignment.Center),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            AsyncImage(
                model = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Text(
                label,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(TvColors.Purple)
                        .padding(horizontal = 9.dp, vertical = 3.dp),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            if (focused) {
                Box(Modifier.fillMaxSize().border(3.dp, TvColors.FocusRing, RoundedCornerShape(14.dp)))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            ep.title?.takeIf { it.isNotBlank() } ?: "Episode ${ep.index.orEmpty()}",
            color = TvColors.Text,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun TvHero(movie: Movie?) {
    if (movie == null) {
        Column(Modifier.fillMaxWidth().heightIn(min = 220.dp)) {
            Text("AnimeX TV", color = TvColors.Text, fontSize = 46.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Pilih anime untuk mulai nonton.", color = TvColors.Muted, fontSize = 16.sp)
        }
        return
    }
    val meta =
        listOfNotNull(
            movie.score?.takeIf { it.isNotBlank() }?.let { "★ $it" },
            movie.type?.takeIf { it.isNotBlank() },
            movie.year?.takeIf { it.isNotBlank() },
            movie.status?.takeIf { it.isNotBlank() },
        ).joinToString("  •  ")
    val genres = movie.genre.orEmpty().split(",", "•").map { it.trim() }.filter { it.isNotBlank() }.joinToString("  •  ")
    Column(Modifier.fillMaxWidth().heightIn(min = 196.dp)) {
        Text(
            movie.title.orEmpty(),
            color = TvColors.Text,
            fontSize = 44.sp,
            lineHeight = 48.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (meta.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(meta, color = TvColors.Text.copy(alpha = 0.85f), fontSize = 15.sp)
        }
        if (genres.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(genres, color = TvColors.Accent, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        movie.synopsis?.takeIf { it.isNotBlank() }?.let { synopsis ->
            Spacer(Modifier.height(10.dp))
            Text(
                synopsis,
                color = TvColors.Muted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.62f),
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun TvActionButton(
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val background =
        when {
            filled && focused -> Color.White
            filled -> TvColors.AccentContainer
            focused -> Color.White
            else -> Color.White.copy(alpha = 0.10f)
        }
    val content =
        when {
            filled -> TvColors.OnAccentContainer
            focused -> Color.Black
            else -> TvColors.Text
        }
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 11.dp),
    ) {
        Text(label, color = content, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun TvChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val background =
        when {
            selected -> TvColors.Purple
            focused -> Color.White
            else -> TvColors.SurfaceHigh
        }
    val content =
        when {
            selected -> Color.White
            focused -> Color.Black
            else -> TvColors.Text
        }
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .then(
                if (focused && !selected) {
                    Modifier.border(2.dp, Color.White, RoundedCornerShape(50))
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 18.dp, vertical = 9.dp),
    ) {
        Text(label, color = content, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun TvMovieGrid(
    movies: List<Movie>,
    modifier: Modifier = Modifier,
    badge: (Movie) -> String? = { null },
    onFocused: (Movie) -> Unit = {},
    onOpen: (Movie) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(100.dp),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(TvDimens.GridGapX),
        verticalArrangement = Arrangement.spacedBy(TvDimens.GridGapY),
    ) {
        items(movies, key = { it.id ?: (it.title.orEmpty() + "#" + it.hashCode()) }) { movie ->
            TvPosterCard(
                movie = movie,
                modifier = Modifier.fillMaxWidth(),
                badge = badge(movie),
                onFocused = { onFocused(movie) },
            ) { onOpen(movie) }
        }
    }
}

@Composable
fun TvSettingRow(
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    toggle: Boolean? = null,
    chevron: Boolean = false,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val background = if (focused) TvColors.AccentContainer else Color.Transparent
    val content = if (focused) TvColors.OnAccentContainer else TvColors.Text
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(background)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = if (enabled) content else content.copy(alpha = 0.4f),
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
        )
        when {
            toggle != null -> TvToggle(toggle, focused)
            value != null -> {
                Text(
                    value,
                    color = if (focused) TvColors.OnAccentContainer.copy(alpha = 0.8f) else TvColors.Muted,
                    fontSize = 14.sp,
                )
                if (chevron) {
                    Spacer(Modifier.width(10.dp))
                    Text("›", color = content, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            chevron -> Text("›", color = content, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TvRadioRow(
    label: String,
    selected: Boolean,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (focused) TvColors.AccentContainer else Color.Transparent)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = if (focused) TvColors.OnAccentContainer else TvColors.Text,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(2.dp, if (focused) TvColors.OnAccentContainer else TvColors.Muted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (focused) TvColors.OnAccentContainer else TvColors.AccentContainer),
                )
            }
        }
    }
}

@Composable
private fun TvToggle(
    checked: Boolean,
    focused: Boolean,
) {
    Box(
        Modifier
            .width(44.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(50))
            .background(
                when {
                    checked && focused -> TvColors.PurpleSoft
                    checked -> TvColors.Purple
                    else -> Color(0xFF4A4750)
                },
            )
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
fun TvPanel(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(TvColors.Scrim).padding(top = 24.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier
                .width(TvDimens.PanelWidth)
                .fillMaxHeight(0.78f)
                .heightIn(max = 640.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(TvColors.Surface)
                .border(1.dp, TvColors.Outline, RoundedCornerShape(28.dp))
                .then(
                    if (focusRequester != null) {
                        Modifier.focusRequester(focusRequester).focusable()
                    } else {
                        Modifier
                    },
                )
                .padding(vertical = 20.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    TvBackChip(onBack)
                    Spacer(Modifier.width(12.dp))
                }
                Text(title, color = TvColors.Text, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                content = content,
            )
        }
    }
}

@Composable
fun TvBackChip(
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (focused) TvColors.AccentContainer else Color.White.copy(alpha = 0.08f))
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Kembali",
            tint = if (focused) TvColors.OnAccentContainer else TvColors.Text,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun TvLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Memuat…", color = TvColors.Muted, fontSize = 16.sp)
    }
}

@Composable
fun TvErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Gagal memuat", color = TvColors.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message, color = TvColors.Muted, fontSize = 14.sp)
        Spacer(Modifier.height(18.dp))
        TvActionButton("Coba lagi", filled = true, onClick = onRetry)
    }
}

@Composable
fun TvEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = TvColors.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(message, color = TvColors.Muted, fontSize = 14.sp)
    }
}
