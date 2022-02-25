import com.newtranx.eval.metrics.MetricUtil
import com.newtranx.eval.metrics.nltk.Meteor
import org.junit.Test
import java.io.File

/**
 * @Author: anson
 * @Date: 2022/2/18 11:16 PM
 */
class Tester {

    companion object {
        val hypothesis = listOf("我是中国人", "我爱吃水果")
        private val ref1 = listOf("我是中国人", "我爱水果")
        private val ref2 = listOf("中国是我", "我爱吃水果")
        val references = listOf(ref1, ref2)
        val path = Meteor::class.java.getResource("/wordnet").path
        val wordnet = MetricUtil.buildWordnet(path)

        val bleu = MetricUtil.buildBleuMetric("en")
        val ter = MetricUtil.buildTerMetric(normalized = true, asianSupport = false)
        val nist = MetricUtil.buildNistMetric(asianSupport = true)
        val meteor = MetricUtil.buildMeteorMetric(wordnet, "en")

        val hyp = "1-2899：CITIC Securities Investment Limited is a wholly-owned subsidiary of the Company with a registered capital of RMB3 billion."
        val refs = listOf("1-2899: CITIC Securities Investment Co., Ltd., with registered capital of RMB 3 billion Yuan, is a wholly-owned subsidiary of the Company.")
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

    @Test
    fun testFile() {
        val src = File("E:\\机翻评测系统\\3501-en.txt")
        val ref = File("E:\\机翻评测系统\\3501-new.txt")
        val hyps = src.readLines()
        val refs = ref.readLines()
        val score = meteor.corpusScore(hyps, listOf(refs))
        println(score)
    }
}