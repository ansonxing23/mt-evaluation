package com.newtranx.eval.tokenizers

import com.hankcs.hanlp.tokenizer.StandardTokenizer

/**
 * @Author: anson
 * @Date: 2022/1/30 5:43 PM
 */
class TokenizerZh : ITokenizer {
    override fun parse(text: String): String {
        return rawParse(text).joinToString(" ")
    }

    override fun rawParse(text: String): List<String> {
        return StandardTokenizer.segment(text).map { it.word }
    }
}