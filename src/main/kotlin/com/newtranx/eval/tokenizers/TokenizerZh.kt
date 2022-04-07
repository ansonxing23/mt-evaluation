package com.newtranx.eval.tokenizers

/**
 * @Author: anson
 * @Date: 2022/1/30 5:43 PM
 */
class TokenizerZh : ITokenizer {
    companion object {
        val UCODE_RANGES = listOf(
            Pair('\u3400', '\u4db5'),  // CJK Unified Ideographs Extension A, release 3.0
            Pair('\u4e00', '\u9fa5'),  // CJK Unified Ideographs, release 1.1
            Pair('\u9fa6', '\u9fbb'),  // CJK Unified Ideographs, release 4.1
            Pair('\uf900', '\ufa2d'),  // CJK Compatibility Ideographs, release 1.1
            Pair('\ufa30', '\ufa6a'),  // CJK Compatibility Ideographs, release 3.2
            Pair('\ufa70', '\ufad9'),  // CJK Compatibility Ideographs, release 4.1
//        Pair('\u20000', '\u2a6d6'),  // (UTF16) CJK Unified Ideographs Extension B, release 3.1
//        Pair('\u2f800', '\u2fa1d'),  // (UTF16) CJK Compatibility Supplement, release 3.1
            Pair('\uff00', '\uffef'),  // Full width ASCII, full width of English punctuation,
            // half width Katakana, half wide half width kana, Korean alphabet
            Pair('\u2e80', '\u2eff'),  // CJK Radicals Supplement
            Pair('\u3000', '\u303f'),  // CJK punctuation mark
            Pair('\u31c0', '\u31ef'),  // CJK stroke
            Pair('\u2f00', '\u2fdf'),  // Kangxi Radicals
            Pair('\u2ff0', '\u2fff'),  // Chinese character structure
            Pair('\u3100', '\u312f'),  // Phonetic symbols
            Pair('\u31a0', '\u31bf'),  // Phonetic symbols (Taiwanese and Hakka expansion)
            Pair('\ufe10', '\ufe1f'),
            Pair('\ufe30', '\ufe4f'),
            Pair('\u2600', '\u26ff'),
            Pair('\u2700', '\u27bf'),
            Pair('\u3200', '\u32ff'),
            Pair('\u3300', '\u33ff')
        )
        val wsRegex = Regex("\\s+")
        val punctRegex = Regex("[\\pP\\p{Punct}]")
    }

    private fun isChineseChar(char: Char): Boolean {
        UCODE_RANGES.forEach { pair ->
            val start = pair.first
            val end = pair.second
            if (char in start..end) {
                return true
            }
        }
        return false
    }

    override fun parse(text: String): String {
        val line = text.trim()
        var lineInChars = ""
        line.forEach { char ->
            if (isChineseChar(char) || punctRegex.matches(char.toString())) {
                lineInChars += " "
                lineInChars += char
                lineInChars += " "
            } else {
                lineInChars += char
            }
        }
        return lineInChars.replace(wsRegex, " ").trim()
    }

    override fun rawParse(text: String): List<String> {
        return parse(text).split(" ")
    }
}

fun main() {
    val tok = TokenizerZh()
    val sent = tok.parse("4月17日，外交部发言人赵立坚主持例行记者会test.")
    print(sent)
}