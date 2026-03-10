package com.cyberarcenal.huddle.ui.comments

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun CommentsScreen(postId: Int?, navController: NavController) {
    Text("Comments for post $postId")
}