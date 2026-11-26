package com.uwu.animex.ui.tv

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.uwu.animex.BuildConfig
import com.uwu.animex.core.notification.NotificationPrefs
import com.uwu.animex.core.pairing.PairingClient
import com.uwu.animex.core.pairing.PairingLink
import com.uwu.animex.core.pairing.PairingPhase
import com.uwu.animex.core.pairing.PairingServer
import com.uwu.animex.data.anilist.AniList
import com.uwu.animex.data.download.Downloads
import com.uwu.animex.data.local.AccentPalette
import com.uwu.animex.data.local.Appearance
import com.uwu.animex.data.mal.Mal
import com.uwu.animex.data.tracker.SyncSource
import com.uwu.animex.data.tracker.SyncSourcePref
import com.uwu.animex.data.update.AppUpdate
import com.uwu.animex.sync.AccountManager
import com.uwu.animex.ui.player.MiniPlayerController
import kotlinx.coroutines.launch

private enum class TvSettingsPage {
    ROOT,
    SYNC,
    PAIR,
    DOWNLOADS,
    BACKDROP,
    DISPLAY,
    ABOUT,
    ;

    fun title(): String =
        when (this) {
            ROOT -> "Setelan"
            SYNC -> "Sinkronisasi"
            PAIR -> "Pairing akun"
            DOWNLOADS -> "Unduhan"
            BACKDROP -> "Latar animasi"
            DISPLAY -> "Mode tema"
            ABOUT -> "Tentang"
        }
}

private enum class PairMode { RECEIVE, SEND }

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
internal fun TvSettingsOverlay(
    initialPair: PairingLink? = null,
    onPairConsumed: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    var page by rememberSaveable { mutableStateOf(TvSettingsPage.ROOT) }
    var pairPrefill by remember { mutableStateOf(initialPair) }
    val pageFocus = remember { FocusRequester() }

    BackHandler(enabled = page != TvSettingsPage.ROOT) { page = TvSettingsPage.ROOT }
    LaunchedEffect(page) { pageFocus.requestFocus() }
    LaunchedEffect(initialPair) {
        val link = initialPair ?: return@LaunchedEffect
        pairPrefill = link
        page = TvSettingsPage.PAIR
        onPairConsumed()
    }

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
            TvSettingsPage.SYNC ->
                SettingsSyncPage(
                    focusRequester = pageFocus,
                    onOpenPair = { page = TvSettingsPage.PAIR },
                )
            TvSettingsPage.PAIR ->
                SettingsPairPage(
                    focusRequester = pageFocus,
                    prefill = pairPrefill,
                    onPrefillTaken = { pairPrefill = null },
                )
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
    val malLogged by Mal.loggedIn.collectAsStateWithLifecycle()
    val aniLogged by AniList.loggedIn.collectAsStateWithLifecycle()

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
        label = "Pairing MAL / AniList",
        value =
            when {
                malLogged && aniLogged -> "Dua akun tersambung"
                malLogged || aniLogged -> "Satu akun tersambung"
                else -> "Kode + QR"
            },
        chevron = true,
    ) { onOpen(TvSettingsPage.PAIR) }
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
private fun SettingsSyncPage(
    focusRequester: FocusRequester,
    onOpenPair: () -> Unit,
) {
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
    TvSettingRow(
        label = "Hubungkan akun lewat pairing",
        value = "Kode + QR",
        chevron = true,
    ) { onOpenPair() }

    Spacer(Modifier.height(10.dp))
    Text(
        "Hubungkan akun sekali lewat pairing, lalu pilih sumber di atas untuk memakai pustaka daring.",
        color = TvColors.Muted,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
    )
}

