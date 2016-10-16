package kr.ac.uos.datamining.spark.nkp

import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql.SparkSession
import org.scalatest.{BeforeAndAfterAll, FunSuite}

/**
  * Created by jun on 2016. 10. 16..
  */
class NKPSuite extends FunSuite with BeforeAndAfterAll {
  private var spark: SparkSession = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    spark = SparkSession.builder()
      .master("local[*]")
      .appName("NKP Suite")
      .getOrCreate
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

  private val intId = sample.indices
  private val doubleId = intId.map(_.toDouble)
  private val stringId = for (i <- intId) yield Identifiable.randomUID("sid")

  private val nkp = new NKP

  test("Defualt parameters") {
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
}
