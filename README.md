# Natural Korean Processor for Apache Spark [![Build Status](https://travis-ci.org/uosdmlab/spark-nkp.svg?branch=master)](https://travis-ci.org/uosdmlab/spark-nkp) [![Maven Central](https://img.shields.io/maven-central/v/com.github.uosdmlab/spark-nkp_2.11.svg)](http://search.maven.org/#search|ga|1|spark-nkp)
> For English, please go to [README.eng.md](README.eng.md)

[은전한닢 프로젝트](http://eunjeon.blogspot.kr/)의 형태소 분석기 [seunjeon](https://bitbucket.org/eunjeon/seunjeon)을 [Apache Spark](http://spark.apache.org/)에서 사용하기 쉽게 포장한 패키지입니다. `spark-nkp`는 다음과 같은 두 가지 [Transformer](http://spark.apache.org/docs/latest/ml-pipeline.html#transformers)를 제공합니다:
* `Tokenizer` 문장을 형태소 단위로 쪼개는 *transformer*. 원하는 품사만을 걸러낼 수도 있습니다.
* `Analyzer` 형태소 분석을 위한 *transformer*. 문장의 단어들에 대한 자세한 정보를 담은 *DataFrame*을 출력합니다.

또한, 사용자 정의 사전을 지원하기 위한 `Dictionary`를 제공합니다.


## 사용법

### spark-shell
```bash
spark-shell --packages com.github.uosdmlab:spark-nkp_2.11:0.3.1
```

### Zeppelin
두 가지 방법으로 사용 가능합니다:
* Interpreter Setting
* Dynamic Dependency Loading (`%spark.dep`)

#### Interpreter Setting
Interpreter Setting > Spark Interpreter > Edit > Dependencies

**artifact** `com.github.uosdmlab:spark-nkp_2.11:0.3.1`

#### Dynamic Dependency Loading (`%spark.dep`)
```scala
%spark.dep
z.load("com.github.uosdmlab:spark-nkp_2.11:0.3.1")
```

## 예제
### Tokenizer
```scala
import com.github.uosdmlab.nkp.Tokenizer

val df = spark.createDataset(
	Seq(
		"아버지가방에들어가신다.",
		"사랑해요 제플린!",
		"스파크는 재밌어",
		"나는야 데이터과학자",
		"데이터야~ 놀자~"
	)
).toDF("text")

val tokenizer = new Tokenizer()
	.setInputCol("text")
	.setOutputCol("words")

val result = tokenizer.transform(df)

result.show(truncate = false)
```

*output:*
```bash
+------------+--------------------------+
|text        |words                     |
+------------+--------------------------+
|아버지가방에들어가신다.|[아버지, 가, 방, 에, 들어가, 신다, .]|
|사랑해요 제플린!   |[사랑, 해요, 제플린, !]          |
|스파크는 재밌어    |[스파크, 는, 재밌, 어]           |
|나는야 데이터과학자  |[나, 는, 야, 데이터, 과학자]       |
|데이터야~ 놀자~   |[데이터, 야, ~, 놀자, ~]        |
+------------+--------------------------+
```
### Analyzer

```scala
import org.apache.spark.sql.functions._
import com.github.uosdmlab.nkp.Analyzer

val df = spark.createDataset(
	Seq(
		"아버지가방에들어가신다.",
		"사랑해요 제플린!",
		"스파크는 재밌어",
		"나는야 데이터과학자",
		"데이터야~ 놀자~"
	)
).toDF("text")
	.withColumn("id", monotonically_increasing_id)

val analyzer = new Analyzer

val result = analyzer.transform(df)

result.show(truncate = false)
```

*output:*
```bash
+---+----+-------+-----------------------------------------------------+-----+---+
|id |word|pos    |feature                                              |start|end|
+---+----+-------+-----------------------------------------------------+-----+---+
|0  |아버지 |[N]    |[NNG, *, F, 아버지, *, *, *, *]                         |0    |3  |
|0  |가   |[J]    |[JKS, *, F, 가, *, *, *, *]                           |3    |4  |
|0  |방   |[N]    |[NNG, *, T, 방, *, *, *, *]                           |4    |5  |
|0  |에   |[J]    |[JKB, *, F, 에, *, *, *, *]                           |5    |6  |
|0  |들어가 |[V]    |[VV, *, F, 들어가, *, *, *, *]                          |6    |9  |
|0  |신다  |[EP, E]|[EP+EF, *, F, 신다, Inflect, EP, EF, 시/EP/*+ᆫ다/EF/*]   |9    |11 |
|0  |.   |[S]    |[SF, *, *, *, *, *, *, *]                            |11   |12 |
|1  |사랑  |[N]    |[NNG, *, T, 사랑, *, *, *, *]                          |0    |2  |
|1  |해요  |[XS, E]|[XSV+EF, *, F, 해요, Inflect, XSV, EF, 하/XSV/*+아요/EF/*]|2    |4  |
|1  |제플린 |[N]    |[NNP, *, T, 제플린, *, *, *, *]                         |5    |8  |
|1  |!   |[S]    |[SF, *, *, *, *, *, *, *]                            |8    |9  |
|2  |스파크 |[N]    |[NNG, *, F, 스파크, *, *, *, *]                         |0    |3  |
|2  |는   |[J]    |[JX, *, T, 는, *, *, *, *]                            |3    |4  |
|2  |재밌  |[V]    |[VA, *, T, 재밌, *, *, *, *]                           |5    |7  |
|2  |어   |[E]    |[EC, *, F, 어, *, *, *, *]                            |7    |8  |
|3  |나   |[N]    |[NP, *, F, 나, *, *, *, *]                            |0    |1  |
|3  |는   |[J]    |[JX, *, T, 는, *, *, *, *]                            |1    |2  |
|3  |야   |[I]    |[IC, *, F, 야, *, *, *, *]                            |2    |3  |
|3  |데이터 |[N]    |[NNG, *, F, 데이터, *, *, *, *]                         |4    |7  |
|3  |과학자 |[N]    |[NNG, *, F, 과학자, Compound, *, *, 과학/NNG/*+자/NNG/*]   |7    |10 |
+---+----+-------+-----------------------------------------------------+-----+---+
only showing top 20 rows
```

### Dictionary
```scala
import com.github.uosdmlab.nkp.{Tokenizer, Dictionary}

val df = spark.createDataset(
	Seq("넌 눈치도 없니? 낄끼빠빠!")
).toDF("text")

val tokenizer = new Tokenizer()
	.setInputCol("text")
	.setOutputCol("words")

Dictionary.addWords("낄끼+빠빠,-100")

val result = tokenizer.transform(df)

result.show(truncate = false)
```

*output:*
```bash
+---------------+----------------------------+
|text           |words                       |
+---------------+----------------------------+
|넌 눈치도 없니? 낄끼빠빠!|[넌, 눈치, 도, 없, 니, ?, 낄끼빠빠, !]|
+---------------+----------------------------+
```

### 명사 단어 TF-IDF with Pipeline
```scala
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.feature.{CountVectorizer, IDF}
import com.github.uosdmlab.nkp.Tokenizer

val df = spark.createDataset(
	Seq(
		"아버지가방에들어가신다.",
		"사랑해요 제플린!",
		"스파크는 재밌어",
		"나는야 데이터과학자",
		"데이터야~ 놀자~"
	)
).toDF("text")

val tokenizer = new Tokenizer()
	.setInputCol("text")
	.setOutputCol("words")
	.setFilter("N")

val cntVec = new CountVectorizer()
  .setInputCol("words")
  .setOutputCol("tf")

val idf = new IDF()
  .setInputCol("tf")
  .setOutputCol("tfidf")

val pipe = new Pipeline()
  .setStages(Array(tokenizer, cntVec, idf))

val pipeModel = pipe.fit(df)

val result = pipeModel.transform(df)

result.show
```

*output:*
```bash
+------------+-------------+--------------------+--------------------+
|        text|        words|                  tf|               tfidf|
+------------+-------------+--------------------+--------------------+
|아버지가방에들어가신다.|     [아버지, 방]| (9,[1,5],[1.0,1.0])|(9,[1,5],[1.09861...|
|   사랑해요 제플린!|    [사랑, 제플린]| (9,[3,8],[1.0,1.0])|(9,[3,8],[1.09861...|
|    스파크는 재밌어|        [스파크]|       (9,[6],[1.0])|(9,[6],[1.0986122...|
|  나는야 데이터과학자|[나, 데이터, 과학자]|(9,[0,2,7],[1.0,1...|(9,[0,2,7],[0.693...|
|   데이터야~ 놀자~|    [데이터, 놀자]| (9,[0,4],[1.0,1.0])|(9,[0,4],[0.69314...|
+------------+-------------+--------------------+--------------------+
```

## API
### Tokenizer
문장을 형태소 단위로 쪼개는 *transformer* 입니다. `setFilter` 함수로 원하는 품사에 해당하는 형태소만을 걸러낼 수도 있습니다. 품사 태그는 아래의 **품사 태그 설명**을 참고하세요.

#### Example
```scala
import com.github.uosdmlab.nkp.Tokenizer

val tokenizer = new Tokenizer()
	.setInputCol("text")
	.setOutputCol("words")
	.setFilter("N", "V", "SN")	// 체언, 용언, 숫자만을 출력
```

#### 품사 태그 설명
* `EP` 선어말어미
* `E` 어미
* `I` 독립언
* `J` 관계언
* `M` 수식언
* `N` 체언 (명사가 여기 속합니다)
* `S` 부호
* `SL` 외국어
* `SH` 한자
* `SN` 숫자
* `V` 용언 (동사가 여기 속합니다)
* `VCP` 긍정지정사
* `XP` 접두사
* `XS` 접미사
* `XR` 어근

#### Members
* `transform(dataset: Dataset[_]): DataFrame`

#### Parameter Setters
* `setFilter(pos: String, poses: String*): Tokenizer`
* `setInputCol(value: String): Tokenizer`
* `setOutputCol(value: String): Tokenizer`

#### Parameter Getters
* `getFilter: Array[String]`
* `getInputCol: String`
* `getOutputCol: String`

### Analyzer
형태소 분석을 위한 *transformer* 입니다. 분석할 문장들과 각 문장들을 구분할 `id`를 입력값으로 받습니다.

#### Example
```scala
import com.github.uosdmlab.nkp.Analyzer

val analyzer = new Analyzer
```

#### Analyzer DataFrame Schema
##### Input Schema
Input DataFrame은 다음과 같은 column들을 가져야 합니다. `id` column의 값들이 고유한(unique) 값이 아닐 경우 오류가 발생합니다. Unique ID는 상단의 Analyzer 예제와 같이 Spark의 SQL 함수 `monotonically_increasing_id`를 사용하면 쉽게 생성할 수 있습니다.

| 이름 | 설명                       |
|------|----------------------------|
| id   | 각 text를 구분할 unique ID |
| text | 분석할 텍스트              |

##### Output Schema
| 이름  | 설명                                     |
|-------|------------------------------------------|
| id    | 각 text를 구분할 unique ID               |
| word  | 단어                                     |
| pos   | Part Of Speech; 품사                     |
| char  | characteristic; 특징, seunjeon의 feature |
| start | 단어 시작 위치                           |
| end   | 단어 종료 위치                           |

자세한 품사 태그 설명은 seunjeon의 [품사 태그 설명 스프레드 시트](https://docs.google.com/spreadsheets/d/1-9blXKjtjeKZqsf4NzHeYJCrr49-nXeRF6D80udfcwY/edit#gid=589544265)를 참고하시기 바랍니다.

#### Members
* `transform(dataset: Dataset[_]): DataFrame`

#### Parameter Setters
* `setIdCol(value: String)`
* `setTextCol(value: String)`
* `setWordCol(value: String)`
* `setPosCol(value: String)`
* `setCharCol(value: String)`
* `setStartCol(value: String)`
* `setEndCol(value: String)`

#### Parameter Getters
* `getIdCol(value: String)`
* `getTextCol(value: String)`
* `getWordCol(value: String)`
* `getPosCol(value: String)`
* `getCharCol(value: String)`
* `getStartCol(value: String)`
* `getEndCol(value: String)`

### Dictionary
사용자 정의 사전을 관리하기 위한 `object` 입니다. `Dictionary`에 추가된 단어들은 `Tokenizer`와
`Analyzer` 모두에게 적용됩니다. 사용자 정의 단어는 `addWords` 혹은 `addWordsFromCSV` 함수를 통해
추가할 수 있습니다.

#### Example
```scala
import com.github.uosdmlab.nkp.Dictionary

Dictionary
  .addWords("덕후", "낄끼+빠빠,-100")
  .addWords(Seq("버카충,-100", "C\\+\\+"))
  .addWordsFromCSV("path/to/CSV1", "path/to/CSV2")
  .addWordsFromCSV("path/to/*.csv")

Dictionary.reset()  // 사용자 정의 사전 초기화
```

#### Members
* `def addWords(word: String, words: String*): Dictionary`
* `def addWords(words: Traversable[String]): Dictionary`
* `def addWordsFromCSV(path: String, paths: String*): Dictionary`
* `def addWordsFromCSV(paths: Traversable[String]): Dictionary`
* `def reset(): Dictionary`

#### CSV Example
`addWordsFromCSV`를 통해 전달되는 CSV 파일은 header는 없어야하고 `word`, `cost` 두 개의 컬럼을
가져야합니다. `cost`는 단어 출연 비용으로 작을수록 출연할 확률이 높음을 뜻합니다. `cost`는 생략 가능합니다. CSV 파일은 `spark.read.csv`를 사용하여 불러오기 때문에 HDFS에 존재하는 파일 또한 사용 가능합니다.
아래는 CSV 파일의 예입니다:

```
덕후
낄끼+빠빠,-100
버카충,-100
C\+\+
```
`+`로 복합 명사를 등록할 수도 있습니다. `+` 문자 자체를 사전에 등록하기 위해서는 `\+`를 사용하세요.

## Test
```bash
sbt test
```

## 알림
본 패키지는 Spark 2.0 버전을 기준으로 만들어졌습니다.

## 감사의 글
은전한닢 프로젝트의 유영호님, 이용운님께 감사의 말씀 드립니다! 연구에 정말 큰 도움이 되었습니다.
