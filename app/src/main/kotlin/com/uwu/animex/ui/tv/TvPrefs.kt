package com.uwu.animex.ui.tv

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TvBackdropStyle(
    val label: String,
    val description: String,
) {
    AURORA("Aurora", "Cahaya utara statis, tanpa gerak"),
    STARS("Bintang", "Taburan bintang statis"),
    STATIC("Gradien statis", "Gradien gelap sederhana"),
    OFF("Mati (hemat daya)", "Latar polos"),
}

/**
 * Preferensi khusus tampilan Android TV.
 *
 * Semua opsi latar di sini sengaja statis: tidak ada animasi yang berjalan.
 */
object TvPrefs {
    private const val FILE = "animex_tv"
    private const val KEY_BACKDROP = "backdrop"
    private const val KEY_WIFI_ONLY = "wifi_only"
    private const val KEY_AUTO_SYNC = "auto_sync"

    private var prefs: SharedPreferences? = null

    private val _backdrop = MutableStateFlow(TvBackdropStyle.STATIC)
    val backdrop: StateFlow<TvBackdropStyle> = _backdrop.asStateFlow()

    private val _wifiOnly = MutableStateFlow(false)
    val wifiOnly: StateFlow<Boolean> = _wifiOnly.asStateFlow()

    private val _autoSync = MutableStateFlow(true)
    val autoSync: StateFlow<Boolean> = _autoSync.asStateFlow()

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        prefs = p
        _backdrop.value =
            runCatching { TvBackdropStyle.valueOf(p.getString(KEY_BACKDROP, null).orEmpty()) }
                .getOrDefault(TvBackdropStyle.STATIC)
        _wifiOnly.value = p.getBoolean(KEY_WIFI_ONLY, false)
        _autoSync.value = p.getBoolean(KEY_AUTO_SYNC, true)
    }

    fun setBackdrop(style: TvBackdropStyle) {
        _backdrop.value = style
        prefs?.edit()?.putString(KEY_BACKDROP, style.name)?.apply()
    }

    fun setWifiOnly(enabled: Boolean) {
        _wifiOnly.value = enabled
        prefs?.edit()?.putBoolean(KEY_WIFI_ONLY, enabled)?.apply()
    }

    fun setAutoSync(enabled: Boolean) {
        _autoSync.value = enabled
        prefs?.edit()?.putBoolean(KEY_AUTO_SYNC, enabled)?.apply()
    }
}
