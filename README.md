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

## API
### IEvaluate

#### sentenceScore(hypothesis: String, references: List<String>): Score
  
Evaluate hypothesis with multi references.
  
#### singleSentenceScore(hypothesis: String, reference: String): Score

Evaluate hypothesis with single reference.
  
#### corpusScore(hypotheses: List<String>, references: List<List<String>>): Score

Evaluate whole corpus

### MetricUtil.buildBleuMetric(language: String): IEvaluate

Build BLEU metric.

#### language

Type: `String`

Must be correct format of Locale string, including ISO code and language in English.

ex. `zh` `Chinese` `en` `English`

### MetricUtil.buildTerMetric(normalized: Boolean, noPunct: Boolean, asianSupport: Boolean, caseSensitive: Boolean): IEvaluate

Build TER metric.

#### normalized

Type: `Boolean`

If `True`, applies basic tokenization to sentences.

#### noPunct

Type: `Boolean`

If `True`, removes punctuations from sentences.

#### asianSupport

Type: `Boolean`

If `True`, adds support for Asian character processing.

#### caseSensitive

Type: `Boolean`

If `True`, does not lowercase sentences.

### MetricUtil.buildNistMetric(asianSupport: Boolean, nGram: Int): IEvaluate

Build NIST metric.

#### nGram

Type: `Int`

highest n-gram order

#### asianSupport

Type: `Boolean`

If `True`, adds support for Asian character processing.

### MetricUtil.buildMeteorMetric(wordnet: IDictionary, language: String, lowercase: Boolean, alpha: Float, beta: Int, gamma: Float): IEvaluate

Build Meteor metric.

#### wordnet

Type: `Boolean`

If `True`, applies basic tokenization to sentences.

#### language

Type: `String`

Must be correct format of Locale string, including ISO code and language in English.

ex. `zh` `Chinese` `en` `English`

#### lowercase

Type: `Boolean`

If `True`, lowercase the input sentence

#### alpha

Type: `Float`

parameter for controlling relative weights of precision and recall.

#### alpha

Type: `Float`

parameter for controlling relative weights of precision and recall.

#### beta

Type: `Int`

parameter for controlling shape of penalty as a function of as a function of fragmentation.

#### gamma

Type: `Float`

relative weight assigned to fragmentation penality.
