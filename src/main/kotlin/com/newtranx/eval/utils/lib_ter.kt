package com.newtranx.eval.utils

import com.newtranx.eval.metrics.sacre.EditRate
import java.util.*
import kotlin.math.*


/**
 * @Author: anson
 * @Date: 2022/2/1 10:23 PM
 */
const val COST_INS = 1
const val COST_DEL = 1
const val COST_SUB = 1

// Tercom-inspired limits
const val MAX_SHIFT_SIZE = 10
const val MAX_SHIFT_DIST = 50
const val BEAM_WIDTH = 25F

// Our own limits
const val MAX_CACHE_SIZE = 10000
const val MAX_SHIFT_CANDIDATES = 1000
const val INT_INFINITY = Int.MAX_VALUE - 10000

const val OP_INS = 'i'
const val OP_DEL = 'd'
const val OP_NOP = ' '
const val OP_SUB = 's'
const val OP_UNDEF = 'x'

val FLIP_OPS = MakeTrans(OP_INS.toString() + OP_DEL, OP_DEL.toString() + OP_INS)

/**
 * Calculate the translation edit rate.
 *
 * @param words_hyp: Tokenized translation hypothesis.
 * @param words_ref: Tokenized reference translation.
 * @return: tuple (number of edits, length)
 */
fun translationEditRate(words_hyp: List<String>, words_ref: List<String>): EditRate {
    val nWordsRef = words_ref.size
    val nWordsHyp = words_hyp.size
    if (nWordsRef == 0) {
        // special treatment of empty refs
        return EditRate(nWordsHyp, 0)
    }
    val cachedEd = BeamEditDistance(words_ref)
    var shifts = 0

    var inputWords = words_hyp
    var checkedCandidates = 0
    while (true) {
        // do shifts until they stop reducing the edit distance
        val data = shift(inputWords, words_ref, cachedEd, checkedCandidates)
        val delta = data.first
        val newInputWords = data.second
        checkedCandidates = data.third

        if (checkedCandidates >= MAX_SHIFT_CANDIDATES)
            break
        if (delta <= 0)
            break
        shifts += 1
        inputWords = newInputWords
    }
    val cached = cachedEd.calculate(inputWords)
    val editDistance = cached.first
    val totalEdits = shifts + editDistance
    return EditRate(totalEdits, nWordsRef)
}

/**
 * Attempt to shift words in hypothesis to match reference.
 *
 * Returns the shift that reduces the edit distance the most.
 *
 * Note that the filtering of possible shifts and shift selection are heavily
 * based on somewhat arbitrary heuristics. The code here follows as closely
 * as possible the logic in Tercom, not always justifying the particular design
 * choices.
 *
 * @param words_h: Hypothesis.
 * @param words_r: Reference.
 * @param cached_ed: Cached edit distance.
 * @param checked_candidates_count: Number of shift candidates that were already
 * evaluated.
 * @return: (score, shifted_words, checked_candidates). Best shift and updated
 * number of evaluated shift candidates.
 */
fun shift(
    words_h: List<String>,
    words_r: List<String>,
    cached_ed: BeamEditDistance,
    checked_candidates_count: Int
): Triple<Int, List<String>, Int> {
    val cached = cached_ed.calculate(words_h)
    val preScore = cached.first
    val invTrace = cached.second

//     to get alignment, we pretend we are rewriting reference into hypothesis,
//     so we need to flip the trace of edit operations
    val trace = flipTrace(invTrace)
    val traceAlign = traceToAlignment(trace)
    val align = traceAlign.first
    val refErr = traceAlign.second
    val hypErr = traceAlign.third

    var best: Ranking? = null
    var checkedCandidates = checked_candidates_count

    for (pairs in findShiftedPairs(words_h, words_r)) {
        val startH = pairs.first
        val startR = pairs.second
        val length = pairs.third
        // don't do the shift unless both the hypothesis was wrong and the
        // reference doesn't match hypothesis at the target position
        if (hypErr.subList(startH, startH + length).sum() == 0)
            continue

        if (refErr.subList(startR, startR + length).sum() == 0)
            continue
        // don't try to shift within the subsequence
        if (startH <= align[startR] ?: 0 && align[startR] ?: 0 < startH + length)
            continue
        var prevIdx = -1
        for (offset in (-1 until length)) {
            val idx = if (startR + offset == -1)
                0  // insert before the beginning
            else if (startR + offset in align) {
                // Unlike Tercom which inserts *after* the index, we insert
                // *before* the index.
                (align[startR + offset] ?: 0).plus(1)
            } else
                break  // offset is out of bounds => aims past reference
            if (idx == prevIdx)
                continue  // skip idx if already tried

            prevIdx = idx

            val shiftedWords = performShift(words_h, startH, length, idx)

            // Elements of the tuple are designed to replicate Tercom ranking
            // of shifts:
            val candidate = Ranking(
                preScore - cached_ed.calculate(shiftedWords).first,  // highest score first
                length,  // then, longest match first
                -startH,  // then, earliest match first
                -idx,   // then, earliest target position first
                shiftedWords
            )

            checkedCandidates += 1

            if (best == null || (candidate.score > best.score))
                best = candidate
        }
        if (checkedCandidates >= MAX_SHIFT_CANDIDATES)
            break
    }
    return if (best == null)
        Triple(0, words_h, checkedCandidates)
    else {
        Triple(best.score, best.shiftedWords, checkedCandidates)
    }
}

