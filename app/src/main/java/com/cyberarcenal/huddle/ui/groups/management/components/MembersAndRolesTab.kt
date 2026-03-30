package com.cyberarcenal.huddle.ui.groups.management.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.api.models.GroupMemberMinimal
import com.cyberarcenal.huddle.api.models.RoleEnum
import com.cyberarcenal.huddle.ui.groups.management.GroupManagementViewModel


@Composable
fun MembersAndRolesTab(
    members: LazyPagingItems<GroupMemberMinimal>,
    viewModel: GroupManagementViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(members.itemCount) { index ->
            val member = members[index]
            member?.let {
                // Reuse MemberItem but add role dropdown menu
                // We'll create a custom item with admin actions
                ManageMemberCard(
                    member = it,
                    onPromoteToAdmin = { viewModel.promoteMember(it.user?.id ?: return@ManageMemberCard, RoleEnum.ADMIN) },
                    onPromoteToModerator = { viewModel.promoteMember(it.user?.id ?: return@ManageMemberCard, RoleEnum.MODERATOR) },
                    onRemove = { viewModel.removeMember(it.user?.id ?: return@ManageMemberCard) }
                )
            }
        }
    }
}

@Composable
fun ManageMemberCard(
    member: GroupMemberMinimal,
    onPromoteToAdmin: () -> Unit,
    onPromoteToModerator: () -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = member.user?.profilePictureUrl,
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(member.user?.username ?: "User", fontWeight = FontWeight.Bold)
                Text(
                    text = when (member.role) {
                        RoleEnum.ADMIN -> "Admin"
                        RoleEnum.MODERATOR -> "Moderator"
                        else -> "Member"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            // Dropdown for actions
            var expanded by remember { mutableStateOf(false) }
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Actions")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (member.role != RoleEnum.ADMIN) {
                    DropdownMenuItem(
                        text = { Text("Promote to Admin") },
                        onClick = {
                            onPromoteToAdmin()
                            expanded = false
                        }
                    )
                }
                if (member.role != RoleEnum.MODERATOR && member.role != RoleEnum.ADMIN) {
                    DropdownMenuItem(
                        text = { Text("Promote to Moderator") },
                        onClick = {
                            onPromoteToModerator()
                            expanded = false
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Remove from Group") },
                    onClick = {
                        onRemove()
                        expanded = false
                    }
                )
            }
        }
    }
}