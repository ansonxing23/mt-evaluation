package com.newtranx.eval.tokenizers

/**
 * @Author: anson
 * @Date: 2022/1/30 5:17 PM
 */
interface ITokenizer {
    fun parse(text: String): String
    fun rawParse(text: String): List<String>
}