# Natural Korean Processor for Apache Spark [![Build Status](https://travis-ci.org/uosdmlab/spark-nkp.svg?branch=master)](https://travis-ci.org/uosdmlab/spark-nkp)
> For English, please go to [README.eng.md](README.eng.md)

[은전한닢 프로젝트](http://eunjeon.blogspot.kr/)의 형태소 분석기 [seunjeon](https://bitbucket.org/eunjeon/seunjeon)을 [Apache Spark](http://spark.apache.org/)에서 사용하기 쉽게 포장한 패키지입니다.

## 사용법

### spark-shell
```bash
spark-shell --packages com.github.uosdmlab:spark-nkp_2.11:0.1.0
```

### Zeppelin
Interpreter Setting > Spark Interpreter > Edit > Dependencies

**artifact** `com.github.uosdmlab:spark-nkp_2.11:0.1.0`

## 예제
### 기본 사용법

```scala
import org.apache.spark.sql.functions._
import com.github.uosdmlab.nkp.NKP

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

df.show

val nkp = new NKP

val result = nkp.transform(df)

result.show(10)
```

*output:*
```bash
+------------+---+
|        text| id|
+------------+---+
|아버지가방에들어가신다.|  0|
|   사랑해요 제플린!|  1|
|    스파크는 재밌어|  2|
|  나는야 데이터과학자|  3|
|   데이터야~ 놀자~|  4|
+------------+---+

+---+----+-------+--------------------+-----+---+
| id|word|    pos|                char|start|end|
+---+----+-------+--------------------+-----+---+
|  0| 아버지|    [N]|[NNG, *, F, 아버지, ...|    0|  3|
|  0|   가|    [J]|[JKS, *, F, 가, *,...|    3|  4|
|  0|   방|    [N]|[NNG, *, T, 방, *,...|    4|  5|
|  0|   에|    [J]|[JKB, *, F, 에, *,...|    5|  6|
|  0| 들어가|    [V]|[VV, *, F, 들어가, *...|    6|  9|
|  0|  신다|[EP, E]|[EP+EF, *, F, 신다,...|    9| 11|
|  0|   .|    [S]|[SF, *, *, *, *, ...|   11| 12|
|  1|  사랑|    [N]|[NNG, *, T, 사랑, *...|    0|  2|
|  1|  해요|[XS, E]|[XSV+EF, *, F, 해요...|    2|  4|
|  1| 제플린|    [N]|[NNP, *, T, 제플린, ...|    5|  8|
+---+----+-------+--------------------+-----+---+
only showing top 10 rows
```

### 명사 단어 TF-IDF with Pipeline
```scala
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.feature.{CountVectorizer, IDF, SQLTransformer}
import org.apache.spark.sql.functions._
import com.github.uosdmlab.nkp.NKP

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

val nkp = new NKP

val sql = new SQLTransformer()
	.setStatement("""
	    |SELECT id, COLLECT_LIST(word) AS words
	    |FROM __THIS__
	    |WHERE ARRAY_CONTAINS(pos, 'N')
	    |GROUP BY id
	    |""".stripMargin)

val cntVec = new CountVectorizer()
	.setInputCol("words")
	.setOutputCol("tf")

val idf = new IDF()
  .setInputCol("tf")
  .setOutputCol("tfidf")

val pipe = new Pipeline()
  .setStages(Array(nkp, sql, cntVec, idf))

val pipeModel = pipe.fit(df)

val result = pipeModel.transform(df)

result.show
```

*output:*
```bash
+---+-------------+--------------------+--------------------+
| id|        words|                  tf|               tfidf|
+---+-------------+--------------------+--------------------+
|  0|     [아버지, 방]| (9,[1,4],[1.0,1.0])|(9,[1,4],[1.09861...|
|  1|    [사랑, 제플린]| (9,[3,8],[1.0,1.0])|(9,[3,8],[1.09861...|
|  3|[나, 데이터, 과학자]|(9,[0,2,7],[1.0,1...|(9,[0,2,7],[0.693...|
|  2|        [스파크]|       (9,[6],[1.0])|(9,[6],[1.0986122...|
|  4|    [데이터, 놀자]| (9,[0,5],[1.0,1.0])|(9,[0,5],[0.69314...|
+---+-------------+--------------------+--------------------+
```
## DataFrame Schema 설명
### Input Schema
Input DataFrame은 다음과 같은 column을 가져야 합니다. `id` column의 값들이 고유한(unique) 값이 아닐 경우 오류가 발생합니다. Unique ID는 기본 사용법 예제와 같이 Spark의 SQL 함수 `monotonically_increasing_id`를 사용하면 쉽게 생성할 수 있습니다.

| 이름 | 설명                       |
|------|----------------------------|
| id   | 각 text를 구분할 unique ID |
| text | 분석할 텍스트              |

### Output Schema
| 이름  | 설명                                     |
|-------|------------------------------------------|
| id    | 각 text를 구분할 unique ID               |
| word  | 단어                                     |
| pos   | Part Of Speech; 품사                     |
| char  | characteristic; 특징, seunjeon의 feature |
| start | 단어 시작 위치                           |
| end   | 단어 종료 위치                           |

품사 태그는 seunjeon의 [품사 태그 설명](https://docs.google.com/spreadsheets/d/1-9blXKjtjeKZqsf4NzHeYJCrr49-nXeRF6D80udfcwY/edit#gid=589544265)을 참고하시기 바랍니다.

## API
### Core
다음 함수를 통해 텍스트를 단어로 쪼갤 수 있습니다.
- transform(dataset: Dataset[_]): DataFrame

### Setter
다음 함수들을 통해 column 이름을 설정할 수 있습니다.
- setIdCol(value: String)
- setTextCol(value: String)
- setWordCol(value: String)
- setPosCol(value: String)
- setCharCol(value: String)
- setStartCol(value: String)
- setEndCol(value: String)

### Getter
- getIdCol(value: String)
- getTextCol(value: String)
- getWordCol(value: String)
- getPosCol(value: String)
- getCharCol(value: String)
- getStartCol(value: String)
- getEndCol(value: String)

## Test
```bash
sbt test
```

## 알림
본 패키지는 Spark 2.0 버전을 기준으로 만들어졌습니다.

## 감사의 글
은전한닢 프로젝트의 유영호님, 이용운님께 감사의 말씀 드립니다! 연구에 정말 큰 도움이 되었습니다.
