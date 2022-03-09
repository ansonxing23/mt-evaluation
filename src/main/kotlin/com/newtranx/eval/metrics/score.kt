package com.newtranx.eval.metrics

/**
 * @Author: anson
 * @Date: 2022/2/1 5:19 PM
 */
open class Score(open var score: Double)

data class BLEUScore(
    override var score: Double,
    var counts: List<Double>,
    var totals: List<Double>,
    var precisions: List<Double>,
    var bp: Float,
    var sys_len: Double,
    var ref_len: Double
) : Score(score)

data class TerScore(
    override var score: Double,
    var num_edits: Double,
    var ref_length: Double
) : Score(score)
