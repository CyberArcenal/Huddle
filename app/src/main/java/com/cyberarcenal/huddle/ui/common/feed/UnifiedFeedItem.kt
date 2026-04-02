package com.cyberarcenal.huddle.ui.common.feed

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.models.MediaDetailData
import com.cyberarcenal.huddle.data.models.StoryViewerData
import com.cyberarcenal.huddle.ui.common.event.EventsRow
import com.cyberarcenal.huddle.ui.common.user.MatchUserRow
import com.cyberarcenal.huddle.ui.common.reel.ReelsRow
import com.cyberarcenal.huddle.ui.common.share.ShareItem
import com.cyberarcenal.huddle.ui.common.group.GroupSuggestionsRow
import com.cyberarcenal.huddle.ui.common.post.PostItem
import com.cyberarcenal.huddle.ui.common.reel.ReelFeedItem
import com.cyberarcenal.huddle.ui.common.share.ShareFrame
import com.cyberarcenal.huddle.ui.common.story.FeedStoriesRow
import com.cyberarcenal.huddle.ui.common.story.StoryFeedItem
import com.cyberarcenal.huddle.ui.common.story.StoryGroupedItem
import com.cyberarcenal.huddle.ui.common.user.SuggestedUserRow
import com.cyberarcenal.huddle.ui.common.userimage.UserImageFeedItem
import com.cyberarcenal.huddle.ui.feed.safeConvertTo
import java.time.OffsetDateTime

data class ShareRequestData(
    val contentType: String,   // "post", "reel", "event", etc.
    val contentId: Int,        // ID ng original item na ishe-share
    val caption: String? = null, // Ang sariling text/caption ng user na nag-share
    val privacy: PrivacyB23Enum = PrivacyB23Enum.PUBLIC, // "public", "followers", o "private"
    val groupId: Int? = null   // Optional: Kung ishe-share ito sa loob ng isang specific group
)