/**
 * Perform a shift in `words` from `start` to `target`.
 *
 * @param words: Words to shift.
 * @param start: Where from.
 * @param length: How many words.
 * @param target: Where to.
 * @return: Shifted words.
 */
fun performShift(words: List<String>, start: Int, length: Int, target: Int): List<String> {
    return when {
        target < start -> {
            // shift before previous position
            words.subList(0, target) + words.subList(start, start + length) +
                    words.subList(target, start) + words.subList(start + length, words.size)
        }
        target > start + length -> {
            // shift after previous position
            words.subList(0, start) + words.subList(start + length, target) +
                    words.subList(start, start + length) + words.subList(target, words.size)
        }
        else -> {
            // shift within the shifted string
            words.subList(0, start) + words.subList(start + length, length + target) +
                    words.subList(start, start + length) + words.subList(length + target, words.size)
        }
    }
}

/**
 * Find matching word sub-sequences in two lists of words.
 *
 * Ignores sub-sequences starting at the same position.
 *
 * @param words_h: First word list.
 * @param words_r: Second word list.
 * @return: Yields tuples of (h_start, r_start, length) such that:
 * words_h[h_start:h_start+length] = words_r[r_start:r_start+length]
 */
fun findShiftedPairs(words_h: List<String>, words_r: List<String>): MutableList<Triple<Int, Int, Int>> {
    var nWordsH = words_h.size
    var nWordsR = words_r.size
    val list = mutableListOf<Triple<Int, Int, Int>>()
    for (start_h in 0 until nWordsH) {
        for (start_r in 0 until nWordsR) {
            // this is slightly different from what tercom does but this should
            // really only kick in in degenerate cases
            if (abs(start_r - start_h) > MAX_SHIFT_DIST)
                continue

            var length = 0
            while (words_h[start_h + length] == words_r[start_r + length] && length < MAX_SHIFT_SIZE) {

                length += 1

                list.add(Triple(start_h, start_r, length))

                // If one sequence is consumed, stop processing
                if (nWordsH == start_h + length || nWordsR == start_r + length)
                    break
            }
        }
    }
    return list
}

/**
 * Flip the trace of edit operations.
 * Instead of rewriting a->b, get a recipe for rewriting b->a.
 * Simply flips insertions and deletions.
 */
fun flipTrace(trace: String): String {
    return FLIP_OPS.translate(trace)
}

/**
 * Transform trace of edit operations into an alignment of the sequences.
 *
 * @param trace: Trace of edit operations (' '=no change or 's'/'i'/'d').
 * @return Alignment, error positions in reference, error positions in hypothesis.
 */
fun traceToAlignment(trace: String): Triple<Map<Int, Int>, List<Int>, List<Int>> {
    var posHyp = -1
    var posRef = -1
    var hypErr = mutableListOf<Int>()
    var refErr = mutableListOf<Int>()
    var align = mutableMapOf<Int, Int>()

    // we are rewriting a into b
    for (op in trace) {
        when (op) {
            OP_NOP -> {
                posHyp += 1
                posRef += 1
                align[posRef] = posHyp
                hypErr.add(0)
                refErr.add(0)
            }
            OP_SUB -> {
                posHyp += 1
                posRef += 1
                align[posRef] = posHyp
                hypErr.add(1)
                refErr.add(1)
            }
            OP_INS -> {
                posHyp += 1
                hypErr.add(1)
            }
            OP_DEL -> {
                posRef += 1
                align[posRef] = posHyp
                refErr.add(1)
            }
            else -> throw Exception("unknown operation $op")
        }
    }
    return Triple(align, refErr, hypErr)
}

