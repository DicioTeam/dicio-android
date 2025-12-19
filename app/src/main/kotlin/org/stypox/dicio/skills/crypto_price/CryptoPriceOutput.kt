package org.stypox.dicio.skills.crypto_price

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillOutput
import org.stypox.dicio.R
import org.stypox.dicio.io.graphical.Body
import org.stypox.dicio.io.graphical.Headline
import org.stypox.dicio.io.graphical.HeadlineSpeechSkillOutput
import org.stypox.dicio.io.graphical.Subtitle
import org.stypox.dicio.util.getString

sealed interface CryptoPriceOutput : SkillOutput {
    data class Success(
        val cryptoName: String,
        val cryptoSymbol: String,
        val price: String
    ) : CryptoPriceOutput {
        override fun getSpeechOutput(ctx: SkillContext): String {
            return ctx.getString(R.string.skill_crypto_price_result, cryptoName, formatPrice(price))
        }

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Headline(text = "$cryptoName ($cryptoSymbol)")
                Spacer(modifier = Modifier.height(4.dp))
                Headline(text = "$${formatPrice(price)} USD")
            }
        }

        private fun formatPrice(price: String): String {
            val priceValue = price.toDoubleOrNull() ?: return price
            val decimalPlaces = price.substringAfter('.', "").length
            
            return if (decimalPlaces < 2) {
                String.format("%.2f", priceValue)
            } else {
                price
            }
        }
    }

    data class UnknownCryptocurrency(
        val crypto: String,
        val errorMessage: String
    ) : CryptoPriceOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = errorMessage
    }

    data class NetworkError(
        val errorMessage: String
    ) : CryptoPriceOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = errorMessage
    }

    data class InvalidResponse(
        val errorMessage: String
    ) : CryptoPriceOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = errorMessage
    }
}
