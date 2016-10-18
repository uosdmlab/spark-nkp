# Natural Korean Processor for Apache Spark
[![Build Status](https://travis-ci.org/uosdmlab/spark-nkp.svg?branch=master)](https://travis-ci.org/uosdmlab/spark-nkp)
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

### 예제
#### 기본 사용법
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

#### 명사 단어 TF-IDF with Pipeline
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
