package com.newtranx.eval.tokenizers

import com.newtranx.eval.utils.LanguageUtil

/**
 * @Author: anson
 * @Date: 2022/1/30 5:02 PM
 */
class TokenizerUtil {
    companion object {
        @JvmStatic
        @JvmOverloads
        fun buildTokenizer(language: String): ITokenizer {
            return when (val lang = LanguageUtil.displayLanguage(language)) {
                "German", "English",
                "Spanish", "French" -> TokenizerEuro(lang)
                "Chinese" -> TokenizerZh()
                "Japanese" -> TokenizerJa()
                else -> TokenizerEuro("Unspecified")
            }
        }
    }
}