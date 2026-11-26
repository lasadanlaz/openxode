package com.uwu.animex.ui.tv

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Palet tetap untuk UI 10-foot (Android TV), tidak ikut warna dinamis HP. */
internal object TvColors {
    val Background = Color(0xFF08080A)
    val BackgroundLift = Color(0xFF0E0C14)
    val Rail = Color(0xF20C0C0F)
    val RailBorder = Color(0x14FFFFFF)
    val Surface = Color(0xFF1B1A1F)
    val SurfaceHigh = Color(0xFF26252B)
    val SurfaceLow = Color(0xFF141317)
    val Outline = Color(0xFF3B3943)
    val Text = Color(0xFFF4F1F8)
    val Muted = Color(0xFFB4AFBF)
    val Accent = Color(0xFFB39DDB)
    val AccentContainer = Color(0xFFEADDFF)
    val OnAccentContainer = Color(0xFF231531)
    val FocusRing = Color(0xFFFFFFFF)
    val Purple = Color(0xFF6750A4)
    val PurpleSoft = Color(0xFF4F378B)
    val Scrim = Color(0xB3000000)
}

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
    val CardWidth = 114.dp
    val CardGap = 10.dp
    val GridGapX = 10.dp
    val GridGapY = 18.dp
    val SectionGap = 26.dp
    val PanelWidth = 700.dp
}
