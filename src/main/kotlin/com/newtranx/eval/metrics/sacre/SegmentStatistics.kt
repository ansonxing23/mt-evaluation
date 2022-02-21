package com.newtranx.eval.metrics.sacre

import com.newtranx.eval.utils.Counter

/**
 * @Author: anson
 * @Date: 2022/2/1 4:10 PM
 */
data class SegmentStatistics(
    val refNgrams: Counter<List<String>> = Counter(),
    val refLens: List<Double> = emptyList(),
    val refWords: List<List<String>> = emptyList()
)
