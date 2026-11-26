package com.uwu.animex.ui.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uwu.animex.BuildConfig
import com.uwu.animex.core.notification.NotificationPrefs
import com.uwu.animex.data.anilist.AniList
import com.uwu.animex.data.download.Downloads
import com.uwu.animex.data.local.AccentPalette
import com.uwu.animex.data.local.Appearance
import com.uwu.animex.data.mal.Mal
import com.uwu.animex.data.tracker.SyncSource
import com.uwu.animex.data.tracker.SyncSourcePref
import com.uwu.animex.data.update.AppUpdate
import com.uwu.animex.ui.player.MiniPlayerController
import kotlinx.coroutines.launch

private enum class TvSettingsPage {
    ROOT,
    SYNC,
    DOWNLOADS,
    BACKDROP,
    DISPLAY,
    ABOUT,
    ;

    fun title(): String =
        when (this) {
            ROOT -> "Setelan"
            SYNC -> "Sinkronisasi"
            DOWNLOADS -> "Unduhan"
            BACKDROP -> "Latar animasi"
            DISPLAY -> "Mode tema"
            ABOUT -> "Tentang"
        }
}

private val tvAccents =
    listOf(
        AccentPalette.DEFAULT,
        AccentPalette.PURPLE,
        AccentPalette.BLUE,
        AccentPalette.GREEN,
        AccentPalette.PINK,
        AccentPalette.RED,
        AccentPalette.ORANGE,
        AccentPalette.TEAL,
    )

@Composable
fun TvSettingsOverlay(onDismiss: () -> Unit) {
    var page by rememberSaveable { mutableStateOf(TvSettingsPage.ROOT) }
    val pageFocus = remember { FocusRequester() }

    BackHandler(enabled = page != TvSettingsPage.ROOT) { page = TvSettingsPage.ROOT }
    LaunchedEffect(page) { pageFocus.requestFocus() }

    TvPanel(
        title = page.title(),
        onBack = if (page == TvSettingsPage.ROOT) null else ({ page = TvSettingsPage.ROOT }),
    ) {
        when (page) {
            TvSettingsPage.ROOT ->
                SettingsRootPage(
                    focusRequester = pageFocus,
                    onDismiss = onDismiss,
                    onOpen = { page = it },
                )
            TvSettingsPage.SYNC -> SettingsSyncPage(focusRequester = pageFocus)
            TvSettingsPage.DOWNLOADS ->
                SettingsDownloadsPage(
                    focusRequester = pageFocus,
                    onBack = { page = TvSettingsPage.ROOT },
                )
            TvSettingsPage.BACKDROP -> SettingsBackdropPage(focusRequester = pageFocus)
            TvSettingsPage.DISPLAY -> SettingsDisplayPage(focusRequester = pageFocus)
            TvSettingsPage.ABOUT -> SettingsAboutPage(focusRequester = pageFocus)
        }
    }
}

@Composable
private fun SettingsRootPage(
    focusRequester: FocusRequester,
    onDismiss: () -> Unit,
    onOpen: (TvSettingsPage) -> Unit,
) {
    val backdrop by TvPrefs.backdrop.collectAsStateWithLifecycle()
    val source by SyncSourcePref.source.collectAsStateWithLifecycle()
    val downloads by Downloads.items.collectAsStateWithLifecycle()
    val appearance by Appearance.settings.collectAsStateWithLifecycle()
    val notifications by NotificationPrefs.state.collectAsStateWithLifecycle()

    TvSettingRow(
        label = "Mode tema",
        value = "Gelap",
        chevron = true,
        focusRequester = focusRequester,
    ) { onOpen(TvSettingsPage.DISPLAY) }
    TvSettingRow(
        label = "Warna dinamis",
        toggle = appearance.dynamicColor,
    ) { Appearance.setDynamicColor(!appearance.dynamicColor) }
    TvSettingRow(
        label = "Latar animasi",
        value = backdrop.label,
        chevron = true,
    ) { onOpen(TvSettingsPage.BACKDROP) }
    TvSettingRow(
        label = "Sinkronisasi",
        value = source.label,
        chevron = true,
    ) { onOpen(TvSettingsPage.SYNC) }
    TvSettingRow(
        label = "Unduhan",
        value = if (downloads.isEmpty()) "Kosong" else "${downloads.size} item",
        chevron = true,
    ) { onOpen(TvSettingsPage.DOWNLOADS) }
    TvSettingRow(
        label = "Notifikasi episode",
        toggle = notifications.episodes,
    ) { NotificationPrefs.update { copy(episodes = !episodes) } }
    TvSettingRow(
        label = "Tentang",
        value = "Versi ${BuildConfig.VERSION_NAME}",
        chevron = true,
    ) { onOpen(TvSettingsPage.ABOUT) }
    Spacer(Modifier.height(6.dp))
    TvSettingRow(label = "Tutup setelan", chevron = false, onClick = onDismiss)
}

@Composable
private fun SettingsSyncPage(focusRequester: FocusRequester) {
    val source by SyncSourcePref.source.collectAsStateWithLifecycle()
    val mirror by SyncSourcePref.mirror.collectAsStateWithLifecycle()
    val malLogged by Mal.loggedIn.collectAsStateWithLifecycle()
    val aniLogged by AniList.loggedIn.collectAsStateWithLifecycle()

    TvRadioRow(
        label = "Lokal (tanpa akun)",
        selected = source == SyncSource.LOCAL,
        focusRequester = focusRequester,
    ) { SyncSourcePref.set(SyncSource.LOCAL) }
    TvRadioRow(
        label = "MyAnimeList" + if (malLogged) " • Masuk" else " • Belum masuk",
        selected = source == SyncSource.MAL,
    ) { SyncSourcePref.set(SyncSource.MAL) }
    TvRadioRow(
        label = "AniList" + if (aniLogged) " • Masuk" else " • Belum masuk",
        selected = source == SyncSource.ANILIST,
    ) { SyncSourcePref.set(SyncSource.ANILIST) }

    Spacer(Modifier.height(10.dp))
    TvSettingRow(
        label = "Cerminkan status tontonan",
        toggle = mirror,
    ) { SyncSourcePref.setMirror(!mirror) }

    Spacer(Modifier.height(10.dp))
    Text(
        "Akun MAL/AniList perlu dihubungkan sekali dari aplikasi pendamping. " +
            "Setelah masuk, pilih sumber di atas untuk memakai pustaka daring.",
        color = TvColors.Muted,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
    )
}

