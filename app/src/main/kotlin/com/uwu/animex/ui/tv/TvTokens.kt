package com.uwu.animex.ui.tv

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Palet UI 10-foot (Android TV). Mengikuti mode tema, bukan warna dinamis HP. */
internal data class TvPalette(
    val Background: Color,
    val BackgroundLift: Color,
    val Rail: Color,
    val RailBorder: Color,
    val Surface: Color,
    val SurfaceHigh: Color,
    val SurfaceLow: Color,
    val Outline: Color,
    val Text: Color,
    val Muted: Color,
    val Accent: Color,
    val AccentContainer: Color,
    val OnAccentContainer: Color,
    val FocusRing: Color,
    val FocusFill: Color,
    val FocusContent: Color,
    val ButtonIdle: Color,
    val ToggleOff: Color,
    val Purple: Color,
    val PurpleSoft: Color,
    val Scrim: Color,
)

internal val TvColorsDark =
    TvPalette(
        Background = Color(0xFF08080A),
        BackgroundLift = Color(0xFF0E0C14),
        Rail = Color(0xF20C0C0F),
        RailBorder = Color(0x14FFFFFF),
        Surface = Color(0xFF1B1A1F),
        SurfaceHigh = Color(0xFF26252B),
        SurfaceLow = Color(0xFF141317),
        Outline = Color(0xFF3B3943),
        Text = Color(0xFFF4F1F8),
        Muted = Color(0xFFB4AFBF),
        Accent = Color(0xFFB39DDB),
        AccentContainer = Color(0xFFEADDFF),
        OnAccentContainer = Color(0xFF231531),
        FocusRing = Color(0xFFFFFFFF),
        FocusFill = Color.White,
        FocusContent = Color.Black,
        ButtonIdle = Color.White.copy(alpha = 0.10f),
        ToggleOff = Color(0xFF4A4750),
        Purple = Color(0xFF6750A4),
        PurpleSoft = Color(0xFF4F378B),
        Scrim = Color(0xB3000000),
    )

internal val TvColorsLight =
    TvPalette(
        Background = Color(0xFFF7F5FA),
        BackgroundLift = Color(0xFFF1EEF7),
        Rail = Color(0xF2FFFFFF),
        RailBorder = Color(0x1A000000),
        Surface = Color(0xFFFFFFFF),
        SurfaceHigh = Color(0xFFECE8F2),
        SurfaceLow = Color(0xFFF5F2F8),
        Outline = Color(0xFFCBC5D3),
        Text = Color(0xFF1B1A1F),
        Muted = Color(0xFF5F5968),
        Accent = Color(0xFF6750A4),
        AccentContainer = Color(0xFFEADDFF),
        OnAccentContainer = Color(0xFF1F1130),
        FocusRing = Color(0xFF6750A4),
        FocusFill = Color(0xFF6750A4),
        FocusContent = Color.White,
        ButtonIdle = Color(0xFFE7E0EC),
        ToggleOff = Color(0xFFCAC4D0),
        Purple = Color(0xFF6750A4),
        PurpleSoft = Color(0xFFD0BCFF),
        Scrim = Color(0x99000000),
    )

internal val LocalTvColors = compositionLocalOf { TvColorsDark }

internal object TvDimens {
    /** Lebar rail saat fokus berada di rail (label ditampilkan). */
    val RailWidth = 240.dp

    /** Lebar rail saat fokus berada di konten (hanya ikon). */
    val RailCollapsedWidth = 82.dp
    val ContentStart = 92.dp
    val ContentStartExpanded = 250.dp
    val ContentEnd = 28.dp
    val ContentTop = 40.dp
    val ContentBottom = 62.dp
    val CardWidth = 150.dp
    val CardGap = 10.dp
    val GridGapX = 10.dp
    val GridGapY = 18.dp
    val SectionGap = 26.dp
    val PanelWidth = 700.dp
}
