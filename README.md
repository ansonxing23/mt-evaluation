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

## Usage
### Corpus Level
BLEU
```
val hypothesis = listOf("how are you?", "I'm fine!")
val ref1 = listOf("how are you?", "I'm fine!")
val ref2 = listOf("how do you do?", "I'm ok!")
val references = listOf(ref1, ref2)
val bleu = MetricUtil.buildBleuMetric("en")
val score = bleu.corpusScore(hypothesis, references)
```

TER
```
val hypothesis = listOf("how are you?", "I'm fine!")
val ref1 = listOf("how are you?", "I'm fine!")
val ref2 = listOf("how do you do?", "I'm ok!")
val references = listOf(ref1, ref2)
val ter = MetricUtil.buildTerMetric(normalized = true, asianSupport = true)
val score = ter.corpusScore(hypothesis, references)
```

METEOR
```
val hypothesis = listOf("how are you?", "I'm fine!")
val ref1 = listOf("how are you?", "I'm fine!")
val ref2 = listOf("how do you do?", "I'm ok!")
val references = listOf(ref1, ref2)

val path = "/home/wordnet"
val wordnet = MetricUtil.buildWordnet(path)
val meteor = MetricUtil.buildMeteorMetric(wordnet, "en")
val score = meteor.corpusScore(hypothesis, references)
```


NIST
```
val hypothesis = listOf("how are you?", "I'm fine!")
val ref1 = listOf("how are you?", "I'm fine!")
val ref2 = listOf("how do you do?", "I'm ok!")
val references = listOf(ref1, ref2)
val nist = MetricUtil.buildNistMetric(asianSupport = true)
val score = nist.corpusScore(hypothesis, references)
```

### Sentence Level
BLEU
```
val hypothesis = "how are you?"
val references = listOf("how are you?", "how do you do?")
val bleu = MetricUtil.buildBleuMetric("en")
val score = bleu.sentenceScore(hypothesis, references)
```

TER
```
val hypothesis = "how are you?"
val references = listOf("how are you?", "how do you do?")
val ter = MetricUtil.buildTerMetric(normalized = true, asianSupport = true)
val score = ter.sentenceScore(hypothesis, references)
```

METEOR
```
val hypothesis = "how are you?"
val references = listOf("how are you?", "how do you do?")
val language = Language.EN

val path = "/home/wordnet"
val wordnet = MetricUtil.buildWordnet(path)
val meteor = MetricUtil.buildMeteorMetric(wordnet, "en")
val score = meteor.sentenceScore(hypothesis, references)
```


NIST
```
val hypothesis = "how are you?"
val references = listOf("how are you?", "how do you do?")
val nist = MetricUtil.buildNistMetric(asianSupport = true)
val score = nist.sentenceScore(hypothesis, references)
```
