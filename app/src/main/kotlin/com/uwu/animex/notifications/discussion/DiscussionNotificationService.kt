package com.uwu.animex.notifications.discussion

import android.content.Context
import com.uwu.animex.ui.viewmodel.DiscussionScope

object DiscussionNotificationService {
    fun discussionKey(
        scope: DiscussionScope,
        targetId: String,
    ): String = "${scope.name}:$targetId"

    fun init(context: Context) {
        sync(context.applicationContext)
    }

    fun sync(context: Context? = null) {
        context?.let { DiscussionCheckWorker.sync(it, enabled = false) }
    }

    fun trackCommentedDiscussion(
        scope: DiscussionScope,
        targetId: String,
        title: String?,
        latestCommentId: String?,
    ) = Unit

    fun trackOwnedOrRepliedComment(
        episodeId: String,
        title: String?,
        commentId: String,
        latestReplyId: String? = null,
    ) = Unit

    fun markDiscussionRead(
        scope: DiscussionScope,
        targetId: String,
        latestCommentId: String?,
    ) = Unit

    fun markRepliesRead(
        commentId: String,
        latestReplyId: String?,
    ) = Unit

    suspend fun checkAll(context: Context) = Unit
}
