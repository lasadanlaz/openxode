package com.uwu.animex.ui.common

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Seluruh transisi aplikasi dimatikan.
 *
 * Android TV tidak butuh animasi perpindahan layar, fade, maupun parallax.
 * Semua fungsi di bawah mengembalikan transisi kosong agar layar langsung
 * berganti tanpa animasi.
 */
object AppMotion {
    const val FADE_OUT_MS = 0
    const val FADE_IN_MS = 0
    const val SHARED_AXIS_MS = 0
    const val FADE_THROUGH_SCALE_MS = 0
    const val IMAGE_CROSSFADE_MS = 0

    const val SHARED_AXIS_SLIDE_DP = 0
    const val FADE_THROUGH_START_SCALE = 1f

    private val noTransform =
        ContentTransform(
            targetContentEnter = EnterTransition.None,
            initialContentExit = ExitTransition.None,
            sizeTransform = SizeTransform(clip = false),
        )

    fun fadeThrough(): ContentTransform = noTransform

    fun crossfade(m: MotionScheme): ContentTransform = noTransform

    fun sharedAxisXEnter(
        forward: Boolean,
        slidePx: Int,
    ): EnterTransition = EnterTransition.None

    fun sharedAxisXExit(
        forward: Boolean,
        slidePx: Int,
    ): ExitTransition = ExitTransition.None

    @Composable
    fun fadeEnter(): EnterTransition = EnterTransition.None

    @Composable
    fun fadeExit(): ExitTransition = ExitTransition.None

    @Composable
    fun popEnter(): EnterTransition = EnterTransition.None

    @Composable
    fun popExit(): ExitTransition = ExitTransition.None

    @Composable
    fun expandEnter(): EnterTransition = EnterTransition.None

    @Composable
    fun expandExit(): ExitTransition = ExitTransition.None

    @Composable
    fun expandHorizontalEnter(): EnterTransition = EnterTransition.None

    @Composable
    fun expandHorizontalExit(): ExitTransition = ExitTransition.None

    @Composable
    fun slideInEnd(): EnterTransition = EnterTransition.None

    @Composable
    fun slideOutEnd(): ExitTransition = ExitTransition.None
}

@Composable
fun LazyItemScope.animateItemExpressive(): Modifier = Modifier

@Composable
fun <P> rememberHeldPhase(
    target: P,
    loadingPhase: P,
): P = target

@Composable
fun <T> UiStateContent(
    state: UiState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    loading: @Composable () -> Unit = { GridPlaceholder() },
    error: @Composable (String) -> Unit = { msg -> ErrorState(msg, onRetry) },
    content: @Composable (T) -> Unit,
) {
    when (state) {
        is UiState.Error -> Box(modifier.fillMaxSize()) { error(state.msg) }
        is UiState.Ready -> content(state.value)
        else -> Box(modifier.fillMaxSize()) { loading() }
    }
}
