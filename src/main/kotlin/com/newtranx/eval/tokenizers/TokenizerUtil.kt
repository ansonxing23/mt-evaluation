package com.newtranx.eval.tokenizers

import com.newtranx.eval.enum.Language

/**
 * @Author: anson
 * @Date: 2022/1/30 5:02 PM
 */
class TokenizerUtil {
    companion object {
        fun buildTokenizer(language: Language): ITokenizer {
            return when (language) {
                Language.DE, Language.EN,
                Language.ES, Language.FR -> TokenizerEuro(language)
                Language.ZH -> TokenizerZh()
                Language.JA -> TokenizerJa()
                else -> TokenizerEuro(Language.UNSPECIFIED)
            }
        }
    }
}