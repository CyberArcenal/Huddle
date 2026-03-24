package com.cyberarcenal.huddle.ui.profile.components

import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.api.models.PostStatsSerializers
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest
import com.cyberarcenal.huddle.api.models.ReactionCreateRequest.ReactionType
import com.cyberarcenal.huddle.api.models.UserMinimal
import com.cyberarcenal.huddle.ui.common.feed.MediaDetailFrame
import java.time.OffsetDateTime

@Composable
fun MediaDetailDialog(
    imageUrl: String,
    user: UserMinimal?,
    createdAt: OffsetDateTime?,
    statistics: PostStatsSerializers?,
    objectId: Int,
    contentType: String, // "post" o "share"
    onDismiss: () -> Unit,
    onReactionClick: (ReactionCreateRequest) -> Unit,
    onCommentClick: (String, Int) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // Fullscreen mode
            decorFitsSystemWindows = false
        )
    ) {
        MediaDetailFrame(
            user = user,
            createdAt = createdAt,
            statistics = statistics,
            onCloseClick = onDismiss,
            onReactionClick = { reactionType ->
                val request = ReactionCreateRequest(
                    contentType = contentType,
                    objectId = objectId,
                    reactionType = reactionType
                );
                onReactionClick(request);
            },
            onCommentClick = {
                onCommentClick(contentType, objectId)
            },
            onShareClick = { /* Handle share logic */ }
        ) {
            // Content: Zoomable Image
            var scale by remember { mutableFloatStateOf(1f) }
            val state = rememberTransformableState { zoomChange, _, _ ->
                scale *= zoomChange
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .transformable(state = state),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale.coerceIn(1f, 5f),
                            scaleY = scale.coerceIn(1f, 5f)
                        ),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}