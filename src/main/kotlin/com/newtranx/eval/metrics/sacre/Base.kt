package com.newtranx.eval.metrics.sacre

import com.newtranx.eval.metrics.IEvaluate
import com.newtranx.eval.metrics.Score
import com.newtranx.eval.utils.zip
import java.util.logging.Logger

/**
 * @Author: anson
 * @Date: 2022/1/30 11:51 PM
 */
abstract class Base : IEvaluate {

    val logger: Logger = Logger.getLogger("sacre.Bleu")

    var refCache = listOf<SegmentStatistics>()
    open var force = true

    /**
     * Compute the metric for a corpus against a single (or multiple) reference(s).
     *
     * @param hypotheses: A sequence of hypothesis strings.
     * @param references: A sequence of reference documents with document being
     * defined as a sequence of reference strings. If `None`, cached references
     * will be used.
     * using bootstrap resampling with `n_bootstrap` samples.
     * @return A `Score` object.
     */
    override fun corpusScore(hypotheses: List<String>, references: List<List<String>>): Score {
        // Collect corpus stats
        val stats = extractCorpusStatistics(hypotheses, references)

        // Compute the actual system score
        return aggregateAndCompute(stats)
    }

    /**
     * Compute the metric for a single sentence against a single (or multiple) reference(s).
     * @param hypothesis: A single hypothesis string.
     * @param references: A sequence of reference strings.
     * @return a `BLEUScore` object.
     */
    override fun sentenceScore(hypothesis: String, references: List<String>): Score {
        val stats = extractCorpusStatistics(
            listOf(hypothesis), references.map { refs -> listOf(refs) }
        )
        return aggregateAndCompute(stats, true)
    }

    /**
     * Reads the corpus and returns sentence-level match statistics for
     * faster re-computations esp. during statistical tests.
     * @param hypothesis: A sequence of hypothesis strings.
     * @param references: A sequence of reference documents with document being
     * defined as a sequence of reference strings. If `None`, cached references
     * will be used.
     * @return A list where each sublist corresponds to segment statistics.
     */
    private fun extractCorpusStatistics(
        hypothesis: List<String>,
        references: List<List<String>>
    ): MutableList<List<Double>> {
        // Pre-compute references
        // Don't store the cache as the user is explicitly passing refs
        val refCache = when {
            references.isNotEmpty() -> {
                cacheReferences(references)
            }
            refCache.isNotEmpty() -> {
                refCache
            }
            else -> throw Exception("No references provided and the cache is empty.")
        }

        val stats = mutableListOf<List<Double>>()
        var tokCount = 0

        hypothesis.indices.forEach {
            // Check for already-tokenized input problem (only for BLEU)
            var hyp = hypothesis[it]
            val refKwargs = refCache[it]
            if (!force && hyp.endsWith(" .")) {
                tokCount += 1
            }
            hyp = preprocessSegment(hyp)
            // Collect stats
            stats.add(computeSegmentStatistics(hyp, refKwargs))
        }
        if (tokCount >= 100) {
            logger.warning("That's 100 lines that end in a tokenized period ('.')")
            logger.warning("It looks like you forgot to detokenize your test data, which may hurt your score.")
            logger.warning("If you insist your data is detokenized, or don't care, you can suppress this message with the `force` parameter.")
        }
        return stats
    }

    /**
     * Given the full set of document references, extract segment n-grams
     * (or other necessary information) for caching purposes.
     * @param references: A sequence of reference documents with document being
     * defined as a sequence of reference strings. A particular reference
     * segment can be '' or `None` to allow the use of variable number
     * of references per segment.
     * @return A list where each element is a tuple of segment n-grams and
     * reference lengths, as returned by `_extract_reference_info()`.
     */
    private fun cacheReferences(references: List<List<String>>): List<SegmentStatistics> {
        val refCache = mutableListOf<SegmentStatistics>()
        val numRefs = mutableSetOf<Int>()
        zip(*references.toTypedArray()).forEach { refs ->
            // remove undefined / empty references
            // i.e. we have fewer references for this particular sentence
            var lines = refs.filterNot { it.isEmpty() }

            if (lines.isEmpty())
                throw Exception("Empty or `None` reference sentence found.")
            // Keep track of reference counts to allow variable reference
            // info in the signature
            numRefs.add(lines.size)

            lines = lines.map { preprocessSegment(it) }
            // Get n-grams
            refCache.add(extractReferenceInfo(lines))
        }
//        if len(num_refs) == 1:
//            self.num_refs = list(num_refs)[0]
//        else:
//        # A variable number of refs exist
//                self.num_refs = -1
        return refCache
    }

    abstract fun aggregateAndCompute(stats: List<List<Double>>, sentenceLevel: Boolean = false): Score

    abstract fun preprocessSegment(sent: String): String

    abstract fun computeSegmentStatistics(hypothesis: String, refKwargs: SegmentStatistics): List<Double>

    abstract fun extractReferenceInfo(refs: List<String>): SegmentStatistics
}