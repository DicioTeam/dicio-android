package org.stypox.dicio.skills.crypto_price

import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.json.JSONException
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences.CryptoPrice
import org.stypox.dicio.util.ConnectionUtils
import java.io.IOException

class CryptoPriceSkill(
    correspondingSkillInfo: SkillInfo,
    data: StandardRecognizerData<CryptoPrice>
) : StandardRecognizerSkill<CryptoPrice>(correspondingSkillInfo, data) {

    data class Cryptocurrency(
        val symbol: String,
        val name: String
    )

    companion object {
        private val SUPPORTED_CRYPTOCURRENCIES = listOf(
            Cryptocurrency("BTC", "Bitcoin"),
            Cryptocurrency("ETH", "Ethereum"),
            Cryptocurrency("LTC", "Litecoin"),
            Cryptocurrency("XRP", "Ripple"),
            Cryptocurrency("SOL", "Solana"),
            Cryptocurrency("DOGE", "Dogecoin"),
            Cryptocurrency("ADA", "Cardano")
        )
    }

    override suspend fun generateOutput(
        ctx: SkillContext,
        inputData: CryptoPrice
    ): SkillOutput {
        return when (inputData) {
            is CryptoPrice.Price -> {
                val cryptoInput = inputData.crypto?.trim() ?: ""
                
                if (cryptoInput.isBlank()) {
                    return CryptoPriceOutput.UnknownCryptocurrency(
                        crypto = "",
                        errorMessage = ctx.android.getString(R.string.skill_crypto_price_unknown_crypto, "")
                    )
                }

                // Find matching cryptocurrency
                val crypto = findCryptocurrency(cryptoInput)
                if (crypto == null) {
                    return CryptoPriceOutput.UnknownCryptocurrency(
                        crypto = cryptoInput,
                        errorMessage = ctx.android.getString(R.string.skill_crypto_price_unknown_crypto, cryptoInput)
                    )
                }

                // Fetch price from OKX API
                fetchCryptoPrice(ctx, crypto)
            }
        }
    }

    private fun findCryptocurrency(input: String): Cryptocurrency? {
        val normalizedInput = input.lowercase().trim()
        
        // First try exact match
        val exactMatch = SUPPORTED_CRYPTOCURRENCIES.find { crypto ->
            crypto.symbol.lowercase() == normalizedInput ||
            crypto.name.lowercase() == normalizedInput
        }
        if (exactMatch != null) return exactMatch
        
        // Try fuzzy matching for STT errors (e.g., "like coin" -> "litecoin")
        val inputNoSpaces = normalizedInput.replace(" ", "")
        
        // Find best match using Levenshtein distance
        var bestMatch: Cryptocurrency? = null
        var bestScore = Int.MAX_VALUE
        
        for (crypto in SUPPORTED_CRYPTOCURRENCIES) {
            val nameNoSpaces = crypto.name.lowercase().replace(" ", "")
            val symbolNoSpaces = crypto.symbol.lowercase().replace(" ", "")
            
            val nameDistance = levenshteinDistance(inputNoSpaces, nameNoSpaces)
            val symbolDistance = levenshteinDistance(inputNoSpaces, symbolNoSpaces)
            val minDistance = minOf(nameDistance, symbolDistance)
            
            // Accept if distance is small enough (allow ~50% error rate for STT corrections)
            val maxLen = maxOf(inputNoSpaces.length, nameNoSpaces.length, symbolNoSpaces.length)
            val threshold = (maxLen * 0.5).toInt().coerceAtLeast(3)
            if (minDistance < bestScore && minDistance <= threshold) {
                bestScore = minDistance
                bestMatch = crypto
            }
        }
        
        return bestMatch
    }
    
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[m][n]
    }

    private fun fetchCryptoPrice(ctx: SkillContext, crypto: Cryptocurrency): CryptoPriceOutput {
        return try {
            val url = "https://www.okx.com/api/v5/market/ticker?instId=${crypto.symbol}-USD"
            val json = ConnectionUtils.getPageJson(url)

            // Validate response structure
            if (!json.has("code") || !json.has("data")) {
                return CryptoPriceOutput.InvalidResponse(
                    errorMessage = ctx.android.getString(R.string.skill_crypto_price_invalid_response)
                )
            }

            val code = json.getString("code")
            if (code != "0") {
                return CryptoPriceOutput.InvalidResponse(
                    errorMessage = ctx.android.getString(R.string.skill_crypto_price_invalid_response)
                )
            }

            val dataArray = json.getJSONArray("data")
            if (dataArray.length() == 0) {
                return CryptoPriceOutput.InvalidResponse(
                    errorMessage = ctx.android.getString(R.string.skill_crypto_price_invalid_response)
                )
            }

            val tickerData = dataArray.getJSONObject(0)
            if (!tickerData.has("last")) {
                return CryptoPriceOutput.InvalidResponse(
                    errorMessage = ctx.android.getString(R.string.skill_crypto_price_invalid_response)
                )
            }

            val price = tickerData.getString("last")
            
            // Validate that price is a valid number
            try {
                price.toDouble()
            } catch (e: NumberFormatException) {
                return CryptoPriceOutput.InvalidResponse(
                    errorMessage = ctx.android.getString(R.string.skill_crypto_price_invalid_response)
                )
            }

            CryptoPriceOutput.Success(
                cryptoName = crypto.name,
                cryptoSymbol = crypto.symbol,
                price = price
            )

        } catch (e: IOException) {
            CryptoPriceOutput.NetworkError(
                errorMessage = ctx.android.getString(R.string.skill_crypto_price_network_error)
            )
        } catch (e: JSONException) {
            CryptoPriceOutput.InvalidResponse(
                errorMessage = ctx.android.getString(R.string.skill_crypto_price_invalid_response)
            )
        }
    }
}
