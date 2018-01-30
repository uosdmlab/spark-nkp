package com.github.uosdmlab.nkp

import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.bitbucket.eunjeon.seunjeon.{Analyzer => EunjeonAnalyzer}


object Dictionary {

  // Words inside driver. This won't be modified in executor.
  private[nkp] var words = Seq.empty[String]

  /**
    * Executed from driver.
    */
  private[nkp] def broadcastWords(): Broadcast[Seq[String]] = {
    SparkSession.builder().getOrCreate().sparkContext.broadcast(words)
  }

  /**
    * Executed from executors.
    * NOTE: broadcastWords() should be executed first.
    */
  private[nkp] def syncWords(bcWords: Broadcast[Seq[String]]): Unit = {
    val dictWords = bcWords.value
    EunjeonAnalyzer.resetUserDict()
    if (words.nonEmpty)
      EunjeonAnalyzer.setUserDict(dictWords.iterator)
  }

  def reset(): this.type = chain {
    words = Seq.empty[String]
  }

  private var isDictionaryUsed = false

  private[nkp] def shouldSync = {
    isDictionaryUsed
  }

  def addWords(word: String, words: String*): this.type = addWords(word +: words)

  def addWords(words: Traversable[String]): this.type = chain {
    this.words = this.words ++ words
    isDictionaryUsed = true
  }

  def addWordsFromCSV(path: String, paths: String*): this.type = addWordsFromCSV(path +: paths)

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

  private def chain(fn: => Any): this.type = {
    fn
    this
  }
}