@Composable
fun UnifiedFeedRow(
    row: UnifiedContentItem,
    navController: NavController,
    onReactionClick: (ReactionCreateRequest) -> Unit,
    onCommentClick: (String, Int, stats: PostStatsSerializers) -> Unit,
    onMoreClick: (Any) -> Unit,
    onImageClick: (MediaDetailData) -> Unit,
    onGroupJoinClick: (GroupMinimal) -> Unit,
    onFollowClick: (UserMinimal) -> Unit,
    onShare: (ShareRequestData) -> Unit,
    followStatuses: Map<Int, Boolean>,
    loadingUsers: Map<Int, Boolean>,

    groupMembershipStatuses: Map<Int, Boolean>,
    joiningGroupIds: Map<Int, Boolean>
) {
    val type = row.type
    if (type == null) {
        // Optionally show an error or skip rendering
        Text(
            text = "Unsupported content",
            modifier = Modifier.padding(16.dp),
            color = Color.Red
        )
        return
    }
    when (type) {
        UnifiedContentItemTypeEnum.POSTS -> {
            val items = row.items
            val posts = items?.mapNotNull { postMap ->
                safeConvertTo<PostFeed>(postMap, "PostFeed")
            } ?: emptyList()

            if (posts.isEmpty()) {
                Text(
                    "No posts to display",
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                posts.forEach { postFeed ->
                    key("feed_post_${postFeed.id}") {
                        FeedItemFrame(
                            user = postFeed.user,
                            createdAt = postFeed.createdAt,
                            statistics = postFeed.statistics,
                            caption = postFeed.content,
                            headerSuffix = "",
                            onReactionClick = { reaction ->
                                postFeed.id?.let { id ->
                                    onReactionClick(
                                        ReactionCreateRequest(
                                            contentType = "post",
                                            objectId = id,
                                            reactionType = reaction
                                        )
                                    )
                                }
                            },
                            onCommentClick = { onCommentClick("post", postFeed.id!!, postFeed.statistics!!) },
                            onShareClick = onShare,
                            onMoreClick = { onMoreClick(postFeed) },
                            onProfileClick = { userId ->
                                navController.navigate("profile/$userId")
                            },
                            content = {
                                PostItem(post = postFeed, onImageClick = onImageClick)
                            },
                            postData = postFeed
                        )
                    }
                }
            }
        }

        UnifiedContentItemTypeEnum.POST -> {
            val post = safeConvertTo<PostFeed>(row.item as Any, "PostFeed")
            post?.let {
                FeedItemFrame(
                    user = it.user,
                    createdAt = it.createdAt,
                    statistics = it.statistics,
                    headerSuffix = "",
                    caption = it.content,
                    onReactionClick = { reaction ->
                        it.id?.let { id ->
                            onReactionClick(
                                ReactionCreateRequest(
                                    contentType = "post",
                                    objectId = id,
                                    reactionType = reaction
                                )
                            )
                        }
                    },
                    onCommentClick = { onCommentClick("post", it.id!!, it.statistics!!) },
                    onShareClick = onShare,
                    onMoreClick = { onMoreClick(it) },
                    onProfileClick = { userId -> navController.navigate("profile/$userId") },
                    content = {
                        PostItem(post = it, onImageClick = onImageClick)
                    },
                    postData = it
                )
            }
        }

        UnifiedContentItemTypeEnum.SHARE -> {
            val shareFeed = safeConvertTo<ShareFeed>(row.item!!, "Share Convert")
            shareFeed?.let {
                key("feed_share_${it.id}") {
                    FeedItemFrame(
                        user = it.user,
                        createdAt = it.createdAt,
                        statistics = it.statistics,
                        headerSuffix = "shared a post",
                        caption = it.caption,   // sharer's caption
                        onReactionClick = { reaction ->
                            it.id?.let { id ->
                                onReactionClick(
                                    ReactionCreateRequest(
                                        contentType = "share",
                                        objectId = id,
                                        reactionType = reaction
                                    )
                                )
                            }
                        },
                        onCommentClick = { onCommentClick("share", it.id!!, it.statistics!!) },
                        onShareClick = onShare,
                        onMoreClick = { onMoreClick(it) },
                        onProfileClick = { userId -> navController.navigate("profile/$userId") },
                        content = {
                            ShareFrame(
                                shareFeed = shareFeed,
                                onImageClick = onImageClick,
                                content = {
                                    ShareItem(it);
                                }
                            )
                        },
                        postData = it
                    )
                }
            }
        }

        UnifiedContentItemTypeEnum.REEL -> {
            val item = row.item
            val reel = safeConvertTo<ReelDisplay>(item as Any, "Reel Convert")
            reel?.let {
                key("feed_reel_${it.id}") {
                    FeedItemFrame(
                        user = it.user,
                        createdAt = it.createdAt,
                        caption = it.caption,
                        statistics = it.statistics,
                        headerSuffix = "posted a reel",
                        onReactionClick = { reaction ->
                            it.id?.let { id ->
                                onReactionClick(
                                    ReactionCreateRequest(
                                        contentType = "reel",
                                        objectId = id,
                                        reactionType = reaction
                                    )
                                )
                            }
                        },
                        onCommentClick = { onCommentClick("reel", it.id!!, it.statistics!!) },
                        onShareClick = onShare,
                        onMoreClick = { onMoreClick(it) },
                        onProfileClick = { userId ->
                            navController.navigate("profile/$userId")
                        },
                        content = {
                            ReelFeedItem(
                                reel = it,
                                onReelClick = { reelId -> navController.navigate("reels/$reelId")},
                                onProfileClick = { userId -> navController.navigate("profile/${userId}") }
                            )
                        },
                        postData = it
                    )
                }
            }
        }

        UnifiedContentItemTypeEnum.STORY -> {
            val item = row.item
            val story = safeConvertTo<Story>(item as Any, "Story Convert")
            story?.let {
                StoryFeedItem(
                    story = story,
                    onStoryClick = { id ->
                        navController.navigate("story/$id")
                    }
                )
            }
        }

        UnifiedContentItemTypeEnum.USER_STORY -> {
            val storyGroup = row.item // from your API response
            val story = safeConvertTo<StoryFeed>(storyGroup as Any, tag = "storyGroup")
            story?.let { userStory ->
                userStory.stories?.let { stories ->
                    StoryGroupedItem(
                        user = userStory.user,
                        stories = userStory.stories,
                        createdAt = userStory.stories
                            .mapNotNull { it.createdAt } // Kunin lang ang mga hindi null na createdAt
                            .maxOrNull()                 // Kunin ang pinakabago (latest)
                            ?: OffsetDateTime.now(),      // Fallback kung empty ang list
                        caption = null,
                        onReactionClick = { reaction -> /* handle reaction */ },
                        onCommentClick = { /* open comments */ },
                        onShareClick = { shareData -> /* handle share */ },
                        onMoreClick = { /* show options */ },
                        onProfileClick = { userId -> navController.navigate("profile/$userId") }
                    )
                }

            }
        }

        UnifiedContentItemTypeEnum.USER_IMAGE -> {
            val item = row.item
            val userImage = safeConvertTo<UserImageDisplay>(item as Any, "UserImage Convert")
            userImage?.user?.let { user ->
                FeedItemFrame(
                    user = user,
                    createdAt = null,
                    statistics = null,
                    caption = userImage.caption,
                    headerSuffix = "updated their profile picture",
                    onReactionClick = { reaction ->
                        userImage.id?.let { id ->
                            onReactionClick(
                                ReactionCreateRequest(
                                    contentType = "user_image",
                                    objectId = id,
                                    reactionType = reaction
                                )
                            )
                        }
                    },
                    onCommentClick = { onCommentClick("userimage", userImage.id!!, userImage.statistics!!) },
                    onShareClick = onShare,
                    onMoreClick = { onMoreClick(userImage) },
                    onProfileClick = { navController.navigate("profile/${user.id}") },
                    content = {
                        UserImageFeedItem(
                            user = user,
                            userImage = userImage,
                            onImageClick = onImageClick
                        )
                    },
                    postData = userImage
                )
            }
        }

        UnifiedContentItemTypeEnum.SHARES -> {
            val items = row.items
            val shares = items?.mapNotNull { shareMap ->
                safeConvertTo<ShareFeed>(shareMap, "ShareFeed")
            } ?: emptyList()

            if (shares.isEmpty()) {
                Text(
                    "No shares to display",
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                shares.forEach { shareFeed ->
                    key("feed_share_${shareFeed.id}") {
                        FeedItemFrame(
                            user = shareFeed.user,
                            createdAt = shareFeed.createdAt,
                            statistics = shareFeed.statistics,
                            headerSuffix = "shared a post",
                            caption = shareFeed.caption,  // sharer's caption
                            onReactionClick = { reaction ->
                                shareFeed.id?.let { id ->
                                    onReactionClick(
                                        ReactionCreateRequest(
                                            contentType = "share",
                                            objectId = id,
                                            reactionType = reaction
                                        )
                                    )
                                }
                            },
                            onCommentClick = {
                                onCommentClick("share", shareFeed.id!!, shareFeed.statistics!!)
                            },
                            onShareClick = onShare,
                            onMoreClick = { onMoreClick(shareFeed) },
                            onProfileClick = { userId ->
                                navController.navigate("profile/$userId")
                            },
                            content = {
                                ShareFrame(
                                    shareFeed = shareFeed,
                                    onImageClick = onImageClick,
                                    content = {
                                        ShareItem(shareFeed)
                                    }
                                )
                            },
                            postData = shareFeed
                        )
                    }
                }
            }
        }

        UnifiedContentItemTypeEnum.EVENTS -> {
            val events = row.items?.mapNotNull { item ->
                runCatching { safeConvertTo<EventList>(item) }.getOrNull()
            } ?: emptyList()
            EventsRow(
                title = row.title ?: "",
                events = events,
                onEventClick = { event ->
                    navController.navigate("event_detail/${event.id}")
                },
                onShowMoreClick = {
                    navController.navigate("events_main")
                }
            )
        }

        UnifiedContentItemTypeEnum.RECOMMENDED_GROUPS -> {
            val groups = row.items?.mapNotNull { item ->
                runCatching { safeConvertTo<GroupSuggestionItem>(item) }.getOrNull()
            } ?: emptyList()
            if (groups.isNotEmpty()) {
                GroupSuggestionsRow(
                    title = row.title ?: "Recommended Groups",
                    groups = groups,
                    onGroupClick = { group ->
                        navController.navigate("group/${group.id}")
                    },
                    onJoinClick = onGroupJoinClick,
                    onShowMoreClick = {
                        navController.navigate("groups")
                    },
                    groupMembershipStatuses = groupMembershipStatuses,
                    joiningGroupIds = joiningGroupIds
                )
            }
        }

        UnifiedContentItemTypeEnum.SUGGESTED_USERS -> {
            val suggested = row.items?.mapNotNull { item ->
                runCatching { safeConvertTo<UserMutualCount>(item) }.getOrNull()
            } ?: emptyList()
            if (suggested.isNotEmpty()) {
                SuggestedUserRow(
                    title = row.title ?: "",
                    suggested = suggested,
                    onUserClick = { user -> navController.navigate("profile/${user.id}") },
                    onFollowClick = onFollowClick,
                    onShowMoreClick = { navController.navigate("suggested_user_page") },
                    followStatuses = followStatuses,
                    loadingUsers = loadingUsers
                )
            }
        }

        UnifiedContentItemTypeEnum.MATCH_USERS -> {
            val match = row.items?.mapNotNull { item ->
                runCatching { safeConvertTo<UserMatchScore>(item) }.getOrNull()
            } ?: emptyList()
            if (match.isNotEmpty()) {
                MatchUserRow(
                    title = row.title ?: "",
                    match = match,
                    onUserClick = { user -> navController.navigate("profile/${user.id}") },
                    onFollowClick = onFollowClick,
                    onShowMoreClick = { navController.navigate("match_user_page") },
                    followStatuses = followStatuses,
                    loadingUsers = loadingUsers,
                )
            }
        }

        UnifiedContentItemTypeEnum.REELS -> {
            val reels = row.items?.mapNotNull { item ->
                runCatching { safeConvertTo<ReelDisplay>(item) }.getOrNull()
            } ?: emptyList()
            if (reels.isNotEmpty()) {
                ReelsRow(
                    reels = reels,
                    onReelClick = {},
                    onShowMoreClick = {}
                )
            }
        }

        UnifiedContentItemTypeEnum.STORIES -> {
            val item = row.items?.mapNotNull { item ->
                runCatching { safeConvertTo<StoryFeed>(item) }.getOrNull()
            } ?: emptyList()
            if (item.isNotEmpty()) {
                FeedStoriesRow(
                    stories = item,
                    onCreateStoryClick = {
                        navController.navigate("create_story")
                    },
                    onStoryClick = { storyFeed, index ->
                        StoryViewerData.storyFeeds = item
                        navController.navigate("story_feed_viewer/$index")
                    }
                )
            }
        }

        else -> Unit
    }
}