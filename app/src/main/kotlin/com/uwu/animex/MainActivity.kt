package com.uwu.animex

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uwu.animex.core.notification.AppNotifier
import com.uwu.animex.core.pairing.PairingLink
import com.uwu.animex.core.pairing.PairingRouter
import com.uwu.animex.data.anilist.AniList
import com.uwu.animex.data.download.Downloads
import com.uwu.animex.data.local.Appearance
import com.uwu.animex.data.local.Bookmarks
import com.uwu.animex.data.local.CuplixPrefs
import com.uwu.animex.data.local.CuplixSaved
import com.uwu.animex.data.local.CuplixWatched
import com.uwu.animex.data.local.HomeSectionPrefs
import com.uwu.animex.data.local.EpisodeAlerts
import com.uwu.animex.data.local.History
import com.uwu.animex.data.local.Onboarding
import com.uwu.animex.data.local.PlayerPrefs
import com.uwu.animex.data.local.Progress
import com.uwu.animex.data.local.SearchHistory
import com.uwu.animex.data.local.ThemeMode
import com.uwu.animex.data.mal.Mal
import com.uwu.animex.data.tracker.SyncSourcePref
import com.uwu.animex.data.update.AppUpdate
import com.uwu.animex.ui.navigation.App
import com.uwu.animex.ui.navigation.DiscussionLink
import com.uwu.animex.ui.navigation.EpisodeLink
import com.uwu.animex.ui.navigation.NotificationRouter
import com.uwu.animex.ui.background.BackgroundType
import com.uwu.animex.ui.viewmodel.DiscussionScope
import com.uwu.animex.ui.player.PipHelper
import com.uwu.animex.ui.theme.AppTheme
import com.uwu.animex.ui.tv.TvPrefs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        Appearance.init(this)
        if (Appearance.settings.value.mode != ThemeMode.DARK) Appearance.setMode(ThemeMode.DARK)
        if (Appearance.settings.value.backgroundType != BackgroundType.NONE) {
            Appearance.setBackgroundType(BackgroundType.NONE)
        }
        if (Appearance.settings.value.backgroundParallax) Appearance.setBackgroundParallax(false)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        SyncSourcePref.init(this)
        TvPrefs.init(this)
        Onboarding.init(this)
        CuplixPrefs.init(this)
        HomeSectionPrefs.init(this)
        CuplixSaved.init(this)
        CuplixWatched.init(this)
        PlayerPrefs.init(this)
        History.init(this)
        Progress.init(this)
        Bookmarks.init(this)
        SearchHistory.init(this)
        Downloads.init(this)
        Mal.init(this)
        AniList.init(this)
        EpisodeAlerts.init(this)
        AppUpdate.init(this)
        handleMalRedirect(intent)
        handlePairLink(intent)
        handleNotificationIntent(intent)
        handleAnimeLink(intent)
        setContent {
            val settings by Appearance.settings.collectAsStateWithLifecycle()
            DisposableEffect(Unit) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                    navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
                onDispose {}
            }
            AppTheme(settings = settings, darkTheme = true) {
                val windowBg = MaterialTheme.colorScheme.background
                SideEffect { window.setBackgroundDrawable(ColorDrawable(windowBg.toArgb())) }
                App()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleMalRedirect(intent)
        handlePairLink(intent)
        handleNotificationIntent(intent)
        handleAnimeLink(intent)
    }

    private fun handleAnimeLink(intent: Intent?) {
        val uri = intent?.data ?: return
        val scheme = uri.scheme?.lowercase()
        val host = uri.host?.lowercase()
        val id =
            when {
                scheme == "animex" && host == "anime" -> uri.lastPathSegment
                scheme == "https" &&
                    host == "hatsunemikuuwu.github.io" &&
                    uri.path?.startsWith("/AnimeX-App/anime.html") == true -> uri.getQueryParameter("id")
                else -> null
            }?.trim()?.takeIf { it.isNotBlank() } ?: return
        intent.data = null
        val epId = uri.getQueryParameter("ep")?.trim()?.takeIf { it.isNotBlank() }
        if (epId != null) {
            val idx = uri.getQueryParameter("idx")?.trim()?.takeIf { it.isNotBlank() }
            val name = uri.getQueryParameter("t")?.trim().orEmpty()
            val startAtMs = uri.getQueryParameter("ms")?.trim()?.toLongOrNull()?.takeIf { it >= 0L }
            val title = if (idx != null && name.isNotEmpty()) "$name - Ep $idx" else name
            NotificationRouter.pendingEpisode.value = EpisodeLink(id, epId, idx, title, startAtMs)
            return
        }
        NotificationRouter.pendingDetail.value = id
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(AppUpdate.EXTRA_OPEN_UPDATE, false)) {
            intent.removeExtra(AppUpdate.EXTRA_OPEN_UPDATE)
            NotificationRouter.pendingOpenUpdate.value = true
            return
        }
        if (intent.getBooleanExtra(AppNotifier.EXTRA_OPEN_NOTIFICATIONS, false)) {
            intent.removeExtra(AppNotifier.EXTRA_OPEN_NOTIFICATIONS)
            NotificationRouter.pendingOpenCenter.value = true
            return
        }
        val discussionTargetId = intent.getStringExtra(AppNotifier.EXTRA_OPEN_DISCUSSION_TARGET_ID)
        if (!discussionTargetId.isNullOrBlank()) {
            val scopeName = intent.getStringExtra(AppNotifier.EXTRA_OPEN_DISCUSSION_SCOPE)
            val title = intent.getStringExtra(AppNotifier.EXTRA_OPEN_DISCUSSION_TITLE)?.takeIf(String::isNotBlank)
            val commentId = intent.getStringExtra(AppNotifier.EXTRA_OPEN_DISCUSSION_COMMENT_ID).orEmpty()
            val parentId = intent.getStringExtra(AppNotifier.EXTRA_OPEN_DISCUSSION_PARENT_ID)?.takeIf(String::isNotBlank)
            intent.removeExtra(AppNotifier.EXTRA_OPEN_DISCUSSION_SCOPE)
            intent.removeExtra(AppNotifier.EXTRA_OPEN_DISCUSSION_TARGET_ID)
            intent.removeExtra(AppNotifier.EXTRA_OPEN_DISCUSSION_TITLE)
            intent.removeExtra(AppNotifier.EXTRA_OPEN_DISCUSSION_COMMENT_ID)
            intent.removeExtra(AppNotifier.EXTRA_OPEN_DISCUSSION_PARENT_ID)
            val scope =
                runCatching { DiscussionScope.valueOf(scopeName.orEmpty()) }
                    .getOrDefault(DiscussionScope.Movie)
            NotificationRouter.pendingDiscussion.value =
                DiscussionLink(
                    scope = scope,
                    targetId = discussionTargetId,
                    title = title,
                    targetCommentId = commentId,
                    parentCommentId = parentId,
                )
            return
        }
        val id = intent.getStringExtra(AppNotifier.EXTRA_OPEN_DETAIL) ?: return
        intent.removeExtra(AppNotifier.EXTRA_OPEN_DETAIL)
        NotificationRouter.pendingDetail.value = id
    }

    private fun handleMalRedirect(intent: Intent?) {
        val uri = intent?.data ?: return
        val isMal = Mal.isRedirect(uri)
        val isAniList = AniList.isRedirect(uri)
        if (!isMal && !isAniList) return
        intent.data = null
        if (isMal) Mal.handleRedirect(uri)
        if (isAniList) AniList.handleRedirect(uri)
    }

    private fun handlePairLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme?.lowercase() != "animex" || uri.host?.lowercase() != "pair") return
        val host = uri.getQueryParameter("host")?.trim().takeIf { !it.isNullOrBlank() } ?: return
        val port = uri.getQueryParameter("port")?.toIntOrNull()?.takeIf { it in 1..65535 } ?: 8787
        val code = uri.getQueryParameter("code")?.trim()?.takeIf { it.isNotBlank() } ?: return
        intent.data = null
        PairingRouter.pending.value = PairingLink(host, port, code)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        PipHelper.onUserLeaveHint(this)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        PipHelper.setInPip(isInPictureInPictureMode)
    }
}
