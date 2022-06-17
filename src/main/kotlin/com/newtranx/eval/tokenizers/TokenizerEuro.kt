package com.newtranx.eval.tokenizers

import edu.stanford.nlp.pipeline.CoreDocument
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import java.util.*

/**
 * @Author: anson
 * @Date: 2022/1/30 5:18 PM
 */
class TokenizerEuro(
    language: String
) : ITokenizer {
    private val props = Properties()

    companion object {
        lateinit var pipeline: StanfordCoreNLP
    }

    init {
        props.setProperty("annotators", "tokenize")
        props.setProperty("tokenize.options", "splitHyphenated=false,americanize=false")
        props.setProperty("tokenize.language", language)
        pipeline = StanfordCoreNLP(props)
    }

    private fun preprocess(text: String): String {
        var line = if (text.endsWith("-")) {
            text.replace("-", "")
        } else text
        line = line.replace("<skipped>", "")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
        line = Regex("([0-9])(-|~|\\+|_)")
            .replace(line) { grp ->
                grp.groupValues.drop(1).joinToString("") {
                    "$it "
                }
            }
        return line
    }

    override fun parse(text: String): String {
        return rawParse(text).joinToString(" ")
    }

    override fun rawParse(text: String): List<String> {
        val doc = CoreDocument(preprocess(text))
        pipeline.annotate(doc)
        return doc.tokens().map { it.word() }
    }
}