package com.newtranx.eval.metrics.nltk

import com.newtranx.eval.metrics.EvaScore
import com.newtranx.eval.metrics.IEvaluate
import com.newtranx.eval.metrics.MetricUtil
import edu.mit.jwi.IDictionary
import edu.mit.jwi.RAMDictionary
import edu.mit.jwi.data.ILoadPolicy
import edu.mit.jwi.item.POS
import edu.mit.jwi.morph.WordnetStemmer
import org.tartarus.snowball.SnowballProgram
import org.tartarus.snowball.ext.PorterStemmer
import java.io.File
import java.net.URLDecoder
import kotlin.math.pow


/**
 * @Author: anson
 * @Date: 2022/2/20 12:17 AM
 */
/**
 * :param stemmer: nltk.stem.api.StemmerI object (default PorterStemmer())
 * :type stemmer: nltk.stem.api.StemmerI or any class that implements a stem method
 * :param wordnet: a wordnet corpus reader object (default nltk.corpus.wordnet)
 * :type wordnet: WordNetCorpusReader
 * :param alpha: parameter for controlling relative weights of precision and recall.
 * :type alpha: float
 * :param beta: parameter for controlling shape of penalty as a
 * function of as a function of fragmentation.
 * :type beta: float
 * :param gamma: relative weight assigned to fragmentation penality.
 * :type gamma: float
 * :return: The sentence-level METEOR score.
 */
