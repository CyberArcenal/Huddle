package com.cyberarcenal.huddle.data.repositories.groups

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class GroupsRepository {
    private val api = ApiService.v1Api

    // ========== GROUPS ==========

    suspend fun getGroups(
        page: Int? = null,
        pageSize: Int? = null,
        privacy: String? = null,
        query: String? = null
    ): Result<PaginatedGroup> = safeApiCall {
        api.v1GroupsRetrieve(page, pageSize, privacy, query)
    }

    suspend fun createGroup(groupCreate: GroupCreateRequest): Result<Group> = safeApiCall {
        api.v1GroupsCreate(groupCreate)
    }

    suspend fun getGroup(groupId: Int): Result<Group> = safeApiCall {
        api.v1GroupsRetrieve2(groupId)
    }

    suspend fun updateGroup(groupId: Int, groupUpdate: GroupUpdateRequest): Result<Group> = safeApiCall {
        api.v1GroupsUpdate(groupId, groupUpdate)
    }

    suspend fun partialUpdateGroup(groupId: Int, patchedGroupUpdate: PatchedGroupUpdateRequest): Result<Group> = safeApiCall {
        api.v1GroupsPartialUpdate(groupId, patchedGroupUpdate)
    }

    suspend fun deleteGroup(groupId: Int): Result<Unit> = safeApiCall {
        api.v1GroupsDestroy(groupId)
    }

    // ========== GROUP MEMBERS ==========

    suspend fun getGroupMembers(
        groupId: Int,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedGroupMember> = safeApiCall {
        api.v1GroupsMembersRetrieve(groupId, page, pageSize)
    }

    suspend fun addGroupMember(
        groupId: Int,
        groupMemberCreate: GroupMemberCreateRequest
    ): Result<GroupMember> = safeApiCall {
        api.v1GroupsMembersCreate(groupId, groupMemberCreate)
    }

    suspend fun removeGroupMember(groupId: Int): Result<Unit> = safeApiCall {
        api.v1GroupsMembersDestroy(groupId)
    }

    suspend fun updateMemberRole(
        groupId: Int,
        userId: Int,
        patchedGroupMemberUpdate: PatchedGroupMemberUpdateRequest
    ): Result<GroupMember> = safeApiCall {
        api.v1GroupsMembersRolePartialUpdate(groupId, userId, patchedGroupMemberUpdate)
    }

    suspend fun searchGroupMembers(
        groupId: Int,
        query: String,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedGroupMember> = safeApiCall {
        api.v1GroupsMembersSearchRetrieve(groupId, query, page, pageSize)
    }

    // ========== GROUP ACTIONS ==========

    suspend fun joinGroup(groupId: Int): Result<GroupMember> = safeApiCall {
        api.v1GroupsJoinCreate(groupId)
    }

    suspend fun leaveGroup(groupId: Int): Result<Unit> = safeApiCall {
        api.v1GroupsLeaveCreate(groupId)
    }

    suspend fun changeGroupPrivacy(
        groupId: Int,
        privacy: PrivacyC6eEnum
    ): Result<Group> = safeApiCall {
        val body = PatchedChangePrivacyInputRequest(privacy = privacy)
        api.v1GroupsPrivacyPartialUpdate(groupId, body)
    }

    suspend fun transferOwnership(
        groupId: Int,
        newOwnerId: Int
    ): Result<V1GroupsTransferOwnershipCreate200Response> = safeApiCall {
        api.v1GroupsTransferOwnershipCreate(groupId, TransferOwnershipRequest(newOwnerId = newOwnerId))
    }

    suspend fun getGroupStatistics(groupId: Int): Result<GroupStatistics> = safeApiCall {
        api.v1GroupsStatisticsRetrieve(groupId)
    }

    // ========== POPULAR & RECOMMENDATIONS ==========

    suspend fun getPopularGroups(
        days: Int? = null,
        limit: Int? = null,
        minMembers: Int? = null
    ): Result<PaginatedGroup> = safeApiCall {
        api.v1GroupsPopularRetrieve(days, limit, minMembers)
    }

    suspend fun getGroupRecommendations(limit: Int? = null): Result<PaginatedGroup> = safeApiCall {
        api.v1GroupsRecommendationsRetrieve(limit)
    }
}