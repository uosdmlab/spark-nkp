# Natural Korean Processor for Apache Spark
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

val df = spark.createDataFrame(
	Seq(
		("아버지가방에들어가신다.", 0),
		("사랑해요 제플린!", 0),
		("스파크는 재밌어", 0),
		("나는야 데이터과학자", 0),
		("데이터야~ 놀자~", 0)
	)
).toDF("text", "just_a_number_to_create_df")
	.withColumn("id", monotonically_increasing_id)

df.show

val nkp = new NKP

val result = nkp.transform(df)

result.show
```

#### 명사 단어 TF-IDF with Pipeline

```scala
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.feature.{CountVectorizer, IDF, SQLTransformer}
import org.apache.spark.sql.functions._
import com.github.uosdmlab.nkp.NKP

val df = spark.createDataFrame(
	Seq(
		("아버지가방에들어가신다.", 0),
		("사랑해요 제플린!", 0),
		("스파크는 재밌어", 0),
		("나는야 데이터과학자", 0),
		("데이터야~ 놀자~", 0)
	)
).toDF("text", "just_a_number_to_create_df")
	.withColumn("id", monotonically_increasing_id)

df.show

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
