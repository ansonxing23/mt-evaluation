package com.newtranx.eval.metrics.nltk

import com.newtranx.eval.metrics.IEvaluate
import com.newtranx.eval.metrics.EvaScore
import com.newtranx.eval.metrics.Score
import com.newtranx.eval.utils.Counter
import com.newtranx.eval.utils.extractNgrams
import com.newtranx.eval.utils.map
import kotlin.math.*

/**
 * @Author: anson
 * @Date: 2022/2/19 2:36 PM
 */
class Nist @JvmOverloads constructor(
    val asianSupport: Boolean = false,
    val nGram: Int = 5
) : IEvaluate {

    override fun sentenceScore(hypothesis: String, references: List<String>): EvaScore {
        return corpusScore(listOf(hypothesis), references.map { listOf(it) })
    }

    override fun corpusScore(hypes: List<String>, refs: List<List<String>>): EvaScore {
        val total = hypes.size
        refs.forEach {
            if (total != it.size)
                throw Exception("The number of hypotheses and their reference(s) should be the same")
        }
        val listOfReferences = (0 until total).map { i ->
            val part = refs.size
            (0 until part).map { j ->
                val sent = refs[j][i]
                sent.split(" ".takeIf { !asianSupport } ?: "").filter { it.isNotBlank() }
            }
        }

        val hypotheses = hypes.map { hyp -> hyp.split(" ".takeIf { !asianSupport } ?: "").filter { it.isNotBlank() } }
        // Collect the ngram coounts from the reference sentences.
        val ngramFreq = Counter<List<String>>()
        var totalReferenceWords = 0
        for (references in listOfReferences) { // For each source sent, there's a list of reference sents.
            for (reference in references) { // For each order of ngram, count the ngram occurrences.
                for (i in (1 until nGram)) {
                    val ngrams = extractNgrams(reference, i)
                    ngramFreq.update(ngrams)
                }
                totalReferenceWords += reference.size
            }
        }
        // Compute the information weights based on the reference sentences.
        // Eqn 2 in Doddington (2002):
        // Info(w_1 ... w_n) = log_2 [ (# of occurrences of w_1 ... w_n-1) / (# of occurrences of w_1 ... w_n) ]
        val informationWeights = mutableMapOf<List<String>, Double>()
        for (_ngram in ngramFreq.keys()) { // w_1 ... w_n
            val mgram = _ngram.take(_ngram.size - 1)
            // From https://github.com/moses-smt/mosesdecoder/blob/master/scripts/generic/mteval-v13a.pl#L546
            // it's computed as such:
            //     denominator = ngram_freq[_mgram] if _mgram and _mgram in ngram_freq else denominator = total_reference_words
            //     information_weights[_ngram] = -1 * math.log(ngram_freq[_ngram]/denominator) / math.log(2)
            //
            // Mathematically, it's equivalent to the our implementation:
            val numerator = if (ngramFreq.exist(mgram)) {
                ngramFreq.count(mgram)
            } else {
                totalReferenceWords
            }
            informationWeights[_ngram] = ln(numerator.toDouble() / ngramFreq.count(_ngram)) / ln(2.0)
        }

        // Micro-average.
        val nistPrecisionNumeratorPerNgram = mutableMapOf<Int, Double>()
        val nistPrecisionDenominatorPerNgram = mutableMapOf<Int, Int>()
        var lRef = 0
        var lSys = 0
        // For each order of ngram.
        for (i in (1 until nGram + 1)) {
            // Iterate through each hypothesis and their corresponding references.
            listOfReferences.indices.forEach {
                val references = listOfReferences[it]
                val hypothesis = hypotheses[it]
                val hypLen = hypothesis.size

                // Find reference with the best NIST score.
                val nistPrecisionPerRef = mutableListOf<Double>()
                val nistNumeratorPerRef = mutableListOf<Double>()
                val nistDenominatorPerRef = mutableListOf<Int>()
                val nistRefLenPerRef = mutableListOf<Int>()
                for (reference in references) {
                    val refLen = reference.size
                    // Counter of ngrams in hypothesis.
                    val hypNgrams = if (hypothesis.size >= i) {
                        val ngram = extractNgrams(hypothesis, i)
                        Counter(ngram)
                    } else Counter()
                    val refNgrams = if (reference.size >= i) {
                        val ngram = extractNgrams(reference, i)
                        Counter(ngram)
                    } else Counter()
                    val ngramOverlaps = hypNgrams.intersect(refNgrams)
                    // Precision part of the score in Eqn 3
                    val numerator = ngramOverlaps.map { _ngram, count ->
                        (informationWeights[_ngram] ?: 0.0).times(count)
                    }.sum()
                    val denominator = hypNgrams.values().sum()
                    val precision = 0.0.takeIf { denominator == 0 } ?: (numerator / denominator)
                    nistPrecisionPerRef.add(precision)
                    nistNumeratorPerRef.add(numerator)
                    nistDenominatorPerRef.add(denominator)
                    nistRefLenPerRef.add(refLen)
                }
                // Best reference.
                val precision = nistPrecisionPerRef.maxOrNull() ?: 0.0
                val numerator = nistNumeratorPerRef.maxOrNull() ?: 0.0
                val denominator = nistDenominatorPerRef.maxOrNull() ?: 0
                val refLen = nistRefLenPerRef.maxOrNull() ?: 0
                nistPrecisionNumeratorPerNgram[i] =
                    (nistPrecisionNumeratorPerNgram[i] ?: 0.0).plus(numerator)
                nistPrecisionDenominatorPerNgram[i] =
                    (nistPrecisionDenominatorPerNgram[i] ?: 0).plus(denominator)
                lRef += refLen
                lSys += hypLen
            }

        }
        // Final NIST micro-average mean aggregation.
        var nistPrecision = 0.0
        for (i in nistPrecisionNumeratorPerNgram.keys) {
            val precision = (nistPrecisionNumeratorPerNgram[i] ?: 0.0).div(
                ((nistPrecisionDenominatorPerNgram[i] ?: 0).toDouble().plus(1e-5))
            )
            nistPrecision += precision
        }
        // Eqn 3 in Doddington(2002)
        return EvaScore((nistPrecision * nistLengthPenalty(lRef, lSys)))
    }

    override fun singleSentenceScore(hypothesis: String, reference: String): Score {
        return corpusScore(listOf(hypothesis), listOf(listOf(reference)))
    }

    /**
     * Calculates the NIST length penalty, from Eq. 3 in Doddington (2002)
     *
     * penalty = exp( beta * log( min( len(hyp)/len(ref) , 1.0 )))
     *
     * where,
     *
     * `beta` is chosen to make the brevity penalty factor = 0.5 when the
     * no. of words in the system output (hyp) is 2/3 of the average
     * no. of words in the reference translation (ref)
     *
     * The NIST penalty is different from BLEU's such that it minimize the impact
     * of the score of small variations in the length of a translation.
     * See Fig. 4 in  Doddington (2002)
     */
    private fun nistLengthPenalty(ref_len: Int, hyp_len: Int): Double {
        val ratio = hyp_len.toDouble() / ref_len
        return if (0 < ratio && ratio < 1) {
            val ratioX = 1.5
            val scoreX = 0.5
            val beta = ln(scoreX) / ln(ratioX).pow(2)
            exp(beta * ln(ratio).pow(2))
        } else {
            // ratio <= 0 or ratio >= 1
            max(min(ratio, 1.0), 0.0)
        }
    }
}