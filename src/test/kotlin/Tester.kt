import com.newtranx.eval.metrics.MetricUtil
import com.newtranx.eval.metrics.enums.Language
import com.newtranx.eval.metrics.nltk.Meteor
import org.junit.Test

/**
 * @Author: anson
 * @Date: 2022/2/18 11:16 PM
 */
class Tester {

    companion object {
        val hypothesis = listOf("我是中国人", "我爱吃水果")
        private val ref1 = listOf("我是中国人", "我爱水果")
        private val ref2 = listOf("中国是我", "我爱吃水果")
        val language = Language.ZH
        val references = listOf(ref1, ref2)
        val path = Meteor::class.java.getResource("/wordnet").path
        val wordnet = MetricUtil.buildWordnet(path)

        val bleu = MetricUtil.buildBleuMetric(language)
        val ter = MetricUtil.buildTerMetric(normalized = true, asianSupport = true)
        val nist = MetricUtil.buildNistMetric(asianSupport = true)
        val meteor = MetricUtil.buildMeteorMetric(wordnet, language)

        val hyp = "我爱吃水果"
        val refs = listOf("我爱吃水果", "我爱吃果果")
    }

    @Test
    fun testCorpusBleu() {
        val score = bleu.corpusScore(hypothesis, references)
        println(score)
    }

    @Test
    fun testSentBleu() {
        val score = bleu.sentenceScore(hyp, refs)
        println(score)
    }

    @Test
    fun testCorpusTer() {
        val score = ter.corpusScore(hypothesis, references)
        println(score)
    }

    @Test
    fun testSentTer() {
        val score = ter.sentenceScore(hyp, refs)
        println(score)
    }

    @Test
    fun testCorpusNist() {
        val score = nist.corpusScore(hypothesis, references)
//        val score = nist.corpusScore(listOf("It is a guide to action"), listOf(listOf("It is a guide to action"), listOf("It is the guiding principle which")))
        println(score)
    }

    @Test
    fun testSentNist() {
        val score = nist.sentenceScore(hyp, refs)
        println(score)
    }

    @Test
    fun testCorpusMeteor() {
        val score = meteor.corpusScore(hypothesis, references)
        println(score)
    }

    @Test
    fun testSentMeteor() {
        val score = meteor.sentenceScore(hyp, refs)
        println(score)
    }

    @Test
    fun testMeteorSingle() {
        val meteor = Meteor(wordnet = wordnet)
        val score = meteor.singleMeteorScore(
            "It is a guide to action which ensures that the military always obeys the commands of the party",
            "It is a guide to action which ensures that the military always obeys the commands of the party"
        )
        println(score)
    }
}