package org.stypox.dicio.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import java.text.Collator
import java.util.Locale
import org.dicio.skill.skill.SkillInfo
import org.stypox.dicio.R
import org.stypox.dicio.eval.SkillHandler
import org.stypox.dicio.ui.theme.AppTheme
import org.stypox.dicio.ui.util.SkillInfoPreviews

@Composable
fun WhatICanDo(skills: List<SkillInfo>) {
    val context = LocalContext.current
    
    // Get current locale for proper collation
    val locale = context.resources.configuration.locales.get(0) ?: Locale.getDefault()
    val collator = Collator.getInstance(locale)
    
    // Group skills by category and sort
    val categorizedSkills = skills
        .groupBy { skill ->
            val categoryRes = skill.categoryNameRes
            if (categoryRes == 0) {
                context.getString(R.string.category_other)
            } else {
                context.getString(categoryRes)
            }
        }
        .mapValues { (_, skillsList) ->
            // Sort skills alphabetically by localized name within each category
            skillsList.sortedWith(compareBy(collator) { it.name(context) })
        }
        .toList()
        // Sort categories alphabetically by localized name
        .sortedWith(compareBy(collator) { it.first })
    
    MessageCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
        Column {
            if (skills.isEmpty()) {
                NoEnabledSkills()
            } else {
                // Header
                WhatICanDoHeader()

                // Show all categories
                for ((category, categorySkills) in categorizedSkills) {
                    CategorySection(
                        categoryName = category,
                        skills = categorySkills
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun WhatICanDoHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.ready_to_help),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.here_is_what_i_can_do),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun CategorySection(
    categoryName: String,
    skills: List<SkillInfo>
) {
    var expanded by rememberSaveable(categoryName) { mutableStateOf(false) }
    
    Column(modifier = Modifier.animateContentSize()) {
        // Category header
        CategoryHeader(
            categoryName = categoryName,
            skillCount = skills.size,
            expanded = expanded,
            toggleExpanded = { expanded = !expanded }
        )

        // Conditionally show skills list when expanded
        if (expanded) {
            for (skill in skills) {
                SkillRow(
                    skill = skill,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CategoryHeader(
    categoryName: String,
    skillCount: Int,
    expanded: Boolean,
    toggleExpanded: () -> Unit,
) {
    val expandedAnimation by animateFloatAsState(
        label = "category_expanded_$categoryName",
        targetValue = if (expanded) 180f else 0f,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = toggleExpanded)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = categoryName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.skills_count, skillCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
            )
        }
        Icon(
            modifier = Modifier
                .rotate(expandedAnimation)
                .size(20.dp),
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = stringResource(
                if (expanded) R.string.reduce else R.string.expand
            )
        )
    }
}

@Preview
@Composable
private fun SkillRow(
    @PreviewParameter(SkillInfoPreviews::class) skill: SkillInfo,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = skill.icon(),
            contentDescription = null,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = skill.name(LocalContext.current),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = skill.sentenceExample(LocalContext.current),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NoEnabledSkills() {
    Text(
        text = stringResource(R.string.all_skills_disabled_title),
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
    )
    Text(
        text = stringResource(R.string.all_skills_disabled_description),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
    )
}

@Preview
@Composable
private fun WhatICanDoPreview() {
    AppTheme {
        WhatICanDo(skills = SkillInfoPreviews().values.toList())
    }
}

// this preview is useful to take screenshots for presentations
@Preview
@Composable
private fun WhatICanDoAllPreview() {
    AppTheme {
        WhatICanDo(skills = SkillHandler.newForPreviews(LocalContext.current).allSkillInfoList)
    }
}

@Preview
@Composable
private fun NoEnabledSkillsPreview() {
    AppTheme {
        WhatICanDo(skills = listOf())
    }
}

// this preview is useful to take screenshots for presentations
@Preview(device = "spec:width=2400px,height=2340px,dpi=440")
@Composable
private fun SkillChipsPreview() {
    AppTheme {
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val skills = SkillHandler.newForPreviews(LocalContext.current).allSkillInfoList
            for (skill in skills) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = skill.icon(),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = skill.name(LocalContext.current),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

// this preview is useful to take screenshots for presentations
@Preview(device = "spec:width=500px,height=2340px,dpi=440")
@Composable
private fun SkillIconsPreview() {
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        horizontalArrangement = Arrangement.Center
    ) {
        val skills = SkillHandler.newForPreviews(LocalContext.current).allSkillInfoList
        for (skill in skills) {
            Icon(
                painter = skill.icon(),
                contentDescription = null,
                tint = Color(0xFF6D9861),
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
