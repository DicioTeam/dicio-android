package org.stypox.dicio.skills.grocery_list

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillOutput
import org.stypox.dicio.R
import org.stypox.dicio.util.getString

data class GroceryListOutput(
    private val item: String,
    private val success: Boolean,
    private val needsConfiguration: Boolean
) : SkillOutput {
    override fun getSpeechOutput(ctx: SkillContext): String {
        return when {
            needsConfiguration -> ctx.getString(R.string.skill_grocery_list_not_configured)
            success -> ctx.getString(R.string.skill_grocery_list_added, item)
            else -> ctx.getString(R.string.skill_grocery_list_failed, item)
        }
    }

    @Composable
    override fun GraphicalOutput(ctx: SkillContext) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = getSpeechOutput(ctx),
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}