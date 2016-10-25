package com.github.uosdmlab.nkp

import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.feature.{CountVectorizer, IDF}
import org.apache.spark.sql.SparkSession
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter, FunSuite}

/**
  * Created by jun on 2016. 10. 16..
  */
class TokenizerSuite extends FunSuite with BeforeAndAfterAll with BeforeAndAfter {

  private var tokenizer: Tokenizer = _

  private val spark: SparkSession =
    SparkSession.builder()
      .master("local[2]")
      .appName("Tokenizer Suite")
      .getOrCreate

  spark.sparkContext.setLogLevel("WARN")

  import spark.implicits._

  override protected def afterAll(): Unit = {
    try {
      spark.stop
    } finally {
      super.afterAll()
    }
  }

  before {
    tokenizer = new Tokenizer()
      .setInputCol("text")
      .setOutputCol("words")
  }

  private val df = spark.createDataset(
    Seq(
      "아버지가방에들어가신다.",
      "사랑해요 제플린!",
      "스파크는 재밌어",
      "나는야 데이터과학자",
      "데이터야~ 놀자~"
    )
  ).toDF("text")

  test("Default parameters") {
    assert(tokenizer.getFilter sameElements Array.empty[String])
  }

  test("Basic operation") {
    val words = tokenizer.transform(df)

    assert(df.count == words.count)
    assert(words.schema.fieldNames.contains(tokenizer.getOutputCol))
  }

  test("POS filter") {
    val nvTokenizer = new Tokenizer()
      .setInputCol("text")
      .setOutputCol("nvWords")
      .setFilter("N", "V")

    val words = tokenizer.transform(df).join(nvTokenizer.transform(df), "text")

    assert(df.count == words.count)
    assert(words.schema.fieldNames.contains(nvTokenizer.getOutputCol))
    assert(words.where(s"SIZE(${tokenizer.getOutputCol}) < SIZE(${nvTokenizer.getOutputCol})").count == 0)
  }

  test("TF-IDF pipeline") {
    tokenizer.setFilter("N")

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

    assert(result.count == df.count)

    val fields = result.schema.fieldNames
    assert(fields.contains(tokenizer.getOutputCol))
    assert(fields.contains(cntVec.getOutputCol))
    assert(fields.contains(idf.getOutputCol))

    result.show
  }
}
