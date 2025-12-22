package org.stypox.dicio.io.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.dicio.skill.context.SkillContext
import org.stypox.dicio.io.input.SttState
import org.stypox.dicio.ui.home.InteractionLog
import org.stypox.dicio.ui.home.SttFab

@Composable
fun AssistantOverlay(
    skillContext: SkillContext,
    interactionLog: InteractionLog,
    sttState: SttState?,
    onSttClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Use a simple MaterialTheme without the Activity-dependent SideEffect
    val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    
    MaterialTheme(colorScheme = colorScheme) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp,
            tonalElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = androidx.compose.ui.platform.LocalContext.current.getString(org.stypox.dicio.R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Compact interaction list
                CompactInteractionList(
                    skillContext = skillContext,
                    interactionLog = interactionLog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Microphone button
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (sttState != null) {
                        SttFab(
                            state = sttState,
                            onClick = onSttClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactInteractionList(
    skillContext: SkillContext,
    interactionLog: InteractionLog,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val interactions = interactionLog.interactions
    val pendingQuestion = interactionLog.pendingQuestion
    
    // Simple approach: continuously scroll to bottom while there's a pending question
    LaunchedEffect(pendingQuestion, interactions) {
        if (pendingQuestion != null) {
            // Keep scrolling while the question is pending
            while (isActive && pendingQuestion != null) {
                val itemCount = listState.layoutInfo.totalItemsCount
                if (itemCount > 0) {
                    listState.scrollToItem(itemCount - 1)
                }
                delay(150)
            }
        } else {
            // Scroll once when new answer is added
            val itemCount = listState.layoutInfo.totalItemsCount
            if (itemCount > 0) {
                delay(100) // Small delay to let content render
                listState.animateScrollToItem(itemCount - 1)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(8.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Show recent interactions
        interactions.forEach { interaction ->
            items(interaction.questionsAnswers) { qa ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // User question
                    if (qa.question != null) {
                        CompactQuestionBubble(text = qa.question)
                    }
                    
                    // Assistant answer
                    CompactAnswerBubble {
                        qa.answer.GraphicalOutput(ctx = skillContext)
                    }
                }
            }
        }

        // Pending question
        if (pendingQuestion != null) {
            item {
                CompactQuestionBubble(
                    text = pendingQuestion.userInput,
                    isPending = true
                )
            }
        }

        // Add some bottom padding
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun CompactQuestionBubble(
    text: String,
    isPending: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
            modifier = Modifier.widthIn(max = LocalConfiguration.current.screenWidthDp.dp * 0.7f)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isPending) FontWeight.Normal else FontWeight.Medium,
                        fontStyle = if (isPending) FontStyle.Italic else FontStyle.Normal
                    ),
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun CompactAnswerBubble(
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier.widthIn(max = LocalConfiguration.current.screenWidthDp.dp * 0.7f)
        ) {
            Box(
                modifier = Modifier.padding(8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                content()
            }
        }
    }
}
