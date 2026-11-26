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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
        color = LocalTvColors.current.Text,
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
        Text(title, color = LocalTvColors.current.Text, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = LocalTvColors.current.Muted, fontSize = 14.sp)
        }
    }
}

@Composable
fun TvRailItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    expanded: Boolean = false,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    onFocus: (Boolean) -> Unit = {},
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    LaunchedEffect(focused) { onFocus(focused) }
    val colors = LocalTvColors.current
    val highlighted = enabled && focused
    val background = if (highlighted) colors.FocusFill else Color.Transparent
    val content =
        when {
            highlighted -> colors.FocusContent
            selected -> colors.Text
            else -> colors.Muted
        }
    Row(
        modifier =
            Modifier
                .height(40.dp)
                .width(if (expanded) TvDimens.RailWidth else 52.dp)
                .clip(RoundedCornerShape(50))
                .background(background)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
                .padding(horizontal = if (expanded) 12.dp else 0.dp),
        horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = content, modifier = Modifier.size(22.dp))
        if (expanded) {
            Spacer(Modifier.width(12.dp))
            Text(
                label,
                color = content,
                fontSize = 14.sp,
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
    focusRequesters: Map<TvTab, FocusRequester>,
    enabled: Boolean = true,
    expanded: Boolean = false,
    onFocusChange: (Boolean) -> Unit = {},
) {
    var focusedTab by remember { mutableStateOf<TvTab?>(null) }
    var settingsFocused by remember { mutableStateOf(false) }

    LaunchedEffect(enabled) {
        if (!enabled) {
            focusedTab = null
            settingsFocused = false
        }
    }
    LaunchedEffect(focusedTab, settingsFocused) {
        onFocusChange(enabled && (focusedTab != null || settingsFocused))
    }

    Column(
        Modifier
            .fillMaxHeight()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.Start,
    ) {
        entries.forEach { entry ->
            TvRailItem(
                label = entry.label,
                icon = entry.icon,
                selected = selected == entry.tab,
                expanded = expanded,
                enabled = enabled,
                focusRequester = focusRequesters[entry.tab],
                onFocus = { focused ->
                    if (focused && enabled) {
                        focusedTab = entry.tab
                    } else if (!focused && focusedTab == entry.tab) {
                        focusedTab = null
                    }
                },
                onClick = { if (enabled) onSelect(entry.tab) },
            )
        }
        TvRailItem(
            label = "Setelan",
            icon = Icons.Outlined.Settings,
            selected = false,
            expanded = expanded,
            enabled = enabled,
            onFocus = { focused -> settingsFocused = focused && enabled },
            onClick = { if (enabled) onSettings() },
        )
    }
}

@Composable
fun TvPosterCard(
    movie: Movie,
    modifier: Modifier = Modifier,
    badge: String? = null,
    onFocused: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val domain = Api.activeDomain
    val poster = remember(movie.image_poster, domain) { Api.absUrl(movie.image_poster) }
    LaunchedEffect(focusRequester) { focusRequester?.requestFocus() }
    Column(modifier) {
        Box(
            Modifier
                .then(
                    if (focusRequester != null) {
                        Modifier.focusRequester(focusRequester)
                    } else {
                        Modifier
                    },
                )
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
                            .background(LocalTvColors.current.Purple)
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
                        .border(3.dp, LocalTvColors.current.FocusRing, RoundedCornerShape(14.dp)),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            movie.title.orEmpty(),
            color = LocalTvColors.current.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 17.sp,
        )
    }
}

@Composable
fun TvPosterRow(
    title: String,
    movies: List<Movie>,
    onOpen: (Movie) -> Unit,
    onFocused: (Movie) -> Unit = {},
    firstCardFocusRequester: FocusRequester? = null,
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
            itemsIndexed(unique, key = { _, movie -> movie.id ?: movie.title.orEmpty() }) { index, movie ->
                TvPosterCard(
                    movie = movie,
                    modifier = Modifier.width(TvDimens.CardWidth),
                    badge = movie.episode_index?.takeIf { it.isNotBlank() }?.let { "Ep $it" },
                    onFocused = { onFocused(movie) },
                    focusRequester = if (index == 0) firstCardFocusRequester else null,
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
                        .background(LocalTvColors.current.Purple)
                        .padding(horizontal = 9.dp, vertical = 3.dp),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            if (focused) {
                Box(Modifier.fillMaxSize().border(3.dp, LocalTvColors.current.FocusRing, RoundedCornerShape(14.dp)))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            ep.title?.takeIf { it.isNotBlank() } ?: "Episode ${ep.index.orEmpty()}",
            color = LocalTvColors.current.Text,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun TvSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    focusRequester: FocusRequester? = null,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val interaction = remember { MutableInteractionSource() }
    val rowFocused by interaction.collectIsFocusedAsState()
    var fieldFocused by remember { mutableStateOf(false) }
    val focused = if (readOnly) rowFocused else fieldFocused
    LaunchedEffect(focusRequester) { focusRequester?.requestFocus() }
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(LocalTvColors.current.SurfaceHigh)
            .border(
                width = if (focused) 3.dp else 1.dp,
                color = if (focused) LocalTvColors.current.FocusRing else LocalTvColors.current.Outline,
                shape = RoundedCornerShape(50),
            )
            .then(
                if (readOnly) {
                    val focus =
                        if (focusRequester != null) {
                            Modifier.focusRequester(focusRequester)
                        } else {
                            Modifier
                        }
                    focus.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onSubmit,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = LocalTvColors.current.Muted)
        Spacer(Modifier.width(14.dp))
        if (readOnly) {
            Text(
                value.takeIf { it.isNotBlank() } ?: "Cari anime…",
                color = if (value.isBlank()) LocalTvColors.current.Muted else LocalTvColors.current.Text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            )
        } else {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier =
                    Modifier
                        .then(
                            if (focusRequester != null) {
                                Modifier.focusRequester(focusRequester)
                            } else {
                                Modifier
                            },
                        )
                        .weight(1f)
                        .onFocusChanged { fieldFocused = it.isFocused },
                singleLine = true,
                textStyle =
                    TextStyle(
                        color = LocalTvColors.current.Text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                cursorBrush = SolidColor(LocalTvColors.current.Accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions =
                    KeyboardActions(
                        onSearch = {
                            keyboard?.hide()
                            onSubmit()
                        },
                    ),
                decorationBox = { inner ->
                    Box {
                        if (value.isBlank()) {
                            Text("Ketik judul anime…", color = LocalTvColors.current.Muted, fontSize = 18.sp)
                        }
                        inner()
                    }
                },
            )
        }
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
    val colors = LocalTvColors.current
    val background =
        when {
            filled && focused -> colors.FocusFill
            filled -> colors.AccentContainer
            focused -> colors.FocusFill
            else -> colors.ButtonIdle
        }
    val content =
        when {
            filled -> colors.OnAccentContainer
            focused -> colors.FocusContent
            else -> colors.Text
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
    focusRequester: FocusRequester? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    LaunchedEffect(focusRequester) { focusRequester?.requestFocus() }
    val colors = LocalTvColors.current
    val background =
        when {
            selected -> colors.Purple
            focused -> colors.FocusFill
            else -> colors.SurfaceHigh
        }
    val content =
        when {
            selected -> Color.White
            focused -> colors.FocusContent
            else -> colors.Text
        }
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .then(
                if (focused && !selected) {
                    Modifier.border(2.dp, colors.FocusRing, RoundedCornerShape(50))
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
        columns = GridCells.Adaptive(150.dp),
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
    val background = if (focused) LocalTvColors.current.AccentContainer else Color.Transparent
    val content = if (focused) LocalTvColors.current.OnAccentContainer else LocalTvColors.current.Text
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
                    color = if (focused) LocalTvColors.current.OnAccentContainer.copy(alpha = 0.8f) else LocalTvColors.current.Muted,
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
            .background(if (focused) LocalTvColors.current.AccentContainer else Color.Transparent)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = if (focused) LocalTvColors.current.OnAccentContainer else LocalTvColors.current.Text,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(2.dp, if (focused) LocalTvColors.current.OnAccentContainer else LocalTvColors.current.Muted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (focused) LocalTvColors.current.OnAccentContainer else LocalTvColors.current.AccentContainer),
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
                    checked && focused -> LocalTvColors.current.PurpleSoft
                    checked -> LocalTvColors.current.Purple
                    else -> LocalTvColors.current.ToggleOff
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
        Modifier.fillMaxSize().background(LocalTvColors.current.Scrim).padding(top = 24.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier
                .width(TvDimens.PanelWidth)
                .fillMaxHeight(0.78f)
                .heightIn(max = 640.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(LocalTvColors.current.Surface)
                .border(1.dp, LocalTvColors.current.Outline, RoundedCornerShape(28.dp))
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
                Text(title, color = LocalTvColors.current.Text, fontSize = 27.sp, fontWeight = FontWeight.Bold)
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
            .background(if (focused) LocalTvColors.current.AccentContainer else LocalTvColors.current.ButtonIdle)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Kembali",
            tint = if (focused) LocalTvColors.current.OnAccentContainer else LocalTvColors.current.Text,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun TvLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Memuat…", color = LocalTvColors.current.Muted, fontSize = 16.sp)
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
        Text("Gagal memuat", color = LocalTvColors.current.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message, color = LocalTvColors.current.Muted, fontSize = 14.sp)
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
        Text(title, color = LocalTvColors.current.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(message, color = LocalTvColors.current.Muted, fontSize = 14.sp)
    }
}