@Composable
private fun SettingsPairPage(
    focusRequester: FocusRequester,
    prefill: PairingLink?,
    onPrefillTaken: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by rememberSaveable { mutableStateOf(if (prefill != null) PairMode.SEND else PairMode.RECEIVE) }
    var host by rememberSaveable { mutableStateOf(prefill?.host.orEmpty()) }
    var port by rememberSaveable { mutableStateOf((prefill?.port ?: 8787).toString()) }
    var code by rememberSaveable { mutableStateOf(prefill?.code.orEmpty()) }
    var result by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val session by PairingServer.session.collectAsStateWithLifecycle()
    val phase by PairingServer.phase.collectAsStateWithLifecycle()
    val detail by PairingServer.detail.collectAsStateWithLifecycle()
    val malLogged by Mal.loggedIn.collectAsStateWithLifecycle()
    val aniLogged by AniList.loggedIn.collectAsStateWithLifecycle()

    LaunchedEffect(prefill) {
        val link = prefill ?: return@LaunchedEffect
        host = link.host
        port = link.port.toString()
        code = link.code
        mode = PairMode.SEND
        result = "Kode dari QR sudah terisi. Kirim akun yang sudah masuk di perangkat ini."
        onPrefillTaken()
    }

    DisposableEffect(mode) {
        if (mode == PairMode.RECEIVE) PairingServer.start(context)
        onDispose {
            if (mode == PairMode.RECEIVE && PairingServer.phase.value == PairingPhase.WAITING) {
                PairingServer.stop()
            }
        }
    }

    TvRadioRow(
        label = "Terima di TV (QR + kode)",
        selected = mode == PairMode.RECEIVE,
        focusRequester = focusRequester,
    ) {
        mode = PairMode.RECEIVE
        result = null
    }
    TvRadioRow(
        label = "Kirim dari HP",
        selected = mode == PairMode.SEND,
    ) {
        mode = PairMode.SEND
        result = null
    }

    Spacer(Modifier.height(6.dp))
    Text(
        "Akun di perangkat ini: MAL ${if (malLogged) "masuk" else "belum"} • " +
            "AniList ${if (aniLogged) "masuk" else "belum"}",
        color = TvColors.Muted,
        fontSize = 13.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
    )

    if (mode == PairMode.RECEIVE) {
        when (phase) {
            PairingPhase.WAITING ->
                session?.let { current ->
                    val qr = remember(current.deepLink) { qrBitmap(current.deepLink, 360) }
                    Text(
                        "Pindai QR di HP, atau ketik manual alamat dan kode di mode Kirim dari HP.",
                        color = TvColors.Muted,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(androidx.compose.ui.graphics.Color.White)
                                .padding(8.dp),
                        ) {
                            Image(
                                bitmap = qr.asImageBitmap(),
                                contentDescription = "QR pairing MAL/AniList",
                                modifier = Modifier.size(196.dp),
                                contentScale = ContentScale.Fit,
                            )
                        }
                        Spacer(Modifier.width(18.dp))
                        Column {
                            Text("KODE PAIRING", color = TvColors.Muted, fontSize = 12.sp)
                            Text(
                                current.link.code,
                                color = TvColors.Text,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 4.sp,
                            )
                            Spacer(Modifier.height(10.dp))
                            Text("Alamat", color = TvColors.Muted, fontSize = 12.sp)
                            Text(
                                "${current.link.host}:${current.link.port}",
                                color = TvColors.Text,
                                fontSize = 15.sp,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "deep link: ${current.deepLink}",
                        color = TvColors.Muted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                    detail?.let {
                        Text(
                            it,
                            color = TvColors.Muted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        )
                    }
                }
            PairingPhase.SUCCESS -> {
                Text(
                    detail.orEmpty().ifBlank { "Pairing berhasil." },
                    color = TvColors.Text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
                TvSettingRow(
                    label = "Buat kode baru",
                    chevron = false,
                ) { PairingServer.start(context) }
            }
            PairingPhase.ERROR -> {
                Text(
                    detail.orEmpty().ifBlank { "Pairing gagal." },
                    color = TvColors.Muted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
                TvSettingRow(
                    label = "Coba lagi",
                    chevron = false,
                    focusRequester = focusRequester,
                ) { PairingServer.start(context) }
            }
            PairingPhase.IDLE ->
                Text(
                    "Menyiapkan pairing…",
                    color = TvColors.Muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
        }
    } else {
        TvPairInput(
            label = "Host TV",
            value = host,
            onValueChange = { host = it },
            placeholder = "192.168.1.10",
        )
        TvPairInput(
            label = "Port",
            value = port,
            onValueChange = { port = it },
            placeholder = "8787",
            keyboardType = KeyboardType.Number,
        )
        TvPairInput(
            label = "Kode",
            value = code,
            onValueChange = { code = it },
            placeholder = "6 digit",
            keyboardType = KeyboardType.Number,
        )
        Spacer(Modifier.height(8.dp))
        TvSettingRow(
            label = "Kirim akun ke TV",
            value = if (busy) "Mengirim…" else null,
            chevron = false,
        ) {
            val portNumber = port.toIntOrNull()
            if (host.isBlank() || code.isBlank() || portNumber == null) {
                result = "Host, port, dan kode wajib diisi dengan benar."
            } else {
                AccountManager.init(context)
                val malData = AccountManager.authData("mal")
                val aniData = AccountManager.authData("anilist")
                if (malData == null && aniData == null) {
                    result = "Belum ada akun masuk di perangkat ini. Masuk dulu lewat tombol di bawah."
                } else {
                    busy = true
                    result = "Mengirim…"
                    scope.launch {
                        val response =
                            PairingClient.send(
                                link = PairingLink(host.trim(), portNumber, code.trim()),
                                mal = malData,
                                anilist = aniData,
                            )
                        result =
                            if (response.ok) {
                                listOfNotNull(
                                    if (response.mal) "MAL" else null,
                                    if (response.anilist) "AniList" else null,
                                ).joinToString(" • ").ifBlank { "Akun" } + " tersambung di TV."
                            } else {
                                "Gagal: ${response.message.orEmpty().ifBlank { "periksa kode dan jaringan" }}"
                            }
                        busy = false
                    }
                }
            }
        }
        TvSettingRow(
            label = if (malLogged) "MAL sudah masuk" else "Masuk MAL di browser",
            value = if (malLogged) null else "OAuth",
            chevron = false,
        ) { Mal.startLogin(context) }
        TvSettingRow(
            label = if (aniLogged) "AniList sudah masuk" else "Masuk AniList di browser",
            value = if (aniLogged) null else "OAuth",
            chevron = false,
        ) { AniList.startLogin(context) }
        result?.let {
            Text(
                it,
                color = TvColors.Muted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        Text(
            "Kirim memakai HTTPS? Tidak. Pairing ini hanya untuk jaringan lokal (debug) dan kode sekali pakai.",
            color = TvColors.Muted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun TvPairInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(TvColors.SurfaceHigh)
            .border(1.dp, TvColors.Outline, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = TvColors.Muted, fontSize = 14.sp, modifier = Modifier.width(76.dp))
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(color = TvColors.Text, fontSize = 17.sp),
            cursorBrush = SolidColor(TvColors.Accent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
            decorationBox = { inner ->
                Box {
                    if (value.isBlank()) Text(placeholder, color = TvColors.Muted, fontSize = 17.sp)
                    inner()
                }
            },
        )
    }
}

private fun qrBitmap(
    value: String,
    size: Int,
): Bitmap {
    val hints =
        mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 2,
        )
    val matrix = QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, size, size, hints)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(
                x,
                y,
                if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE,
            )
        }
    }
    return bitmap
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
        fontWeight = FontWeight.Bold,
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
