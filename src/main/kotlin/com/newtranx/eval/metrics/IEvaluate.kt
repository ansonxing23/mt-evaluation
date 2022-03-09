package com.newtranx.eval.metrics

/**
 * @Author: anson
 * @Date: 2022/2/1 4:06 PM
 */
interface IEvaluate {
    fun sentenceScore(hypothesis: String, references: List<String>): Score
    fun corpusScore(hypotheses: List<String>, references: List<List<String>>): Score
    fun singleCorpusScore(hypotheses: List<String>, reference: List<String>): Score
    fun singleSentenceScore(hypothesis: String, reference: String): Score
}