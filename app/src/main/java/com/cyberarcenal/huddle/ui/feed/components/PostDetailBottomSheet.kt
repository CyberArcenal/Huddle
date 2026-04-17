package com.cyberarcenal.huddle.ui.feed.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.api.models.PostStatsSerializers
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest
import com.cyberarcenal.huddle.data.models.MediaDetailData
import com.cyberarcenal.huddle.ui.common.feed.FeedItemFrame
import com.cyberarcenal.huddle.ui.common.feed.ShareRequestData
import com.cyberarcenal.huddle.ui.common.post.PostItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailBottomSheet(
    post: PostFeed,
    navController: NavController,
    onDismiss: () -> Unit,
    onReactionClick: (ReactionCreateRequest) -> Unit,
    onCommentClick: (String, Int, PostStatsSerializers) -> Unit,
    onShareClick: (ShareRequestData) -> Unit,
    onImageClick: (MediaDetailData) -> Unit,
    onVideoClick: (PostFeed, String) -> Unit,
    onMoreClick: (PostFeed) -> Unit,
    onReactionSummaryClick: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            FeedItemFrame(
                user = post.user,
                createdAt = post.createdAt,
                statistics = post.statistics,
                caption = post.content,
                onReactionClick = { reaction ->
                    post.id?.let { id ->
                        onReactionClick(
                            ReactionCreateRequest(
                                contentType = "post",
                                objectId = id,
                                reactionType = reaction
                            )
                        )
                    }
                },
                onCommentClick = {
                    onDismiss()
                    onCommentClick("post", post.id!!, post.statistics!!)
                },
                onShareClick = onShareClick,
                onMoreClick = { onMoreClick(post) },
                onProfileClick = { userId ->
                    onDismiss()
                    navController.navigate("profile/$userId")
                },
                onGroupClick = { groupId ->
                    onDismiss()
                    navController.navigate("group/$groupId")
                },
                onReactionSummaryClick = onReactionSummaryClick,
                onCommentSummaryClick = {
                    onDismiss()
                    onCommentClick("post", post.id!!, post.statistics!!)
                },
                showBottomDivider = false,
                content = {
                    PostItem(
                        post = post,
                        onImageClick = onImageClick,
                        onVideoClick = onVideoClick,
                        isPaused = false
                    )
                },
                postData = post
            )
        }
    }
}
