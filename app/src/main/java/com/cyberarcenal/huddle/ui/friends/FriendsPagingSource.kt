package com.cyberarcenal.huddle.ui.friends

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.cyberarcenal.huddle.api.models.UserMinimal
import com.cyberarcenal.huddle.data.repositories.FollowRepository
import com.cyberarcenal.huddle.data.repositories.UserMatchingRepository

class FriendsPagingSource(
    private val followRepository: FollowRepository,
    private val matchingRepository: UserMatchingRepository,
    private val userId: Int?,
    private val tab: FriendsTab
) : PagingSource<Int, UserMinimal>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, UserMinimal> {
        return try {
            val page = params.key ?: 1
            val pageSize = params.loadSize

            when (tab) {
                FriendsTab.FOLLOWERS -> {
                    followRepository.getFollowers(userId = userId, page = page, pageSize = pageSize).fold(
                        onSuccess = { paginated ->
                            LoadResult.Page(
                                data = paginated.data.results,
                                prevKey = if (page == 1) null else page - 1,
                                nextKey = if (paginated.data.hasNext) page + 1 else null
                            )
                        },
                        onFailure = { LoadResult.Error(it) }
                    )
                }
                FriendsTab.FOLLOWING -> {
                    followRepository.getFollowing(userId = userId, page = page, pageSize = pageSize).fold(
                        onSuccess = { paginated ->
                            LoadResult.Page(
                                data = paginated.data.results,
                                prevKey = if (page == 1) null else page - 1,
                                nextKey = if (paginated.data.hasNext) page + 1 else null
                            )
                        },
                        onFailure = { LoadResult.Error(it) }
                    )
                }
                FriendsTab.MOOTS -> {
                    followRepository.getMutualFriends(page, pageSize).fold(
                        onSuccess = { paginated ->
                            LoadResult.Page(
                                data = paginated.data.results,
                                prevKey = if (page == 1) null else page - 1,
                                nextKey = if (paginated.data.hasNext) page + 1 else null
                            )
                        },
                        onFailure = { LoadResult.Error(it) }
                    )
                }
                FriendsTab.SUGGESTIONS -> {
                    followRepository.getSuggestedUsers(page = page, pageSize = pageSize).fold(
                        onSuccess = { paginated ->
                            val users = paginated.data.results.map { suggested ->
                                UserMinimal(
                                    id = suggested.user?.id,
                                    username = suggested.user?.username,
                                    profilePictureUrl = suggested.user?.profilePictureUrl,
                                    isFollowing = suggested.user?.isFollowing,
                                    capabilityScore = suggested.user?.capabilityScore,
                                    reasons = suggested.user?.reasons
                                )
                            }
                            LoadResult.Page(
                                data = users,
                                prevKey = if (page == 1) null else page - 1,
                                nextKey = if (paginated.data.hasNext) page + 1 else null
                            )
                        },
                        onFailure = { LoadResult.Error(it) }
                    )
                }
                FriendsTab.MATCHES -> {
                    matchingRepository.getMatches(limit = pageSize, offset = (page - 1) * pageSize).fold(
                        onSuccess = { paginated ->
                            val users = paginated.data.results.map { match ->
                                UserMinimal(
                                    id = match.user?.id,
                                    username = match.user?.username,
                                    profilePictureUrl = match.user?.profilePictureUrl,
                                    isFollowing = match.user?.isFollowing,
                                    capabilityScore = match.score,
                                    reasons = match.reasons
                                )
                            }
                            LoadResult.Page(
                                data = users,
                                prevKey = if (page == 1) null else page - 1,
                                nextKey = if (paginated.data.hasNext) page + 1 else null
                            )
                        },
                        onFailure = { LoadResult.Error(it) }
                    )
                }
                FriendsTab.POPULAR -> {
                    followRepository.getPopularUsers(page, pageSize).fold(
                        onSuccess = { paginated ->
                            LoadResult.Page(
                                data = paginated.data.results,
                                prevKey = if (page == 1) null else page - 1,
                                nextKey = if (paginated.data.hasNext) page + 1 else null
                            )
                        },
                        onFailure = { LoadResult.Error(it) }
                    )
                }
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, UserMinimal>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}