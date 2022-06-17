package com.newtranx.eval

import com.newtranx.eval.metrics.MetricUtil
import com.newtranx.eval.metrics.nltk.Meteor
import com.newtranx.eval.utils.LanguageUtil
import java.io.File
import java.math.RoundingMode
import java.text.DecimalFormat


/**
 * @Author: anson
 * @Date: 2022/2/21 6:01 PM
 */
fun main(args: Array<String>) {
    val lang = args[0]
    val refPath = args[1]
    val hypoPath = args[2]
    val disLang = LanguageUtil.displayLanguage(lang)
    val asianSupport = LanguageUtil.isAsian(disLang)
    val rootPath = System.getProperty("user.dir")
    val wordnet = MetricUtil.buildWordnet(rootPath + File.separator + "wordnet")
    val bleu = MetricUtil.buildBleuMetric(lang)
    val ter = MetricUtil.buildTerMetric(normalized = true, asianSupport = asianSupport)
    val nist = MetricUtil.buildNistMetric(asianSupport = asianSupport)
    val meteor = MetricUtil.buildMeteorMetric(wordnet, lang)

    val refs = File(refPath).readLines()
    val hypos = File(hypoPath).readLines()

    assert(refs.size == hypos.size)
    fun formatScore(score: Double): Float {
        val format = DecimalFormat("0.##")
        format.roundingMode = RoundingMode.FLOOR
        return format.format(score).toFloat()
    }
    val bleuScore = bleu.singleCorpusScore(hypos, refs)
    val terScore = ter.singleCorpusScore(hypos, refs)
    val nistScore = nist.singleCorpusScore(hypos, refs)
    val meteorScore = meteor.singleCorpusScore(hypos, refs)
    val size = refs.size
    val output = File("evaluate_result_${System.currentTimeMillis()}.csv")
    output.bufferedWriter().use { writer ->
        var line = "序号,样本译文,译文,Bleu: ${formatScore(bleuScore.score)},Ter: ${formatScore(terScore.score)},Nist: ${formatScore(nistScore.score)},Meteor: ${formatScore(meteorScore.score)},\n"
        writer.write(line)
        (0 until size).forEach {
            val ref = refs[it]
            val hypo = hypos[it]
            val bs = bleu.singleSentenceScore(hypo, ref)
            val ts = ter.singleSentenceScore(hypo, ref)
            val ns = nist.singleSentenceScore(hypo, ref)
            val ms = meteor.singleSentenceScore(hypo, ref)
            line = "${it + 1},\"${ref.replace("\"", "\"\"")}\",\"${hypo.replace("\"", "\"\"")}\",${formatScore(bs.score)},${formatScore(ts.score)},${formatScore(ns.score)},${formatScore(ms.score)},\n"
            writer.write(line)
            val process = (it + 1).div(size.toFloat()) * 100
            println("progress: $process%")
        }
        println("Completed!")
    }
}
