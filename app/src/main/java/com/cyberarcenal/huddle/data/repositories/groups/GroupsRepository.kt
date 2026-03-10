package com.cyberarcenal.huddle.data.repositories.groups

import com.cyberarcenal.huddle.api.models.*
import com.cyberarcenal.huddle.data.repositories.utils.safeApiCall
import com.cyberarcenal.huddle.network.ApiService

class GroupsRepository {
    private val api = ApiService.v1Api

    // ========== GROUPS ==========

    /**
     * List groups with optional filters.
     * @param page Page number.
     * @param pageSize Results per page.
     * @param privacy Filter by privacy (public, private, secret).
     * @param query Search query for group name or description.
     */
    suspend fun getGroups(
        page: Int? = null,
        pageSize: Int? = null,
        privacy: String? = null,
        query: String? = null
    ): Result<PaginatedGroup> = safeApiCall {
        api.v1GroupsRetrieve(page, pageSize, privacy, query)
    }

    /**
     * Create a new group. Current user becomes the creator and admin.
     */
    suspend fun createGroup(groupCreate: GroupCreate): Result<Group> = safeApiCall {
        api.v1GroupsCreate(groupCreate)
    }

    /**
     * Get a single group by ID.
     */
    suspend fun getGroup(groupId: Int): Result<Group> = safeApiCall {
        api.v1GroupsRetrieve2(groupId)
    }

    /**
     * Update all fields of a group.
     */
    suspend fun updateGroup(groupId: Int, groupUpdate: GroupUpdate): Result<Group> = safeApiCall {
        api.v1GroupsUpdate(groupId, groupUpdate)
    }

    /**
     * Partially update a group.
     */
    suspend fun partialUpdateGroup(groupId: Int, patchedGroupUpdate: PatchedGroupUpdate): Result<Group> = safeApiCall {
        api.v1GroupsPartialUpdate(groupId, patchedGroupUpdate)
    }

    /**
     * Delete a group. Only the creator can delete.
     */
    suspend fun deleteGroup(groupId: Int): Result<Unit> = safeApiCall {
        api.v1GroupsDestroy(groupId)
    }

    // ========== GROUP MEMBERS ==========

    /**
     * List all members of a group (paginated).
     */
    suspend fun getGroupMembers(
        groupId: Int,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedGroupMember> = safeApiCall {
        api.v1GroupsMembersRetrieve(groupId, page, pageSize)
    }

    /**
     * Add a user to the group. Requires admin permissions.
     */
    suspend fun addGroupMember(
        groupId: Int,
        groupMemberCreate: GroupMemberCreate
    ): Result<GroupMember> = safeApiCall {
        api.v1GroupsMembersCreate(groupId, groupMemberCreate)
    }

    /**
     * Remove a user from the group. Requires appropriate permissions.
     */
    suspend fun removeGroupMember(groupId: Int): Result<Unit> = safeApiCall {
        api.v1GroupsMembersDestroy(groupId)
    }

    /**
     * Update a member's role (admin, moderator, member). Requires appropriate permissions.
     */
    suspend fun updateMemberRole(
        groupId: Int,
        userId: Int,
        patchedGroupMemberUpdate: PatchedGroupMemberUpdate
    ): Result<GroupMember> = safeApiCall {
        api.v1GroupsMembersRolePartialUpdate(groupId, userId, patchedGroupMemberUpdate)
    }

    /**
     * Search members within a group by username or name.
     */
    suspend fun searchGroupMembers(
        groupId: Int,
        query: String,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<PaginatedGroupMember> = safeApiCall {
        api.v1GroupsMembersSearchRetrieve(groupId, query, page, pageSize)
    }

    // ========== GROUP ACTIONS ==========

    /**
     * Join a public group. For private groups, the user must be invited.
     */
    suspend fun joinGroup(groupId: Int): Result<GroupMember> = safeApiCall {
        api.v1GroupsJoinCreate(groupId)
    }

    /**
     * Leave a group. Creator cannot leave without transferring ownership first.
     */
    suspend fun leaveGroup(groupId: Int): Result<Unit> = safeApiCall {
        api.v1GroupsLeaveCreate(groupId)
    }

    /**
     * Change group privacy. Only creator can do this.
     */
    suspend fun changeGroupPrivacy(
        groupId: Int,
        privacy: PrivacyC6eEnum
    ): Result<Group> = safeApiCall {
        // The API expects a body with a "privacy" field. We'll pass a map.
        val body = PatchedChangePrivacyInput(privacy = privacy)
        api.v1GroupsPrivacyPartialUpdate(groupId, body)
    }

    /**
     * Transfer group ownership to another member. Only current creator can do this.
     */
    suspend fun transferOwnership(
        groupId: Int,
        newOwnerId: Int
    ): Result<V1GroupsTransferOwnershipCreate200Response> = safeApiCall {
        api.v1GroupsTransferOwnershipCreate(groupId, TransferOwnership(newOwnerId = newOwnerId))
    }

    /**
     * Get statistics for a group (member count, posts count, etc.).
     */
    suspend fun getGroupStatistics(groupId: Int): Result<GroupStatistics> = safeApiCall {
        api.v1GroupsStatisticsRetrieve(groupId)
    }

    // ========== POPULAR & RECOMMENDATIONS ==========

    /**
     * Get popular groups based on recent activity and member count.
     */
    suspend fun getPopularGroups(
        days: Int? = null,
        limit: Int? = null,
        minMembers: Int? = null
    ): Result<PaginatedGroup> = safeApiCall {
        api.v1GroupsPopularRetrieve(days, limit, minMembers)
    }

    /**
     * Get group recommendations for the current user based on their interests/follows.
     */
    suspend fun getGroupRecommendations(limit: Int? = null): Result<PaginatedGroup> = safeApiCall {
        api.v1GroupsRecommendationsRetrieve(limit)
    }
}