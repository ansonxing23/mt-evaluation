package com.newtranx.eval.metrics

import com.newtranx.eval.metrics.enums.Language
import com.newtranx.eval.metrics.nltk.Meteor
import com.newtranx.eval.metrics.nltk.Nist
import com.newtranx.eval.metrics.sacre.Bleu
import com.newtranx.eval.metrics.sacre.Ter
import com.newtranx.eval.tokenizers.TokenizerUtil
import edu.mit.jwi.IDictionary
import edu.mit.jwi.RAMDictionary
import edu.mit.jwi.data.ILoadPolicy
import org.tartarus.snowball.ext.*
import java.io.File
import java.net.URLDecoder

/**
 * @Author: anson
 * @Date: 2022/2/1 4:19 PM
 */
class MetricUtil {
    companion object {
        @JvmStatic
        fun buildBleuMetric(language: Language): IEvaluate {
            val tokenizer = TokenizerUtil.buildTokenizer(language)
            return Bleu(
                tokenizer = tokenizer,
                effectiveOrder = true
            )
        }

        @JvmStatic
        @JvmOverloads
        fun buildTerMetric(
            normalized: Boolean = false,
            noPunct: Boolean = false,
            asianSupport: Boolean = false,
            caseSensitive: Boolean = false
        ): IEvaluate {
            return Ter(
                normalized = normalized,
                noPunct = noPunct,
                asianSupport = asianSupport,
                caseSensitive = caseSensitive
            )
        }

        @JvmStatic
        @JvmOverloads
        fun buildNistMetric(
            asianSupport: Boolean = false,
            nGram: Int = 5
        ): IEvaluate {
            return Nist(
                asianSupport = asianSupport,
                nGram = nGram
            )
        }

        @JvmStatic
        @JvmOverloads
        fun buildMeteorMetric(
            wordnet: IDictionary,
            language: Language,
            lowercase: Boolean = true,
            alpha: Float = 0.9F,
            beta: Int = 3,
            gamma: Float = 0.5F
        ): IEvaluate {
            val stemmer = findStemmer(language)
            val asianSupport = when (language) {
                Language.ZH, Language.KO, Language.JA -> true
                else -> false
            }
            return Meteor(
                wordnet = wordnet,
                stemmer = stemmer,
                asianSupport = asianSupport,
                lowercase = lowercase,
                alpha = alpha,
                beta = beta,
                gamma = gamma
            )
        }

        @JvmStatic
        fun buildWordnet(path: String): RAMDictionary {
            return RAMDictionary(File(URLDecoder.decode(path, "UTF-8")), ILoadPolicy.IMMEDIATE_LOAD)
        }

        private fun findStemmer(language: Language) = when (language) {
            Language.EN -> EnglishStemmer()
            Language.FR -> FrenchStemmer()
            Language.DE -> German2Stemmer()
            Language.ES -> SpanishStemmer()
            else -> PorterStemmer()
        }
    }
}