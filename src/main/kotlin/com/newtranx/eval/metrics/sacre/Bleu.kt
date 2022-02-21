package com.newtranx.eval.metrics.sacre

import com.newtranx.eval.metrics.BLEUScore
import com.newtranx.eval.tokenizers.ITokenizer
import com.newtranx.eval.utils.*
import kotlin.math.*

/**
 * @Author: anson
 * @Date: 2022/1/29 6:52 PM
 *
 * Computes BLEU for a single sentence against a single (or multiple) reference(s).
 * Disclaimer: Computing BLEU at the sentence level is not its intended use as
 * BLEU is a corpus-level metric.
 * @param hypothesis: A single hypothesis string.
 * @param references: A sequence of reference strings.
 * @param smooth_method: The smoothing method to use ('floor', 'add-k', 'exp' or 'none')
 * @param smooth_value: The smoothing value for `floor` and `add-k` methods. `None` falls back to default value.
 * @param lowercase: Lowercase the data
 * @param tokenize: The tokenizer to use
 * @param use_effective_order: Don't take into account n-gram orders without any match.
 * @return Returns a `BLEUScore` object.
 */
data class Bleu(
    var lowercase: Boolean = false,
    val tokenizer: ITokenizer,
    override var force: Boolean = false,
    var smoothMethod: String = "exp",
    var smoothValue: Double? = null,
    var maxNgramOrder: Int = 4,
    var effectiveOrder: Boolean = false
) : Base() {

    companion object {
        val SMOOTH_DEFAULTS = mapOf(
            "none" to null,   // No value is required
            "floor" to 0.1,
            "add-k" to 1.0,
            "exp" to null
        )
    }

    override fun sentenceScore(hypothesis: String, references: List<String>): BLEUScore {
        logger.warning("It is recommended to enable `effective_order` for sentence-level BLEU.")
            .takeIf { !effectiveOrder }
        return sentenceScore(hypothesis, references)
    }

    /**
     * Computes the final BLEU score given the pre-computed corpus statistics.
     * @param stats: A list of segment-level statistics
     * @return A `BLEUScore` instance.
     */
    override fun aggregateAndCompute(stats: List<List<Double>>): BLEUScore {
        return computeScoreFromStats(sumOfLists(stats))
    }

    /**
     * Computes the final score from already aggregated statistics.
     * @param stats: A list or numpy array of segment-level statistics.
     * @return A `BLEUScore` object.
     */
    private fun computeScoreFromStats(stats: List<Double>): BLEUScore {
        return computeBleu(
            correct = stats.subList(2, 2 + maxNgramOrder).toMutableList(),
            total = stats.subList(2 + maxNgramOrder, stats.size).toMutableList(),
            sysLen = stats[0],
            refLen = stats[1],
            smoothMethod = smoothMethod,
            smoothValue = smoothValue,
            effectiveOrder = effectiveOrder
        )
    }

    /**
     * Computes BLEU score from its sufficient statistics with smoothing.
     *
     * Smoothing methods (citing "A Systematic Comparison of Smoothing Techniques for Sentence-Level BLEU",
     * Boxing Chen and Colin Cherry, WMT 2014: http://aclweb.org/anthology/W14-3346)
     *
     * - none: No smoothing.
     * - floor: Method 1 (requires small positive value (0.1 in the paper) to be set)
     * - add-k: Method 2 (Generalizing Lin and Och, 2004)
     * - exp: Method 3 (NIST smoothing method i.e. in use with mteval-v13a.pl)
     *
     * @param correct: List of counts of correct ngrams, 1 <= n <= max_ngram_order
     * @param total: List of counts of total ngrams, 1 <= n <= max_ngram_order
     * @param sysLen: The cumulative system length
     * @param refLen: The cumulative reference length
     * @param smoothMethod: The smoothing method to use ('floor', 'add-k', 'exp' or 'none')
     * @param smoothValue: The smoothing value for `floor` and `add-k` methods. `None` falls back to default value.
     * @param effectiveOrder: If `True`, stop including n-gram orders for which precision is 0. This should be
     * `True`, if sentence-level BLEU will be computed.
     * @param maxNgramOrder: If given, it overrides the maximum n-gram order (default: 4) when computing precisions.
     * @return A `BLEUScore` instance.
     */
    private fun computeBleu(
        correct: MutableList<Double>,
        total: MutableList<Double>,
        sysLen: Double,
        refLen: Double,
        smoothMethod: String = "none",
        smoothValue: Double?,
        effectiveOrder: Boolean = false,
        maxNgramOrder: Int = 4
    ): BLEUScore {
        // Fetch the default value for floor and add-k
        val newSmoothValue = smoothValue ?: SMOOTH_DEFAULTS[smoothMethod]

        // Compute brevity penalty
        val bp = (if (sysLen < refLen) {
            exp((1 - refLen / sysLen)).takeIf { sysLen > 0 } ?: 0.0
        } else 1.0).toFloat()

        // n-gram precisions
        val precisions = DoubleArray(maxNgramOrder) { 0.0 }.toMutableList()

        // Early stop if there are no matches (#141)
        if (correct.isEmpty()) {
            return BLEUScore(0.0F, correct, total, precisions.toList(), bp, sysLen, refLen)
        }
        var smoothMteval = 1.0
        var effOrder = maxNgramOrder

        for (n in 1 until precisions.size + 1) {
            if (smoothMethod == "add-k" && n > 1) {
                correct[n - 1] += newSmoothValue ?: 0.0
                total[n - 1] += newSmoothValue ?: 0.0
            }
            if (total[n - 1] == 0.0) {
                break
            }
            // If the system guesses no i-grams, 1 <= i <= max_ngram_order,
            // the BLEU score is 0 (technically undefined). This is a problem for sentence
            // level BLEU or a corpus of short sentences, where systems will get
            // no credit if sentence lengths fall under the max_ngram_order threshold.
            // This fix scales max_ngram_order to the observed maximum order.
            // It is only available through the API and off by default
            if (effectiveOrder)
                effOrder = n
            if (correct[n - 1] == 0.0) {
                if (smoothMethod == "exp") {
                    smoothMteval *= 2.0
                    precisions[n - 1] = 100.0 / (smoothMteval * total[n - 1])
                } else if (smoothMethod == "floor") {
                    precisions[n - 1] = 100.0 * newSmoothValue!! / total[n - 1]
                }
            } else {
                precisions[n - 1] = 100.0 * correct[n - 1] / total[n - 1]
            }
        }
        // Compute BLEU score
        val score = bp * exp(precisions.subList(0, effOrder).map { myLog(it) }.sum() / effOrder).toFloat()
        return BLEUScore(score, correct, total, precisions, bp, sysLen, refLen)
    }

    /**
     * Given a (pre-processed) hypothesis sentence and already computed
     * reference n-grams & lengths, returns the best match statistics across the
     * references.
     * @param hypothesis: Hypothesis sentence.
     * @param refKwargs: A dictionary with `refs_ngrams`and `ref_lens` keys
     * that denote the counter containing all n-gram counts and reference lengths,
     * respectively.
     * @return A list of integers with match statistics.
     */
    override fun computeSegmentStatistics(hypothesis: String, refKwargs: SegmentStatistics): List<Double> {
        val refNgrams = refKwargs.refNgrams
        val refLens = refKwargs.refLens

        // Extract n-grams for the hypothesis
        val data = extractAllWordNgrams(hypothesis, 1, maxNgramOrder)
        val hypNgrams = data.first
        val hypLen = data.second
        val refLen = getClosestRefLen(hypLen, refLens)

        // Count the stats
        // Although counter has its internal & and | operators, this is faster
        val correct = DoubleArray(maxNgramOrder) { 0.0 }.toMutableList()
        val total = DoubleArray(maxNgramOrder) { 0.0 }.toMutableList()

        hypNgrams.forEach { hypNgram, hypCount ->
            // n-gram order
            val n = hypNgram.size - 1
            // count hypothesis n-grams
            val ori = total[n]
            total[n] = ori + hypCount
            // count matched n-grams
            if (refNgrams.exist(hypNgram)) {
                val cor = correct[n]
                correct[n] = cor + min(hypCount, refNgrams.count(hypNgram))
            }
        }
//        hypNgrams.map { key, value ->
//
//            val hypNgram = it.key
//            val hypCount = it.value
//            // n-gram order
//            val n = hypNgram.size - 1
//            // count hypothesis n-grams
//            total[n] += hypCount
//            // count matched n-grams
//            if (refNgrams.containsKey(hypNgram)) {
//                correct[n] += min(hypCount, refNgrams[hypNgram] ?: 0.0)
//            }
//        }

        // Return a flattened list for efficient computation
        val stat = mutableListOf<Double>()
        stat.add(hypLen)
        stat.add(refLen)
        stat.addAll(correct)
        stat.addAll(total)
        return stat
    }

    /**
     * Given a hypothesis length and a list of reference lengths, returns
     * the closest reference length to be used by BLEU.
     * @param hypLen: The hypothesis length.
     * @param refLens: A list of reference lengths.
     * @return The closest reference length.
     */
    private fun getClosestRefLen(hypLen: Double, refLens: List<Double>): Double {
        var closestDiff = -1.0
        var closestLen = -1.0
        refLens.forEach { refLen ->
            val diff = abs(hypLen - refLen)
            if (closestDiff == -1.0 || diff < closestDiff) {
                closestDiff = diff
                closestLen = refLen
            } else if (diff == closestDiff && refLen < closestLen) {
                closestLen = refLen
            }
        }
        return closestLen
    }

    /**
     * Given a sentence, lowercases (optionally) and tokenizes it
     * @param sent: The input sentence string.
     * @return The pre-processed output string.
     */
    override fun preprocessSegment(sent: String): String {
        val sentence = sent.toLowerCase().takeIf { lowercase } ?: sent
        return tokenizer.parse(sentence.trim())
    }

    /**
     * Given a list of reference segments, extract the required
     * information (such as n-grams for BLEU and chrF). This should be implemented
     * for the generic `_cache_references()` to work across all metrics.
     * @param refs: A sequence of strings.
     */
    override fun extractReferenceInfo(refs: List<String>): SegmentStatistics {
        var nGrams: Counter<List<String>>? = null
        val refLens = mutableListOf<Double>()

        refs.forEach {
            // extract n-grams for this ref
            val data = extractAllWordNgrams(it, 1, maxNgramOrder)
            val thisNgrams = data.first
            val refLen = data.second
            refLens.add(refLen)

            if (nGrams == null) {
                nGrams = thisNgrams
            } else {
                // Merge counts across multiple references
                // The below loop is faster than `ngrams |= this_ngrams`
                thisNgrams.forEach { ngram, count ->
//                    val count = ngram.value
                    nGrams!![ngram] = max(nGrams!!.count(ngram), count)
                }
            }
        }
        return SegmentStatistics(
            refNgrams = nGrams!!,
            refLens = refLens
        )
    }
}