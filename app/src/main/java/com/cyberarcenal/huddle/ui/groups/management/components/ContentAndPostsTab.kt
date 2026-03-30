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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import coil.compose.AsyncImage
import com.cyberarcenal.huddle.api.models.PostFeed
import com.cyberarcenal.huddle.ui.groups.management.GroupManagementViewModel

@Composable
fun ContentAndPostsTab(
    posts: LazyPagingItems<PostFeed>,
    viewModel: GroupManagementViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(posts.itemCount) { index ->
            val post = posts[index]
            post?.let {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = post.user?.profilePictureUrl,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp).clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(post.user?.username ?: "User", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            // Pin button
                            IconButton(onClick = { viewModel.pinPost(post.id ?: return@IconButton) }) {
                                Icon(Icons.Default.PushPin, contentDescription = "Pin")
                            }
                            // Delete button
                            IconButton(onClick = { viewModel.deletePost(post.id ?: return@IconButton) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                        Text(post.content ?: "", modifier = Modifier.padding(top = 8.dp))
                        // Optional: show image if any
                    }
                }
            }
        }
    }
}