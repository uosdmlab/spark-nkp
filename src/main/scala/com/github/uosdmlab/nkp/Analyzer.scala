package com.github.uosdmlab.nkp

import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param.{Param, ParamMap, Params}
import org.apache.spark.ml.util.{DefaultParamsReadable, DefaultParamsWritable, Identifiable}
import org.apache.spark.sql.{DataFrame, Dataset}
import org.apache.spark.sql.types.{ArrayType, IntegerType, StringType, StructType}

private[nkp] trait AnalyzerParams extends Params {
  final val idCol: Param[String] = new Param[String](this, "idCol", "Column name to identify each row")

  final def getIdCol: String = $(idCol)

  final val textCol: Param[String] = new Param[String](this, "textCol", "Text column name")

  final def getTextCol: String = $(textCol)

  final val wordCol: Param[String] = new Param[String](this, "wordCol", "Word column name")

  final def getWordCol: String = $(wordCol)

  final val posCol: Param[String] = new Param[String](this, "posCol", "POS(Part Of Speech) column name")

  final def getPosCol: String = $(posCol)

  final val charCol: Param[String] = new Param[String](this, "charCol", "Characteristic column name. " +
    "This column's value is 'feature' in seunjeon. It can be confused with Spark ML's feature, so " +
    "I changed name.")

  final def getCharCol: String = $(charCol)

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

  import org.bitbucket.eunjeon.seunjeon.{Analyzer => EunjeonAnalyzer, LNode}
  import org.apache.spark.sql.functions._

  def this() = this(Identifiable.randomUID("nkp_a"))

  def setIdCol(value: String): this.type = set(idCol, value)

  setDefault(idCol -> "id")

  def setTextCol(value: String): this.type = set(textCol, value)

  setDefault(textCol -> "text")

  def setWordCol(value: String): this.type = set(wordCol, value)

  setDefault(wordCol -> "word")

  def setPosCol(value: String): this.type = set(posCol, value)

  setDefault(posCol -> "pos")

  def setCharCol(value: String): this.type = set(charCol, value)

  setDefault(charCol -> "char")

  def setStartCol(value: String): this.type = set(startCol, value)

  setDefault(startCol -> "start")

  def setEndCol(value: String): this.type = set(endCol, value)

  setDefault(endCol -> "end")

  /**
    * Text segmentation UDF
    */
  private val extractWords = udf { text: String =>
    val parsed: Seq[LNode] = EunjeonAnalyzer.parse(text) // Parse text using seunjeon

    parsed.map { lNode: LNode =>
      val start = lNode.startPos // start offset
    val end = lNode.endPos // end offset
    val mor = lNode.morpheme // morpheme

      // word, POS(Part Of Speech), characteristic(feature in seunjeon), start offset, end offset
      (mor.surface, mor.poses.map(_.toString), mor.feature, start, end)
    }
  }

  // temporary array of morpheme column name
  private final val MORS_COL = Identifiable.randomUID("__mors__")
  // temporary morpheme column name
  private final val MOR_COL = Identifiable.randomUID("__mor__")

  override def transform(dataset: Dataset[_]): DataFrame = {
    transformSchema(dataset.schema)

    require(dataset.select($(idCol)).count == dataset.select($(idCol)).distinct.count,
      s"Column ${$(idCol)} should be unique ID")

    dataset.select($(idCol), $(textCol))
      .withColumn(MORS_COL, extractWords(col($(textCol)))) // segment text into array
      .select(col($(idCol)), explode(col(MORS_COL)).as(MOR_COL)) // explode array
      .selectExpr($(idCol), s"$MOR_COL._1", s"$MOR_COL._2", s"$MOR_COL._3", s"$MOR_COL._4", s"$MOR_COL._5") // flatten struct
      .toDF($(idCol), $(wordCol), $(posCol), $(charCol), $(startCol), $(endCol)) // assign column names
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
      .add($(charCol), ArrayType(StringType))
      .add($(startCol), IntegerType)
      .add($(endCol), IntegerType)
  }
}

object Analyzer extends DefaultParamsReadable[Analyzer] {
  override def load(path: String): Analyzer = super.load(path)
}