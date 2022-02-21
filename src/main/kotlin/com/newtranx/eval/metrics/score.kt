package com.newtranx.eval.metrics

/**
 * @Author: anson
 * @Date: 2022/2/1 5:19 PM
 */
abstract class Score()

data class EvaScore(
    var score: Float
) : Score()

data class BLEUScore(
    var score: Float,
    var counts: List<Double>,
    var totals: List<Double>,
    var precisions: List<Double>,
    var bp: Float,
    var sys_len: Double,
    var ref_len: Double
) : Score()


data class TerScore(
    var score: Float,
    var num_edits: Float,
    var ref_length: Float
) : Score()
