package com.cyberarcenal.huddle.ui.groups.groupdetail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow

// ExpandableText.kt (if needed)
@Composable
fun ExpandableText(
    text: String,
    maxLines: Int = 3,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val textLayoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    val isOverflow = textLayoutResult.value?.let {
        it.lineCount > maxLines
    } ?: false

    Column(modifier = modifier) {
        Text(
            text = text,
            style = style,
            maxLines = if (expanded) Int.MAX_VALUE else maxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { textLayoutResult.value = it }
        )
        if (isOverflow) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Show less" else "Show more")
            }
        }
    }
}