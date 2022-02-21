package com.newtranx.eval.tokenizers

/**
 * @Author: anson
 * @Date: 2022/2/14 10:28 PM
 */
class TokenizerTer(
    private val normalized: Boolean = false,
    private val noPunct: Boolean = false,
    private val asianSupport: Boolean = false,
    private val caseSensitive: Boolean = false
) : ITokenizer {
    companion object {
        val PUNCT = Regex("[\\pP\\p{Punct}]")
    }

    override fun parse(text: String): String {
        return rawParse(text).joinToString(" ")
    }

    override fun rawParse(text: String): List<String> {
        if (text.isBlank()) return listOf("")

        var sent: String = text
        if (caseSensitive) {
            sent = text.toLowerCase()
        }
        if (normalized) {
            sent = normalizeGeneralAndWestern(sent)
            if (asianSupport)
                sent = normalizeAsian(sent)
        }

        if (noPunct) {
            sent = removePunct(sent)
        }
        // Strip extra whitespaces
        val wsRegex = Regex("\\s+")
        return sent.trim().split(wsRegex)
    }

    private fun removePunct(sent: String): String {
        return sent.replace(PUNCT, "")
    }

    private fun normalizeAsian(text: String): String {
        var sent = text
        // Split Chinese chars and Japanese kanji down to character level

        // 4E00—9FFF CJK Unified Ideographs
        // 3400—4DBF CJK Unified Ideographs Extension A
        sent = Regex("([\u4e00-\u9fff\u3400-\u4dbf])")
            .replace(sent) { " ${it.value} " }

        // 31C0—31EF CJK Strokes
        // 2E80—2EFF CJK Radicals Supplement
        sent = Regex("([\u31c0-\u31ef\u2e80-\u2eff])")
            .replace(sent) { " ${it.value} " }

        // 3300—33FF CJK Compatibility
        // F900—FAFF CJK Compatibility Ideographs
        // FE30—FE4F CJK Compatibility Forms
        sent = Regex("([\u3300-\u33ff\uf900-\ufaff\ufe30-\ufe4f])")
            .replace(sent) { " ${it.value} " }

        // 3200—32FF Enclosed CJK Letters and Months
        sent = Regex("([\u3200-\u3f22])")
            .replace(sent) { " ${it.value} " }

        // Split Hiragana, Katakana, and KatakanaPhoneticExtensions
        // only when adjacent to something else
        // 3040—309F Hiragana
        // 30A0—30FF Katakana
        // 31F0—31FF Katakana Phonetic Extensions
        // r"(^|^[\u3040-\u309f])([\u3040-\u309f]+)(?=$|^[\u3040-\u309f])"
        sent = Regex("(^|^[\u3040-\u309f])([\u3040-\u309f]+)(?=$|^[\u3040-\u309f])")
            .replace(sent) { grp ->
                grp.groupValues.drop(1).joinToString("") {
                    "$it "
                }.trim()
            }

        // r"(^|^[\u30a0-\u30ff])([\u30a0-\u30ff]+)(?=$|^[\u30a0-\u30ff])"
        sent = Regex("(^|^[\u30a0-\u30ff])([\u30a0-\u30ff]+)(?=$|^[\u30a0-\u30ff])")
            .replace(sent) { grp ->
                grp.groupValues.drop(1).joinToString("") {
                    "$it "
                }.trim()
            }

        // r"(^|^[\u31f0-\u31ff])([\u31f0-\u31ff]+)(?=$|^[\u31f0-\u31ff])"
        sent = Regex("(^|^[\u31f0-\u31ff])([\u31f0-\u31ff]+)(?=$|^[\u31f0-\u31ff])")
            .replace(sent) { grp ->
                grp.groupValues.drop(1).joinToString("") {
                    "$it "
                }.trim()
            }
        return PUNCT.replace(sent) { " ${it.value} " }
    }

    private fun normalizeGeneralAndWestern(text: String): String {
        var sent = text
        // language-independent (general) part

        // strip end-of-line hyphenation and join lines
        sent = sent.replace("\n-", "")

        // join lines
        sent = sent.replace("\n", " ")

        // handle XML escaped symbols
        sent = sent.replace("&quot;", "\"")
        sent = sent.replace("&amp;", "&")
        sent = sent.replace("&lt;", "<")
        sent = sent.replace("&gt;", ">")

        // language-dependent (Western) part
        sent = " $sent "

        // tokenize punctuation
        sent = Regex("([{-~\\[-` -&(-+:-@/])")
            .replace(sent) { " ${it.value} " }

        // handle possesives
        sent = sent.replace("'s ", " 's ")
        sent = sent.replace("'s$", " 's")

        // tokenize period and comma unless preceded by a digit
        // re.sub(r"([^0-9])([\.,])", r"\1 \2 ", sent)
        sent = Regex("([^0-9])([\\\\.,])").replace(sent) { grp ->
            grp.groupValues.drop(1).joinToString("") {
                "$it "
            }
        }

        // tokenize period and comma unless followed by a digit
        // re.sub(r"([\.,])([^0-9])", r" \1 \2", sent)
        sent = Regex("([\\\\.,])([^0-9])").replace(sent) { grp ->
            grp.groupValues.drop(1).joinToString("") {
                " $it"
            }
        }

        // tokenize dash when preceded by a digit
//        sent = re.sub(r"([0-9])(-)", r"\1 \2 ", sent)
        sent = Regex("([0-9])(-)").replace(sent) { grp ->
            grp.groupValues.drop(1).joinToString("") {
                "$it "
            }
        }
        return sent
    }
}