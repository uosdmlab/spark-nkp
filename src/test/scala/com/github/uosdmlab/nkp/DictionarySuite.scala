/**
  * Referenced test codes of https://bitbucket.org/eunjeon/seunjeon/src/master/src/test/scala/org/bitbucket/eunjeon/seunjeon/AnalyzerTest.scala
  */
package com.github.uosdmlab.nkp

import org.apache.spark.sql.SparkSession
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSuite}

/**
  * Created by jun on 2016. 10. 16..
  */
class DictionarySuite extends FunSuite with BeforeAndAfterAll with BeforeAndAfter {
  val userDictFile = "src/test/resources/userdict.csv"
  val userDictAnotherFile = "src/test/resources/another_userdict.csv"
  val userDictFileWildCard = "src/test/resources/*.csv"

  private var tokenizer: Tokenizer = _
  private var analyzer: Analyzer = _

  private val spark: SparkSession =
    SparkSession.builder()
      .master("local[2]")
      .appName("Dictionary Suite")
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
    Dictionary.reset()
    analyzer = new Analyzer()
    tokenizer = new Tokenizer()
      .setInputCol("text")
      .setOutputCol("words")
  }

  private val df = spark.createDataset(
    Seq(
      "덕후냄새가 난다.",
      "낄끼빠빠",
      "버카충했어?",
      "C++"
    )
  ).toDF("text")

  private val answerSentences = Array(
    Seq("덕후", "냄새", "가", "난다", "."),
    Seq("낄끼빠빠"),
    Seq("버카충", "했", "어", "?"),
    Seq("C++")
  )

  def assertSentences(indicesToAssert: Int*): Unit = {
    val sentences = tokenizer
      .transform(df)
      .select("words")
      .map(_.getSeq[String](0))
      .collect()

    if (indicesToAssert.isEmpty)
      assert(sentences.sameElements(answerSentences))
    else
      indicesToAssert.foreach { i => assert(sentences(i) == answerSentences(i)) }
  }

  test("addWords: one word") {
    Dictionary.addWords("버카충,-100")
    assertSentences(2)
  }

  test("addWords: with var-args") {
    Dictionary.addWords("덕후", "낄끼+빠빠,-100", "버카충,-100", "C\\+\\+")
    assertSentences()
  }

  test("addWords: with Seq") {
    Dictionary.addWords(Seq("덕후", "낄끼+빠빠,-100", "버카충,-100", "C\\+\\+"))
    assertSentences()
  }

  test("addWordsFromCSV: one path") {
    Dictionary.addWordsFromCSV(userDictFile)
    assertSentences(0, 1)
    Dictionary.addWordsFromCSV(userDictAnotherFile)
    assertSentences()
  }

  test("addWordsFromCSV: wildcard") {
    Dictionary.addWordsFromCSV(userDictFileWildCard)
    assertSentences()
  }

  test("addWordsFromCSV: multi-paths") {
    Dictionary.addWordsFromCSV(userDictFile, userDictAnotherFile)
    assertSentences()
  }

  test("reset") {
    Dictionary.addWordsFromCSV(userDictFileWildCard)
    assertSentences()

    Dictionary.reset()
    intercept[Exception] {
      assertSentences()
    }
  }

  test("method chaining") {
    Dictionary
      .addWords("덕후")
      .reset()
      .addWords("덕후", "낄끼+빠빠,-100", "버카충,-100", "C\\+\\+")
      .reset()
      .addWords(Seq("덕후", "낄끼+빠빠,-100", "버카충,-100", "C\\+\\+"))
      .reset()
      .addWordsFromCSV(userDictFile)
      .reset()
      .addWordsFromCSV(userDictFileWildCard)
      .reset()
      .addWordsFromCSV(userDictFile, userDictAnotherFile)
      .reset()
  }
}
