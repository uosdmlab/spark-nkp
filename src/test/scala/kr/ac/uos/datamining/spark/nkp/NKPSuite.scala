package kr.ac.uos.datamining.spark.nkp

import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.feature.{CountVectorizer, IDF, SQLTransformer}
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql.SparkSession
import org.scalatest.{BeforeAndAfterAll, FunSuite}

/**
  * Created by jun on 2016. 10. 16..
  */
class NKPSuite extends FunSuite with BeforeAndAfterAll {
  private var spark: SparkSession = _
  private var nkp: NKP = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    spark = SparkSession.builder()
      .master("local[2]")
      .appName("NKP Suite")
      .getOrCreate

    nkp = new NKP
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
  private val doubleId: Seq[Double] = intId.map(_.toDouble)
  private val stringId: Seq[String] = for (i <- intId) yield Identifiable.randomUID("sid")

  test("Default parameters") {
    assert(nkp.getIdCol == "id")
    assert(nkp.getTextCol == "text")
    assert(nkp.getWordCol == "word")
    assert(nkp.getPosCol == "pos")
    assert(nkp.getCharCol == "char")
    assert(nkp.getStartCol == "start")
    assert(nkp.getEndCol == "end")
  }

  test("Integer ID") {
    val df = spark.createDataFrame(
      intId zip sample
    ).toDF("id", "text")

    val result = nkp.transform(df)

    assert(result.select("id").distinct.count == sample.size)
  }

  test("Double ID") {
    val df = spark.createDataFrame(
      doubleId zip sample
    ).toDF("id", "text")

    val result = nkp.transform(df)

    assert(result.select("id").distinct.count == sample.size)
  }

  test("String ID") {
    val df = spark.createDataFrame(
      stringId zip sample
    ).toDF("id", "text")

    val result = nkp.transform(df)

    assert(result.select("id").distinct.count == sample.size)
  }

  test("Unidentifiable id column exception") {
    val df = spark.createDataFrame(
      (1 +: (1 until sample.size)) zip sample
    ).toDF("id", "text")

    intercept[IllegalArgumentException] {
      nkp.transform(df).collect
    }
  }

  test("There is no 'idCol' exception") {
    val df = spark.createDataFrame(
      intId zip sample
    ).toDF("not_a_id_column", "text")

    intercept[IllegalArgumentException] {
      nkp.transform(df).collect
    }
  }

  test("There is no 'textCol' exception") {
    val df = spark.createDataFrame(
      intId zip sample
    ).toDF("id", "not_a_text_column")

    intercept[IllegalArgumentException] {
      nkp.transform(df).collect
    }
  }

  test("Non-string text column exception") {
    val df = spark.createDataFrame(
      intId zip intId
    ).toDF("id", "text")

    intercept[IllegalArgumentException] {
      nkp.transform(df).collect
    }
  }

  test("TF-IDF pipeline") {
    val df = spark.createDataFrame(
      intId zip sample
    ).toDF("id", "text")

    val sql = new SQLTransformer()
      .setStatement(
        """
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

    assert(result.select("id").distinct.count == sample.size)
    assert(result.schema.fieldNames.intersect(Array("id", "words", "tf", "tfidf")).size == 4)

    result.show
  }
}