class Meteor @JvmOverloads constructor(
    val stemmer: SnowballProgram = PorterStemmer(),
    val wordnet: IDictionary,
    val asianSupport: Boolean = false,
    var lowercase: Boolean = true,
    val alpha: Float = 0.9F,
    val beta: Int = 3,
    val gamma: Float = 0.5F
) : IEvaluate {
    private val wordnetStemmer = WordnetStemmer(wordnet)

    companion object {
        val WORD_POS = listOf(POS.NOUN, POS.VERB, POS.ADJECTIVE, POS.ADVERB)
    }

    init {
        wordnet.open()
    }

    override fun sentenceScore(hypothesis: String, references: List<String>): EvaScore {
        val score = references.map { reference ->
            singleMeteorScore(hypothesis, reference)
        }.maxOrNull() ?: 0.0
        return EvaScore(score)
    }

    override fun corpusScore(hypotheses: List<String>, references: List<List<String>>): EvaScore {
        val totalHyp = hypotheses.size
        references.forEach {
            if (totalHyp != it.size)
                throw Exception("The number of hypotheses and their reference(s) should be the same")
        }
        val listOfReferences = (0 until totalHyp).map { i ->
            val part = references.size
            (0 until part).map { j ->
                references[j][i]
            }
        }
        val total = hypotheses.indices.sumOf { i ->
            val hypothesis = hypotheses[i]
            val refs = listOfReferences[i]
            val score = refs.map { ref ->
                singleMeteorScore(hypothesis, ref)
            }.maxOrNull() ?: 0.0
            score
        }
        val score = total / hypotheses.size
        return EvaScore(score)
    }

    /**
     * :param references: reference sentences
     * :type references: list(str)
     * :param hypothesis: a hypothesis sentence
     * :type hypothesis: str
     * :param preprocess: preprocessing function (default str.lower)
     * :type preprocess: method
     * :rtype: float
     */
    fun singleMeteorScore(hypothesis: String, reference: String): Double {
        val enumHypothesis = generateEnums(hypothesis)
        val enumReference = generateEnums(reference)
        val translationLength = enumHypothesis.size
        val referenceLength = enumReference.size
        val matches = enumAllignWords(enumHypothesis, enumReference)
        val matchesCount = matches.size

        return try {
            val precision = matchesCount.toDouble() / translationLength
            val recall = matchesCount.toDouble() / referenceLength
            val fMean = (precision * recall) / (alpha * precision + (1 - alpha) * recall)
            val chunkCount = countChunks(matches).toDouble()
            val fragFrac = chunkCount / matchesCount
            val penalty = gamma * fragFrac.pow(beta)
            (1 - penalty) * fMean
        } catch (e: Exception) {
            0.0
        }
    }

    /**
     * Takes in string inputs for hypothesis and reference and returns
     * enumerated word lists for each of them
     *
     * @param sentence: hypothesis string
     * :type hypothesis: str
     * @return: enumerated words list
     * :rtype: list of 2D tuples
     */
    private fun generateEnums(sentence: String): MutableList<Pair<Int, String>> {
        var idx = -1
        return (sentence.toLowerCase().takeIf { lowercase } ?: sentence)
            .split(" ".takeIf { !asianSupport } ?: "")
            .mapNotNull {
                if (it.isNotBlank()) {
                    idx += 1
                    Pair(idx, it)
                } else null
            }.toMutableList()
    }

    /**
     * Aligns/matches words in the hypothesis to reference by sequentially
     * applying exact match, stemmed match and wordnet based synonym match.
     * in case there are multiple matches the match which has the least number
     * of crossing is chosen. Takes enumerated list as input instead of
     * string input
     *
     * @param enum_hypothesis_list: enumerated hypothesis list
     * @param enum_reference_list: enumerated reference list
     * @param stemmer: nltk.stem.api.StemmerI object (default PorterStemmer())
     * @return: sorted list of matched tuples, unmatched hypothesis list,
     * unmatched reference list
     * :rtype: list of tuples, list of tuples, list of tuples
     */
    private fun enumAllignWords(
        enum_hypothesis_list: MutableList<Pair<Int, String>>,
        enum_reference_list: MutableList<Pair<Int, String>>
    ): List<Pair<Int, Int>> {
        val exactMatches = matchEnums(enum_hypothesis_list, enum_reference_list)

        val stemMatches = enumStemMatch(enum_hypothesis_list, enum_reference_list)

        val wnsMatches = enumWordnetsynMatch(enum_hypothesis_list, enum_reference_list)

        return (exactMatches + stemMatches + wnsMatches).sortedBy { it.first }
    }

    /**
     * Stems each word and matches them in hypothesis and reference
     * and returns a word mapping between enum_hypothesis_list and
     * enum_reference_list based on the enumerated word id. The function also
     * returns a enumerated list of unmatched words for hypothesis and reference.
     *
     * @param enumHypothesisList:
     * :type enum_hypothesis_list:
     * @param enumReferenceList:
     * :type enum_reference_list:
     * @return: enumerated matched tuples, enumerated unmatched hypothesis tuples,
     * enumerated unmatched reference tuples
     * :rtype: list of 2D tuples, list of 2D tuples,  list of 2D tuples
     */
    private fun enumStemMatch(
        enumHypothesisList: MutableList<Pair<Int, String>>,
        enumReferenceList: MutableList<Pair<Int, String>>
    ): MutableList<Pair<Int, Int>> {
        val stemmedEnumList1 = enumHypothesisList.map {
            val idx = it.first
            stemmer.current = it.second
            stemmer.stem()
            val word = stemmer.current
            Pair(idx, word)
        }.toMutableList()

        val stemmedEnumList2 = enumReferenceList.map {
            val idx = it.first
            stemmer.current = it.second
            stemmer.stem()
            val word = stemmer.current
            Pair(idx, word)
        }.toMutableList()
        val wordMatch = matchEnums(stemmedEnumList1, stemmedEnumList2)

        val enumUnmatHypoList = listOf<MutableList<Any>>(mutableListOf(), mutableListOf())
            .takeIf { stemmedEnumList1.size > 0 } ?: listOf()
        stemmedEnumList1.forEach {
            enumUnmatHypoList[0].add(it.first)
            enumUnmatHypoList[1].add(it.second)
        }

        val enumUnmatRefList = listOf<MutableList<Any>>(mutableListOf(), mutableListOf())
            .takeIf { stemmedEnumList1.size > 0 } ?: listOf()
        stemmedEnumList2.forEach {
            enumUnmatRefList[0].add(it.first)
            enumUnmatRefList[1].add(it.second)
        }
        return wordMatch
    }

    /**
     * matches exact words in hypothesis and reference and returns
     * a word mapping between enum_hypothesis_list and enum_reference_list
     * based on the enumerated word id.
     *
     * :param enum_hypothesis_list: enumerated hypothesis list
     * :type enum_hypothesis_list: list of tuples
     * :param enum_reference_list: enumerated reference list
     * :type enum_reference_list: list of 2D tuples
     * :return: enumerated matched tuples, enumerated unmatched hypothesis tuples,
     * enumerated unmatched reference tuples
     * :rtype: list of 2D tuples, list of 2D tuples,  list of 2D tuples
     */
    private fun matchEnums(
        enumHypothesisList: MutableList<Pair<Int, String>>,
        enumReferenceList: MutableList<Pair<Int, String>>
    ): MutableList<Pair<Int, Int>> {
        val wordMatch = mutableListOf<Pair<Int, Int>>()
        val hypLen = enumHypothesisList.size - 1
        for (i in hypLen downTo 0) {
            val refLen = enumReferenceList.size - 1
            for (j in refLen downTo 0) {
                if (enumHypothesisList[i].second == enumReferenceList[j].second) {
                    wordMatch.add(
                        Pair(enumHypothesisList[i].first, enumReferenceList[j].first)
                    )
                    enumHypothesisList.removeAt(i)
                    enumReferenceList.removeAt(j)
                    break
                }
            }
        }
        return wordMatch
    }

    /**
     * Matches each word in reference to a word in hypothesis
     * if any synonym of a hypothesis word is the exact match
     * to the reference word.
     *
     * @param enum_hypothesis_list: enumerated hypothesis list
     * @param enum_reference_list: enumerated reference list
     * @param wordnet: a wordnet corpus reader object (default nltk.corpus.wordnet)
     * type wordnet: WordNetCorpusReader
     * @return: list of matched tuples, unmatched hypothesis list, unmatched reference list
     * rtype:  list of tuples, list of tuples, list of tuples
     */
    private fun enumWordnetsynMatch(
        enumHypothesisList: MutableList<Pair<Int, String>>,
        enumReferenceList: MutableList<Pair<Int, String>>
    ): MutableList<Pair<Int, Int>> {
        val wordMatch = mutableListOf<Pair<Int, Int>>()

        val hypLen = enumHypothesisList.size - 1
        for (i in hypLen downTo 0) {
            val hypothesisSyns = getSynSet(enumHypothesisList[i].second)
            val refLen = enumReferenceList.size - 1
            for (j in refLen downTo 0) {
                if (enumReferenceList[j].second in hypothesisSyns) {
                    wordMatch.add(
                        Pair(enumHypothesisList[i].first, enumReferenceList[j].first)
                    )
                    enumHypothesisList.removeAt(i)
                    enumReferenceList.removeAt(j)
                    break
                }
            }
        }
        return wordMatch
    }

    /**
     * Counts the fewest possible number of chunks such that matched unigrams
     * of each chunk are adjacent to each other. This is used to caluclate the
     * fragmentation part of the metric.
     *
     * @param matches: list containing a mapping of matched words (output of allign_words)
     * @return: Number of chunks a sentence is divided into post allignment
     * :rtype: int
     */
    private fun countChunks(matches: List<Pair<Int, Int>>): Int {
        var i = 0
        var chunks = 1
        while (i < matches.size - 1) {
            if ((matches[i + 1].first == matches[i].first + 1) &&
                (matches[i + 1].second == matches[i].second + 1)
            ) {
                i += 1
                continue
            }
            i += 1
            chunks += 1
        }
        return chunks
    }

    private fun getSynSet(lemma: String): MutableSet<String> {
        val hypothesisSyns = mutableSetOf<String>()
        WORD_POS.forEach { pos ->
            wordnetStemmer.findStems(lemma, pos).forEach {
                val idxWord = wordnet.getIndexWord(it, pos)
                idxWord?.wordIDs?.forEach { wordId ->
                    val words = wordnet.getSynset(wordId.synsetID).words
                    words.forEach { word ->
                        val set = word.synset
                        set.words.forEach { syn ->
                            if (syn.lemma.indexOf("_") < 0) {
                                hypothesisSyns.add(syn.lemma)
                            }
                        }
                    }
                }
            }
        }
        hypothesisSyns.add(lemma)
        return hypothesisSyns
    }
}