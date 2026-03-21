package com.cyberarcenal.huddle.ui.friends

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.cyberarcenal.huddle.api.models.UserMinimal
import com.cyberarcenal.huddle.data.repositories.FollowViewsRepository
import com.cyberarcenal.huddle.data.repositories.UsersRepository

class FriendsPagingSource(
    private val repository: FollowViewsRepository,
    private val userId: Int?,
    private val tab: FriendsTab
) : PagingSource<Int, UserMinimal>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, UserMinimal> {
        return try {
            val page = params.key ?: 1
            
            when (tab) {
                FriendsTab.FOLLOWERS -> {
                    repository.getFollowers(userId = userId, page = page, pageSize = params.loadSize).fold(
                        onSuccess = { paginated ->
                            val users = paginated.results.map {
                                UserMinimal(
                                    id = it.id,
                                    username = it.username!!,
                                    profilePictureUrl = it.profilePictureUrl,
                                    isFollowing = it.isFollowing
                                )
                            }
                            LoadResult.Page(
                                data = users,
                                prevKey = if (page == 1) null else page - 1,
                                nextKey = if (paginated.hasNext) page + 1 else null
                            )
                        },
                        onFailure = { LoadResult.Error(it) }
                    )
                }
                FriendsTab.FOLLOWING -> {
                    repository.getFollowing(userId = userId, page = page, pageSize = params.loadSize).fold(
                        onSuccess = { paginated ->
                            val users = paginated.results.map {
                                UserMinimal(
                                    id = it.id,
                                    username = it.username,
                                    profilePictureUrl = it.profilePictureUrl,
                                    isFollowing = it.isFollowing
                                )
                            }
                            LoadResult.Page(
                                data = users,
                                prevKey = if (page == 1) null else page - 1,
                                nextKey = if (paginated.hasNext) page + 1 else null
                            )
                        },
                        onFailure = { LoadResult.Error(it) }
                    )
                }
                FriendsTab.MOOTS -> {

                    repository.getMutualFriends(page, params.loadSize).fold(
                        onSuccess = { paginated ->
                            val users = paginated.results.map {
                                UserMinimal(
                                    id = it.id,
                                    username = it.username,
                                    profilePictureUrl = it.profilePictureUrl,
                                    isFollowing = true // Usually moots implies mutual following
                                )
                            }
                            LoadResult.Page(
                                data = users,
                                prevKey = if (page == 1) null else page - 1,
                                nextKey = if (paginated.hasNext) page + 1 else null
                            )
                        },
                        onFailure = { LoadResult.Error(it) }
                    )
                }
                FriendsTab.SUGGESTIONS -> {
                    repository.getSuggestedUsers(page, params.loadSize).fold(
                        onSuccess = { paginated ->
                            val users = paginated.results.map {
                                UserMinimal(
                                    id = it.user!!.id,
                                    username = it.user.username,
                                    profilePictureUrl = it.user.profilePictureUrl,
                                    isFollowing = it.user.isFollowing
                                )
                            }
                            LoadResult.Page(
                                data = users,
                                prevKey = if (page == 1) null else page - 1,
                                nextKey = if (paginated.hasNext) page + 1 else null
                            )
                        },
                        onFailure = { LoadResult.Error(it) }
                    )
                }
                FriendsTab.POPULAR -> {
                    repository.getPopularUsers(page, params.loadSize).fold(
                        onSuccess = { paginated ->
                            val users = paginated.results.map {
                                UserMinimal(
                                    id = it.id,
                                    username = it.username,
                                    profilePictureUrl = it.profilePictureUrl,
                                    isFollowing = it.isFollowing
                                )
                            }
                            LoadResult.Page(
                                data = users,
                                prevKey = if (page == 1) null else page - 1,
                                nextKey = if (paginated.hasNext) page + 1 else null
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
