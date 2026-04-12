package com.cyberarcenal.huddle.ui.highlight.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cyberarcenal.huddle.api.models.StoryHighlight
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToHighlightSheet(
    storyId: Int,
    highlights: List<StoryHighlight>,
    onDismiss: () -> Unit,
    onAddStoryToHighlight: (highlightId: Int, storyId: Int) -> Unit,
    onCreateNewHighlight: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = {
            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
        },
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Add to Highlight",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                // Option to create a new highlight
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(120.dp)
                            .clickable {
                                scope.launch { sheetState.hide() }.invokeOnCompletion { 
                                    onDismiss()
                                    onCreateNewHighlight()
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(160.dp)
                                .clip(RoundedCornerShape(30.dp))
                                .shadow(8.dp, RoundedCornerShape(30.dp))
                                .background(Color(0xFFF0F2F5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Highlight",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "New",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Existing highlights
                items(highlights) { highlight ->
                    val isAlreadyAdded = highlight.stories?.any { it.id == storyId } ?: false
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(120.dp)
                    ) {
                        HighlightCard(
                            highlight = highlight,
                            isAdded = isAlreadyAdded,
                            onClick = {
                                val hId = highlight.id
                                if (hId != null) {
                                    onAddStoryToHighlight(hId, storyId)
                                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                                }
                            },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = highlight.title ?: "",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
