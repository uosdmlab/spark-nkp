package com.github.uosdmlab.nkp

import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param.{Param, ParamMap, Params}
import org.apache.spark.ml.util.{DefaultParamsReadable, DefaultParamsWritable, Identifiable}
import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._
import org.bitbucket.eunjeon.seunjeon.{Analyzer => EunjeonAnalyzer, LNode}

private[nkp] trait AnalyzerParams extends Params {
  final val idCol: Param[String] = new Param[String](this, "idCol", "Column name to identify each row")

  final def getIdCol: String = $(idCol)

  final val textCol: Param[String] = new Param[String](this, "textCol", "Text column name")

  final def getTextCol: String = $(textCol)

  final val wordCol: Param[String] = new Param[String](this, "wordCol", "Word column name")

  final def getWordCol: String = $(wordCol)

  final val posCol: Param[String] = new Param[String](this, "posCol", "POS(Part Of Speech) column name")

  final def getPosCol: String = $(posCol)

  final val featureCol: Param[String] = new Param[String](this, "featureCol", "Feature column name")

  final def getFeatureCol: String = $(featureCol)

  final val startCol: Param[String] = new Param[String](this, "startCol", "Start offset column name")

  final def getStartCol: String = $(startCol)

  final val endCol: Param[String] = new Param[String](this, "endCol", "End offset column name")

  final def getEndCol: String = $(endCol)
}

/**
  * Natural Korean Processor
  */
class Analyzer(override val uid: String) extends Transformer
  with AnalyzerParams with DefaultParamsWritable {

  def this() = this(Identifiable.randomUID("nkp_a"))

  def setIdCol(value: String): this.type = set(idCol, value)

  setDefault(idCol -> "id")

  def setTextCol(value: String): this.type = set(textCol, value)

  setDefault(textCol -> "text")

  def setWordCol(value: String): this.type = set(wordCol, value)

  setDefault(wordCol -> "word")

  def setPosCol(value: String): this.type = set(posCol, value)

  setDefault(posCol -> "pos")

  def setFeatureCol(value: String): this.type = set(featureCol, value)

  setDefault(featureCol -> "feature")

  def setStartCol(value: String): this.type = set(startCol, value)

  setDefault(startCol -> "start")

  def setEndCol(value: String): this.type = set(endCol, value)

  setDefault(endCol -> "end")

  private val extractWordsFunc = { text: String =>
    val parsed: Seq[LNode] = EunjeonAnalyzer.parse(text) // Parse text using seunjeon

    parsed.map { lNode: LNode =>
      val start = lNode.startPos // start offset
    val end = lNode.endPos // end offset
    val mor = lNode.morpheme // morpheme

      // word, POS(Part Of Speech), feature, start offset, end offset
      (mor.surface, mor.poses.map(_.toString), mor.feature, start, end)
    }
  }
  private val extractWords = udf(extractWordsFunc)

  // temporary array of morpheme column name
  private final val MORS_COL = Identifiable.randomUID("__mors__")
  // temporary morpheme column name
  private final val MOR_COL = Identifiable.randomUID("__mor__")

  override def transform(dataset: Dataset[_]): DataFrame = {
    transformSchema(dataset.schema)

    require(dataset.select($(idCol)).count == dataset.select($(idCol)).distinct.count,
      s"Column ${$(idCol)} should be unique ID")

    // Select required columns
    val df = dataset.select($(idCol), $(textCol))

    // Segment texts.
    val wordsDF = if (Dictionary.shouldSync) transformWithSync(dataset)
    else df.withColumn(MORS_COL, extractWords(col($(textCol))))

    wordsDF.select(col($(idCol)), explode(col(MORS_COL)).as(MOR_COL)) // explode array
      .selectExpr($(idCol), s"$MOR_COL._1", s"$MOR_COL._2", s"$MOR_COL._3", s"$MOR_COL._4", s"$MOR_COL._5") // flatten struct
      .toDF($(idCol), $(wordCol), $(posCol), $(featureCol), $(startCol), $(endCol)) // assign column names
  }

  private final val JOIN_ID_COL: String = Identifiable.randomUID("__joinid__")

  private def transformWithSync(dataset: Dataset[_]): DataFrame = {
    Dictionary.broadcastWords()

    val df = dataset.withColumn(JOIN_ID_COL, monotonically_increasing_id())

    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._

    val outputDF = df.select(JOIN_ID_COL, $(textCol))
      .mapPartitions { it: Iterator[Row] =>
        Dictionary.syncWords()
        it.map {
          case Row(joinId: Long, text: String) => (joinId, extractWordsFunc(text))
        }
      }
      .toDF(JOIN_ID_COL, MORS_COL)

    df.join(outputDF, JOIN_ID_COL).drop(JOIN_ID_COL)
  }

  override def copy(extra: ParamMap): Transformer = defaultCopy(extra)

  @DeveloperApi
  override def transformSchema(schema: StructType): StructType = {
    require(schema.fieldNames.contains($(idCol)),
      s"Dataset should have ${$(idCol)} column")
    require(schema.fieldNames.contains($(textCol)),
      s"Dataset should have ${$(textCol)} column")

    require(schema($(textCol)).dataType.equals(StringType),
      s"Type of ${$(textCol)} should be String")

    new StructType()
      .add($(idCol), schema($(idCol)).dataType)
      .add($(wordCol), StringType)
      .add($(posCol), ArrayType(StringType))
      .add($(featureCol), ArrayType(StringType))
      .add($(startCol), IntegerType)
      .add($(endCol), IntegerType)
  }
}

object Analyzer extends DefaultParamsReadable[Analyzer] {
  override def load(path: String): Analyzer = super.load(path)
}