package com.github.uosdmlab.nkp

import org.apache.spark.sql.{SparkSession, Row}
import org.apache.spark.sql.types.{StringType, StructField, StructType}


/**
  * Created by jun on 2017. 7. 1..
  */
object Dictionary {

  import org.bitbucket.eunjeon.seunjeon.{Analyzer => EunjeonAnalyzer}

  private var words = Seq.empty[String]

  private def chain(fn: => Any): this.type = {
    fn
    this
  }

  private def syncDictionary(): Unit = EunjeonAnalyzer.setUserDict(words.toIterator)

  def addWords(word: String, words: String*): Unit = addWords(word +: words)

  def addWords(words: Traversable[String]): this.type = chain {
    this.words = this.words ++ words
    syncDictionary()
  }

  def reset(): this.type = chain {
    EunjeonAnalyzer.resetUserDict()
    words = Seq.empty[String]
  }

  def addWordsFromCSV(path: String, paths: String*): this.type =
    addWordsFromCSV(path +: paths)

  def addWordsFromCSV(paths: Traversable[String]): this.type = chain {
    val spark = SparkSession.builder().getOrCreate()

    import spark.implicits._

    val schema = StructType(Array(
      StructField("word", StringType, nullable = false),
      StructField("cost", StringType, nullable = true)))

    val df = spark.read
      .option("sep", ",")
      .option("inferSchema", value = false)
      .option("header", value = false)
      .schema(schema)
      .csv(paths.toSeq: _*)

    val words = df.map {
      case Row(word: String, cost: String) =>
        s"$word,$cost"
      case Row(word: String, null) =>
        word
    }.collect()

    addWords(words)
  }
}
