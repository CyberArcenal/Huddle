package com.cyberarcenal.huddle.ui.friends

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.cyberarcenal.huddle.api.models.UserList
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
                            val users = paginated.results.map { mapUserListToMinimal(it) }
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
                    followRepository.getFollowing(userId = userId, page = page, pageSize = pageSize).fold(
                        onSuccess = { paginated ->
                            val users = paginated.results.map { mapUserListToMinimal(it) }
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
                    followRepository.getMutualFriends(page, pageSize).fold(
                        onSuccess = { paginated ->
                            val users = paginated.results.map { mapUserListToMinimal(it) }
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
                    followRepository.getSuggestedUsers(page = page, pageSize = pageSize).fold(
                        onSuccess = { paginated ->
                            val users = paginated.results.map { suggested ->
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
                                nextKey = if (paginated.hasNext) page + 1 else null
                            )
                        },
                        onFailure = { LoadResult.Error(it) }
                    )
                }
                FriendsTab.MATCHES -> {
                    matchingRepository.getMatches(limit = pageSize, offset = (page - 1) * pageSize).fold(
                        onSuccess = { paginated ->
                            val users = paginated.results.map { match ->
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
                                nextKey = if (paginated.hasNext) page + 1 else null
                            )
                        },
                        onFailure = { LoadResult.Error(it) }
                    )
                }
                FriendsTab.POPULAR -> {
                    followRepository.getPopularUsers(page, pageSize).fold(
                        onSuccess = { paginated ->
                            val users = paginated.results.map { mapUserListToMinimal(it) }
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

    private fun mapUserListToMinimal(user: UserList): UserMinimal {
        return UserMinimal(
            id = user.id,
            username = user.username,
            profilePictureUrl = user.profilePictureUrl,
            isFollowing = user.isFollowing,
            personalityType = user.personalityType?.let {
                when (it) {
                    UserList.PersonalityType.ISTJ -> UserMinimal.PersonalityType.ISTJ
                    UserList.PersonalityType.ISFJ -> UserMinimal.PersonalityType.ISFJ
                    UserList.PersonalityType.INFJ -> UserMinimal.PersonalityType.INFJ
                    UserList.PersonalityType.INTJ -> UserMinimal.PersonalityType.INTJ
                    UserList.PersonalityType.ISTP -> UserMinimal.PersonalityType.ISTP
                    UserList.PersonalityType.ISFP -> UserMinimal.PersonalityType.ISFP
                    UserList.PersonalityType.INFP -> UserMinimal.PersonalityType.INFP
                    UserList.PersonalityType.INTP -> UserMinimal.PersonalityType.INTP
                    UserList.PersonalityType.ESTP -> UserMinimal.PersonalityType.ESTP
                    UserList.PersonalityType.ESFP -> UserMinimal.PersonalityType.ESFP
                    UserList.PersonalityType.ENFP -> UserMinimal.PersonalityType.ENFP
                    UserList.PersonalityType.ENTP -> UserMinimal.PersonalityType.ENTP
                    UserList.PersonalityType.ESTJ -> UserMinimal.PersonalityType.ESTJ
                    UserList.PersonalityType.ESFJ -> UserMinimal.PersonalityType.ESFJ
                    UserList.PersonalityType.ENFJ -> UserMinimal.PersonalityType.ENFJ
                    UserList.PersonalityType.ENTJ -> UserMinimal.PersonalityType.ENTJ
                }
            },
            fullName = "${user.firstName ?: ""} ${user.lastName ?: ""}".trim().ifEmpty { user.username ?: "" },
            capabilityScore = user.capabilityScore,
            reasons = user.reasons
        )
    }

    override fun getRefreshKey(state: PagingState<Int, UserMinimal>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}