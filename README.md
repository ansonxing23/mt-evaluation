mt-metrics
=========
Implement four evaluation methods for machine translation. Most open source evaluation programs writed by Python, therefore, I rewrite the methods with Kotlin.

* BLEU (sacrebleu)
* TER (sacrebleu)
* NIST (nltk)
* METEOR (nltk)

## Support languages
* EN("en", "English")
* FR("fr", "French")
* DE("de", "German")
* ES("es", "Spanish")
* ZH("zh", "Chinese")
* JA("ja", "Japanese")
* KO("ko", "Korean")

## Usage
BLEU
```
val hypothesis = listOf("how are you?", "I'm fine!")
val ref1 = listOf("how are you?", "I'm fine!")
val ref2 = listOf("how do you do?", "I'm ok!")
val language = Language.EN
val references = listOf(ref1, ref2)
val bleu = MetricUtil.buildBleuMetric(language)
val score = bleu.corpusScore(hypothesis, references)
```

TER
```
val hypothesis = listOf("how are you?", "I'm fine!")
val ref1 = listOf("how are you?", "I'm fine!")
val ref2 = listOf("how do you do?", "I'm ok!")
val language = Language.EN
val references = listOf(ref1, ref2)
val ter = MetricUtil.buildTerMetric(normalized = true, asianSupport = true)
val score = ter.corpusScore(hypothesis, references)
```

METEOR
```
val hypothesis = listOf("how are you?", "I'm fine!")
val ref1 = listOf("how are you?", "I'm fine!")
val ref2 = listOf("how do you do?", "I'm ok!")
val language = Language.EN
val references = listOf(ref1, ref2)

val path = "/home/wordnet"
val wordnet = MetricUtil.buildWordnet(path)
val meteor = MetricUtil.buildMeteorMetric(wordnet, language)
val score = meteor.corpusScore(hypothesis, references)
```


NIST
```
val hypothesis = listOf("how are you?", "I'm fine!")
val ref1 = listOf("how are you?", "I'm fine!")
val ref2 = listOf("how do you do?", "I'm ok!")
val language = Language.EN
val references = listOf(ref1, ref2)
val nist = MetricUtil.buildNistMetric(asianSupport = true)
val score = nist.corpusScore(hypothesis, references)
```
