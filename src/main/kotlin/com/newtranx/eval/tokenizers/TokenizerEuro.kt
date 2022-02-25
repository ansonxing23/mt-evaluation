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

    override fun parse(text: String): String {
        return rawParse(text).joinToString(" ")
    }

    override fun rawParse(text: String): List<String> {
        val doc = CoreDocument(text)
        pipeline.annotate(doc)
        return doc.tokens().map { it.word() }
    }
}