/**
 * Edit distance with several features required for TER calculation.
 *
 * internal cache
 * "beam" search
 * tracking of edit operations
 *
 * The internal self._cache works like this:
 *
 * Keys are words of the hypothesis. Values are tuples (next_node, row) where:
 *
 * next_node is the cache for the next word in the sequence
 * row is the stored row of the edit distance matrix
 *
 * Effectively, caching allows to skip several rows in the edit distance
 * matrix calculation and instead, to initialize the computation with the last
 * matching matrix row.
 *
 * Beam search, as implemented here, only explores a fixed-size sub-row of
 * candidates around the matrix diagonal (more precisely, it's a
 * "pseudo"-diagonal since we take the ratio of sequence lengths into account).
 *
 * Tracking allows to reconstruct the optimal sequence of edit operations.
 *
 * :param words_ref: A list of reference tokens.
 */
data class BeamEditDistance(val words_ref: List<String>) {
    private val wordsRef = words_ref
    private val nWordsRef = words_ref.size

    // first row corresponds to insertion operations of the reference,
    // so we do 1 edit operation per reference word
    private val initialRow = (0 until (nWordsRef + 1)).map { i ->
        Pair(i * COST_INS, OP_INS)
    }.toMutableList()

    private val cache = mutableMapOf<String, Node>()  // type: Dict[str, Tuple]
    private var cacheSize = 0

    // Precomputed empty matrix row. Contains infinities so that beam search
    // avoids using the uninitialized cells.
    private val emptyRow = (0 until (nWordsRef + 1)).map {
        Pair(INT_INFINITY, OP_UNDEF)
    }

    /**
     * Calculate edit distance between self._words_ref and the hypothesis.
     *
     * Uses cache to skip some of the computation.
     *
     * @param words_hyp: Words in translation hypothesis.
     * @return Edit distance score.
     */
    fun calculate(words_hyp: List<String>): Pair<Int, String> {
        // skip initial words in the hypothesis for which we already know the
        // edit distance
        val cacheRef = findCache(words_hyp)
        val startPosition = cacheRef.first
        val dist = cacheRef.second

        // calculate the rest of the edit distance matrix
        val ed = editDistance(words_hyp, startPosition, dist)
        val editDistance = ed.first
        val newlyCreatedMatrix = ed.second
        val trace = ed.third

        // update our cache with the newly calculated rows
        addCache(words_hyp, newlyCreatedMatrix)

        return Pair(editDistance, trace)
    }

    /**
     * Actual edit distance calculation.
     *
     * Can be initialized with the last cached row and a start position in
     * the hypothesis that it corresponds to.
     *
     * @param words_h: Words in translation hypothesis.
     * @param start_h: Position from which to start the calculation.
     * (This is zero if no cache match was found.)
     * @param cache: Precomputed rows corresponding to edit distance matrix
     * before `start_h`.
     * @return: Edit distance value, newly computed rows to update the
     * cache, trace.
     */
    private fun editDistance(
        words_h: List<String>, start_h: Int,
        cache: MutableList<MutableList<Pair<Int, Char>>>
    ): Triple<Int, MutableList<MutableList<Pair<Int, Char>>>, String> {
        val nWordsH = words_h.size

        // initialize the rest of the matrix with infinite edit distances
        val restEmpty = (0 until (nWordsH - start_h)).map {
            emptyRow.toMutableList()
        }.toMutableList()

        val dist = (cache + restEmpty).toMutableList()
        val lengthRatio: Float = (nWordsRef.toFloat() / nWordsH.toFloat()).takeIf { words_h.isNotEmpty() } ?: 1F

        // in some crazy sentences, the difference in length is so large that
        // we may end up with zero overlap with previous row
        val beamWidth = if (BEAM_WIDTH < (lengthRatio / 2)) {
            ceil(lengthRatio / 2 + BEAM_WIDTH)
        } else {
            BEAM_WIDTH
        }
        // calculate the Levenshtein distance
        for (i in (start_h + 1 until nWordsH + 1)) {
            val pseudoDiag = floor(i * lengthRatio)
            val minJ = max(0F, pseudoDiag - beamWidth)
            var maxJ = min((nWordsRef + 1).toFloat(), pseudoDiag + beamWidth)

            if (i == nWordsH)
                maxJ = (nWordsRef + 1).toFloat()

            for (j in (minJ.toInt() until maxJ.toInt())) {
                if (j == 0) {
                    dist[i][j] = Pair(dist[i - 1][j].first + COST_DEL, OP_DEL)
                } else {
                    var costSub = COST_SUB
                    var opSub = OP_SUB
                    if (words_h[i - 1] == wordsRef[j - 1]) {
                        costSub = 0
                        opSub = OP_NOP
                    }

                    // Tercom prefers no-op/sub, then insertion, then deletion.
                    // But since we flip the trace and compute the alignment from
                    // the inverse, we need to swap order of insertion and
                    // deletion in the preference.
                    val ops = listOf(
                        Pair(dist[i - 1][j - 1].first + costSub, opSub),
                        Pair(dist[i - 1][j].first + COST_DEL, OP_DEL),
                        Pair(dist[i][j - 1].first + COST_INS, OP_INS),
                    )

                    for (op in ops) {
                        val opCost = op.first
                        val opName = op.second
                        if (dist[i][j].first > opCost)
                            dist[i][j] = Pair(opCost, opName)
                    }
                }
            }
        }
        // get the trace
        var trace = ""
        var i = nWordsH
        var j = nWordsRef

        while (i > 0 || j > 0) {
            val op = dist[i][j].second
            trace = op + trace
            if (OP_SUB == op || OP_NOP == op) {
                i -= 1
                j -= 1
            } else if (op == OP_INS) {
                j -= 1
            } else if (op == OP_DEL) {
                i -= 1
            } else
                throw Exception("unknown operation $op")
        }
        return Triple(dist.last().last().first, dist.subList(cache.size, dist.size), trace)
    }