@Composable
private fun SettingsDownloadsPage(
    focusRequester: FocusRequester,
    onBack: () -> Unit,
) {
    val downloads by Downloads.items.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (downloads.isEmpty()) {
        Text(
            "Belum ada unduhan. Unduhan dibuat dari halaman episode.",
            color = TvColors.Muted,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        )
        TvSettingRow(
            label = "Kembali",
            chevron = false,
            focusRequester = focusRequester,
            onClick = onBack,
        )
        return
    }

    downloads.values
        .sortedBy { it.meta.movieTitle.orEmpty() }
        .forEachIndexed { index, item ->
            val title =
                listOfNotNull(
                    item.meta.movieTitle?.takeIf { it.isNotBlank() },
                    item.meta.epIndex?.takeIf { it.isNotBlank() }?.let { "Ep $it" },
                ).joinToString(" • ").ifBlank { item.id }
            TvSettingRow(
                label = title,
                value = downloadStatus(item),
                chevron = false,
                focusRequester = if (index == 0) focusRequester else null,
            ) {
                if (item.status == Downloads.Status.COMPLETED) {
                    MiniPlayerController.open(
                        epId = item.id,
                        title = title,
                        movieId = item.meta.movieId,
                        epIndex = item.meta.epIndex,
                    )
                } else {
                    Downloads.remove(context, item.id)
                }
            }
        }

    Spacer(Modifier.height(8.dp))
    Text(
        "OK pada unduhan selesai untuk memutar. OK pada unduhan berjalan untuk membatalkan.",
        color = TvColors.Muted,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
    )
    TvSettingRow(
        label = "Kembali",
        chevron = false,
        onClick = onBack,
    )
}

@Composable
private fun SettingsBackdropPage(focusRequester: FocusRequester) {
    val backdrop by TvPrefs.backdrop.collectAsStateWithLifecycle()
    TvBackdropStyle.entries.forEachIndexed { index, style ->
        TvRadioRow(
            label = style.label,
            selected = backdrop == style,
            focusRequester = if (index == 0) focusRequester else null,
        ) { TvPrefs.setBackdrop(style) }
        Text(
            style.description,
            color = TvColors.Muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 6.dp),
        )
    }
}

@Composable
private fun SettingsDisplayPage(focusRequester: FocusRequester) {
    val settings by Appearance.settings.collectAsStateWithLifecycle()

    TvSettingRow(
        label = "Mode gelap",
        value = "Selalu aktif di TV",
        enabled = false,
    ) {}
    TvSettingRow(
        label = "Warna dinamis",
        toggle = settings.dynamicColor,
        focusRequester = focusRequester,
    ) { Appearance.setDynamicColor(!settings.dynamicColor) }
    TvSettingRow(
        label = "Hitam AMOLED",
        toggle = settings.amoled,
    ) { Appearance.setAmoled(!settings.amoled) }

    Spacer(Modifier.height(12.dp))
    Text(
        "Warna aksen",
        color = TvColors.Muted,
        fontSize = 13.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
    )
    tvAccents.forEach { accent ->
        TvRadioRow(
            label = accent.label,
            selected = settings.accent == accent,
        ) { Appearance.setAccent(accent) }
    }
}

@Composable
private fun SettingsAboutPage(focusRequester: FocusRequester) {
    val updateState by AppUpdate.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Text(
        "AnimeX TV",
        color = TvColors.Text,
        fontSize = 22.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
    Text(
        "Versi ${BuildConfig.VERSION_NAME}",
        color = TvColors.Muted,
        fontSize = 14.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
    )
    Spacer(Modifier.height(12.dp))
    TvSettingRow(
        label = "Periksa pembaruan",
        value = updateLabel(updateState),
        chevron = false,
        focusRequester = focusRequester,
    ) { scope.launch { AppUpdate.check() } }

    if (updateState is AppUpdate.State.Error) {
        Text(
            (updateState as AppUpdate.State.Error).message,
            color = TvColors.Muted,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        )
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "Tampilan 10-foot untuk Android TV. Semua animasi dimatikan.",
        color = TvColors.Muted,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
    )
}

private fun downloadStatus(item: Downloads.Item): String =
    when (item.status) {
        Downloads.Status.QUEUED -> "Menunggu"
        Downloads.Status.DOWNLOADING -> "${item.percent.toInt()}%"
        Downloads.Status.PAUSED -> "Dijeda"
        Downloads.Status.COMPLETED -> "Selesai"
        Downloads.Status.FAILED -> "Gagal"
    }

private fun updateLabel(state: AppUpdate.State): String =
    when (state) {
        AppUpdate.State.Idle -> "Terbaru"
        AppUpdate.State.Checking -> "Memeriksa…"
        is AppUpdate.State.Available -> "Tersedia ${state.release.tag}"
        is AppUpdate.State.Downloading -> "Mengunduh ${(state.progress * 100).toInt()}%"
        is AppUpdate.State.Ready -> "Siap dipasang"
        is AppUpdate.State.Error -> "Gagal"
    }
