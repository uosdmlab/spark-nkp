package com.github.uosdmlab.nkp

import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.feature.{CountVectorizer, IDF, SQLTransformer}
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql.SparkSession
import org.scalatest.{BeforeAndAfterAll, FunSuite}

/**
  * Created by jun on 2016. 10. 16..
  */
class TokenizerSuite extends FunSuite with BeforeAndAfterAll {
  private var spark: SparkSession = _
  private var tokenizer: Tokenizer = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    spark = SparkSession.builder()
      .master("local[2]")
      .appName("Tokenizer Suite")
      .getOrCreate

    spark.sparkContext.setLogLevel("WARN")

    tokenizer = new Tokenizer()
      .setInputCol("text")
      .setOutputCol("words")
  }

  override protected def afterAll(): Unit = {
    try {
      spark.stop
    } finally {
      super.afterAll()
    }
  }

  private val sample = Seq(
    "아버지가방에들어가신다.",
    "사랑해요 제플린!",
    "스파크는 재밌어",
    "나는야 데이터과학자",
    "데이터야~ 놀자~"
  )

  private val intId: Seq[Int] = 1 to sample.size

  test("Default parameters") {
    assert(tokenizer.getPosFilter sameElements Array.empty[String])
  }

  test("Basic operation") {
    val df = spark.createDataFrame(
      intId zip sample
    ).toDF("id", "text")
      .drop("id")

    val words = tokenizer.transform(df)

    assert(df.count == words.count)
    assert(words.schema.fieldNames.contains(tokenizer.getOutputCol))
  }

  test("TF-IDF pipeline") {
    val df = spark.createDataFrame(
      intId zip sample
    ).toDF("id", "text")
      .drop("id")

    tokenizer.setPosFilter("N")

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

    assert(result.count == sample.size)
    assert(result.schema.fieldNames.intersect(Array(tokenizer.getOutputCol, cntVec.getOutputCol, idf.getOutputCol)).length == 3)

    result.show
  }
}