    /**
     * Add newly computed rows to cache.
     *
     * Since edit distance is only calculated on the hypothesis suffix that
     * was not in cache, the number of rows in `mat` may be shorter than
     * hypothesis length. In that case, we skip over these initial words.
     *
     * @param words_hyp: Hypothesis words.
     * @param mat: Edit distance matrix rows for each position.
     */
    private fun addCache(words_hyp: List<String>, mat: MutableList<MutableList<Pair<Int, Char>>>) {
        if (cacheSize >= MAX_CACHE_SIZE) return
        var node = cache
        val nMat = mat.size
        // how many initial words to skip
        val skipNum = words_hyp.size - nMat

        // jump through the cache to the current position
        (0 until skipNum).forEach { i ->
            node = node[words_hyp[i]]?.child ?: mutableMapOf()
        }
        // update cache with newly computed rows
        val words = words_hyp.subList(skipNum, words_hyp.size)
        words.indices.forEach { i ->
            val word = words[i]
            val row = mat[i]
            if (!node.containsKey(word)) {
                node[word] = Node(mutableMapOf(), row)
                cacheSize += 1
            }
            val value = node[word]
            node = value?.child ?: mutableMapOf()
        }
    }

    /**
     * Find the already computed rows of the edit distance matrix in cache.
     *
     * Returns a partially computed edit distance matrix.
     *
     * @param words_hyp: Translation hypothesis.
     * @return: Tuple (start position, dist).
     */
    private fun findCache(words_hyp: List<String>): Pair<Int, MutableList<MutableList<Pair<Int, Char>>>> {
        val node = cache
        var startPosition = 0
        var dist = mutableListOf(initialRow)
        for (word in words_hyp) {
            if (node.contains(word)) {
                startPosition += 1
                val nodeChild = node[word]
                if (nodeChild != null) {
                    dist.add(nodeChild.row)
                }
            } else break
        }
        return Pair(startPosition, dist)
    }
}

data class Node(
    val child: MutableMap<String, Node>,
    val row: MutableList<Pair<Int, Char>>
)

data class MakeTrans(val intab: String, val outab: String) {
    private val d: MutableMap<Char, Char>
    fun translate(src: String): String {
        val sb = StringBuilder(src.length)
        for (src_c in src.toCharArray()) sb.append(if (d.containsKey(src_c)) d[src_c] else src_c)
        return sb.toString()
    }

    init {
        d = HashMap()
        for (i in intab.indices) d[intab[i]] = outab[i]
    }
}

data class Ranking(
    val score: Int,  // highest score first
    val length: Int,  // then, longest match first
    val start: Int,  // then, earliest match first
    val idx: Int,   // then, earliest target position first
    val shiftedWords: List<String>
)