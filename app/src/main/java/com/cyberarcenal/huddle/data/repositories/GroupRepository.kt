// GroupMainViewModel.kt
package com.cyberarcenal.huddle.data.repositories

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part




interface GroupCreateApi {
    @Multipart
    @POST("api/v1/groups/groups/")
    suspend fun apiV1GroupsCreate(
        @Part("name") name: RequestBody,
        @Part("description") description: RequestBody,
        @Part("privacy") privacy: RequestBody? = null,
        @Part("group_type") groupType: RequestBody? = null,
        @Part profile_picture: MultipartBody.Part? = null,
        @Part cover_photo: MultipartBody.Part? = null
    ): Response<GroupDisplay>
}

class GroupRepository {
    private val api = ApiService.groupViewsApi
    // Gagamitin natin ang base retrofit mula sa ApiService kung sakaling kailangan i-create
    // Pero ideal na i-expose ito sa ApiService.
    private val createApi = ApiService.createGroupApi

    suspend fun createGroup(request: GroupCreateRequest): Result<GroupDisplay> = safeApiCall {
        createApi.apiV1GroupsCreate(
            name = request.name.toRequestBody("text/plain".toMediaTypeOrNull()),
            description = request.description.toRequestBody("text/plain".toMediaTypeOrNull()),
            privacy = request.privacy?.value?.toRequestBody("text/plain".toMediaTypeOrNull()),
            groupType = request.groupType?.value?.toRequestBody("text/plain".toMediaTypeOrNull()),
            profile_picture = request.profilePicture,
            cover_photo = request.coverPhoto
        )
    }

    suspend fun deleteGroup(groupId: Int): Result<Unit> =
        safeApiCall { api.apiV1GroupsDestroy(groupId) }

    suspend fun joinGroup(groupId: Int): Result<GroupMemberDisplay> =
        safeApiCall { api.apiV1GroupsJoinCreate(groupId) }

    suspend fun leaveGroup(groupId: Int): Result<Unit> =
        safeApiCall { api.apiV1GroupsLeaveCreate(groupId) }

    suspend fun addMember(groupId: Int, request: GroupMemberCreateRequest): Result<GroupMemberDisplay> =
        safeApiCall { api.apiV1GroupsMembersCreate(groupId, request) }

    suspend fun addMemberById(groupId: Int, userId: Int, request: GroupMemberCreateRequest): Result<GroupMemberDisplay> =
        safeApiCall { api.apiV1GroupsMembersCreate2(groupId, userId, request) }

    suspend fun removeMember(groupId: Int): Result<Unit> =
        safeApiCall { api.apiV1GroupsMembersDestroy(groupId) }

    suspend fun removeMemberById(groupId: Int, userId: Int): Result<Unit> =
        safeApiCall { api.apiV1GroupsMembersDestroy2(groupId, userId) }

    suspend fun getMembers(groupId: Int, page: Int? = null, pageSize: Int? = null): Result<PaginatedGroupMember> =
        safeApiCall { api.apiV1GroupsMembersRetrieve(groupId, page, pageSize) }

    suspend fun getMembersForUser(groupId: Int, userId: Int, page: Int? = null, pageSize: Int? = null): Result<PaginatedGroupMember> =
        safeApiCall { api.apiV1GroupsMembersRetrieve2(groupId, userId, page, pageSize) }

    suspend fun updateMemberRole(groupId: Int, userId: Int, request: PatchedGroupMemberUpdateRequest? = null): Result<GroupMemberDisplay> =
        safeApiCall { api.apiV1GroupsMembersRolePartialUpdate(groupId, userId, request) }

    suspend fun searchMembers(groupId: Int, query: String, page: Int? = null, pageSize: Int? = null): Result<PaginatedGroupMember> =
        safeApiCall { api.apiV1GroupsMembersSearchRetrieve(groupId, query, page, pageSize) }

    suspend fun partialUpdateGroup(groupId: Int, request: PatchedGroupCreateRequest? = null): Result<GroupDisplay> =
        safeApiCall { api.apiV1GroupsPartialUpdate(groupId, request) }

    suspend fun getPopularGroups(days: Int? = null, limit: Int? = null, minMembers: Int? = null): Result<PaginatedGroup> =
        safeApiCall { api.apiV1GroupsPopularRetrieve(days, limit, minMembers) }

    suspend fun changeGroupPrivacy(groupId: Int, request: PatchedChangePrivacyInputRequest? = null): Result<GroupDisplay> =
        safeApiCall { api.apiV1GroupsPrivacyPartialUpdate(groupId, request) }

    suspend fun getGroups(page: Int? = null, pageSize: Int? = null, privacy: String? = null, query: String? = null): Result<PaginatedGroup> =
        safeApiCall { api.apiV1GroupsRetrieve(page, pageSize, privacy, query) }

    suspend fun getGroup(groupId: Int): Result<GroupDisplay> =
        safeApiCall { api.apiV1GroupsRetrieve2(groupId) }

    suspend fun getGroupStatistics(groupId: Int): Result<GroupStatistics> =
        safeApiCall { api.apiV1GroupsStatisticsRetrieve(groupId) }

    suspend fun transferOwnership(groupId: Int, request: TransferOwnershipRequest): Result<ApiV1GroupsTransferOwnershipCreate200Response> =
        safeApiCall { api.apiV1GroupsTransferOwnershipCreate(groupId, request) }

    suspend fun updateGroup(groupId: Int, request: GroupCreateRequest): Result<GroupDisplay> =
        safeApiCall { api.apiV1GroupsUpdate(groupId, request) }


    suspend fun getGroupPosts(groupId: Int, page: Int, pageSize: Int): Result<FeedResponse> {
        return safeApiCall { api.apiV1GroupsFeedRetrieve(groupId, page, pageSize) }
    }

    // Add to GroupMainViewModel.kt

    suspend fun getMyGroups(page: Int? = null, pageSize: Int? = null): Result<PaginatedMyGroups> =
        safeApiCall { api.apiV1GroupsMygroupsRetrieve(page=page, pageSize=pageSize) }

    suspend fun getGroupFeed(page: Int? = null, pageSize: Int? = null): Result<FeedResponse> =
        safeApiCall { api.apiV1GroupsGroupsFeedAllRetrieve(page, pageSize) }
}
