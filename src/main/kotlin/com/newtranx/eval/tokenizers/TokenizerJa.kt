package com.newtranx.eval.tokenizers

import com.atilika.kuromoji.ipadic.Token
import com.atilika.kuromoji.ipadic.Tokenizer

/**
 * @Author: anson
 * @Date: 2022/2/14 9:51 PM
 */
class TokenizerJa : ITokenizer {
    override fun parse(text: String): String {
        return rawParse(text).joinToString(" ")
    }

    override fun rawParse(text: String): List<String> {
        val tokenizer = Tokenizer()
        val tokens: List<Token> = tokenizer.tokenize(text)
        return tokens.map { it.surface }
    }
}