package org.dicio.skill.skill

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import org.dicio.skill.context.SkillContext

/**
 * Constructor for [SkillInfo], providing basic information about a skill
 * @param id a unique identifier for this skill (different from that of all other skills)
 */
abstract class SkillInfo(
    /**
     * @return a unique identifier for this skill (different from that of all other skills)
     */
    val id: String,
) {
    /**
     * @param context to call [Context.getString]
     * @return the skill name to show to the user in the current language (e.g. "Weather")
     */
    abstract fun name(context: Context): String

    /**
     * @param context to call [Context.getString]
     * @return an example of the usage of this skill to show to the user in the current language
     * (e.g. "What's the weather?")
     */
    abstract fun sentenceExample(context: Context): String

    /**
     * The skill icon to show to the user (e.g. an icon with sun and clouds representing weather)
     */
    @Composable
    abstract fun icon(): Painter

    /**
     * The category this skill belongs to for organizational purposes. This is used to group
     * skills on the main screen and in settings. Override this to assign a skill to a specific
     * category. If not overridden, skills will be placed in the "Other" category.
     * @return the string resource ID for the skill's category name (e.g. R.string.category_productivity)
     */
    open val categoryNameRes: Int = 0 // Will default to R.string.category_other in app module

    /**
     * Provides all of the permissions this skill needs in order to run. For example, the telephone
     * skill needs the `CALL_PHONE` and `READ_CONTACTS` permissions to run. The
     * permissions expressed here will be requested to the user when the skill is first used, or
     * via settings. A skill should therefore be able to be built with [.build]
     * without any permission, and a skill's input scoring (i.e. [Skill.score] and the
     * related methods) should also work without permissions.
     * @return all of the special permissions this skill requires, or an empty list if no special
     * permissions are needed
     */
    open val neededPermissions: List<Permission> = listOf()

    /**
     * Use this method to signal that the skill is not available in case, for example, the user
     * locale is not supported.
     * @param ctx the skill context with useful resources, see [SkillContext]
     * @return whether this skill can be used with the current system configuration or not
     */
    abstract fun isAvailable(ctx: SkillContext): Boolean

    /**
     * Builds an instance of the [Skill] this [SkillInfo] object represents.
     * @param ctx the skill context with useful resources, see [SkillContext]
     * @return a skill
     */
    abstract fun build(ctx: SkillContext): Skill<*>

    /**
     * Provides a settings screen for this skill, allowing the user to customize it to
     * their needs. This will be `null` if the skill has no settings.
     */
    open val renderSettings: (@Composable () -> Unit)? = null
}
