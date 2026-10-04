package org.stypox.dicio.util

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import org.dicio.skill.context.SkillContext

fun SkillContext.getString(@StringRes resId: Int): String {
    return this.android.getString(resId)
}

fun SkillContext.getString(@StringRes resId: Int, vararg formatArgs: Any?): String {
    return this.android.getString(resId, *formatArgs)
}

fun SkillContext.getPluralString(
    @PluralsRes resId: Int,
    @StringRes resIdIfZero: Int,
    quantity: Int,
    vararg otherFormatArgs: Any?,
): String {
    if (quantity == 0 && resIdIfZero != 0) {
        return this.android.getString(resIdIfZero, quantity, *otherFormatArgs)
    }
    return this.android.resources.getQuantityString(resId, quantity, quantity, *otherFormatArgs)
}
