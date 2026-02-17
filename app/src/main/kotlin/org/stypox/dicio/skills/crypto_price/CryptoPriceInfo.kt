package org.stypox.dicio.skills.crypto_price

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.Skill
import org.dicio.skill.skill.SkillInfo
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences

data object CryptoPriceInfo : SkillInfo("crypto_price") {
    override fun name(context: Context) =
        context.getString(R.string.skill_name_crypto_price)

    override fun sentenceExample(context: Context) =
        context.getString(R.string.skill_sentence_example_crypto_price)

    @Composable
    override fun icon() =
        rememberVectorPainter(Icons.Default.CurrencyBitcoin)

    override fun isAvailable(ctx: SkillContext): Boolean =
        Sentences.CryptoPrice[ctx.sentencesLanguage] != null

    override fun build(ctx: SkillContext): Skill<*> =
        CryptoPriceSkill(
            correspondingSkillInfo = this,
            data = Sentences.CryptoPrice[ctx.sentencesLanguage]!!
        )
}
